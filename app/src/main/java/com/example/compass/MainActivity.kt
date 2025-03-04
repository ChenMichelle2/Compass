package com.example.compass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.compass.ui.theme.CompassTheme

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometerReading = FloatArray(3)
    private var magnetometerReading = FloatArray(3)
    // For roll and pitch
    private var gyroscopeAngles = floatArrayOf(0f, 0f, 0f)
    private var lastGyroTimestamp: Long = 0

    // Variables for sensor data
    private val _compassHeading = mutableStateOf(0f)
    private val _roll = mutableStateOf(0f)
    private val _pitch = mutableStateOf(0f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // Register sensors
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accelerometer ->
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also { magneticField ->
            sensorManager.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_GAME)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.also { gyroscope ->
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME)
        }

        setContent {
            CompassTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Read sensor state values
                    val compassHeading by _compassHeading
                    val roll by _roll
                    val pitch by _pitch
                    CompassLevelScreen(
                        compassHeading = compassHeading,
                        roll = roll,
                        pitch = pitch,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    accelerometerReading = it.values.clone()
                    calculateCompassHeading()
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    magnetometerReading = it.values.clone()
                    calculateCompassHeading()
                }
                Sensor.TYPE_GYROSCOPE -> {
                    // Use gyroscope readings to integrate roll & pitch
                    if (lastGyroTimestamp != 0L) {
                        val dt = (it.timestamp - lastGyroTimestamp) * (1.0f / 1_000_000_000.0f)
                        gyroscopeAngles[0] += it.values[0] * dt  // Pitch
                        gyroscopeAngles[1] += it.values[1] * dt  // Roll
                        _pitch.value =  Math.toDegrees(gyroscopeAngles[0].toDouble()).toFloat()
                        _roll.value = Math.toDegrees(gyroscopeAngles[1].toDouble()).toFloat()
                    }
                    lastGyroTimestamp = it.timestamp
                }
            }
        }
    }

    private fun calculateCompassHeading() {
        val rotationMatrix = FloatArray(9)
        val success = SensorManager.getRotationMatrix(
            rotationMatrix, null, accelerometerReading, magnetometerReading
        )
        if (success) {
            val orientationAngles = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            val azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
            // Normalize heading to 0-360 degrees
            _compassHeading.value = (azimuth + 360) % 360
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onResume() {
        super.onResume()
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accelerometer ->
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also { magneticField ->
            sensorManager.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_GAME)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.also { gyroscope ->
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME)
        }
    }
}

@Composable
fun CompassLevelScreen(
    compassHeading: Float,
    roll: Float,
    pitch: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                // Gradient background
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF2196F3), Color(0xFF03A9F4))
                )
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // Compass
        Text(
            text = "Compass",
            fontSize = 32.sp,
            color = Color.White
        )
        Box(
            modifier = Modifier.size(250.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(color = Color.White.copy(alpha = 0.3f))
            }
            Image(
                painter = painterResource(id = R.drawable.compass_needle),
                contentDescription = "Compass Needle",
                modifier = Modifier
                    .size(200.dp)
                    .rotate(-compassHeading) // Negative rotation aligns the needle with north
            )
            Text(
                text = "${compassHeading.toInt()}°",
                fontSize = 20.sp,
                color = Color.White,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
        // Digital Level Section
        Text(
            text = "Digital Level",
            fontSize = 32.sp,
            color = Color.White
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Roll: ${"%.1f".format(roll)}°",
                fontSize = 24.sp,
                color = Color.White
            )
            Text(
                text = "Pitch: ${"%.1f".format(pitch)}°",
                fontSize = 24.sp,
                color = Color.White
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCompassLevelScreen() {
    CompassTheme {
        CompassLevelScreen(compassHeading = 0f, roll = 0f, pitch = 0f)
    }
}
