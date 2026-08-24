package com.inialpha.plantpoint.data.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

/**
 * Device orientation layer. Rotation-vector is preferred because Android documents it
 * as the versatile sensor for orientation/compass use; the app does not depend on a
 * network service for heading.
 */
class SensorRepository(context: Context) {
    private val sensorManager =
        context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val _orientation = MutableStateFlow<OrientationSnapshot?>(null)
    val orientation: StateFlow<OrientationSnapshot?> = _orientation.asStateFlow()

    val hasRotationSensor: Boolean
        get() = rotationSensor != null

    private val listener = object : SensorEventListener {
        private val rotationMatrix = FloatArray(9)
        private val orientationAngles = FloatArray(3)

        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return

            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)

            val radians = orientationAngles[0]
            val degrees = Math.toDegrees(radians.toDouble()).toFloat().normalizeHeading()

            _orientation.value = OrientationSnapshot(
                headingDegrees = degrees,
                timestampMillis = System.currentTimeMillis()
            )
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    fun start() {
        rotationSensor?.let {
            sensorManager.registerListener(
                listener,
                it,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
    }

    fun stop() {
        sensorManager.unregisterListener(listener)
    }

    private fun Float.normalizeHeading(): Float {
        var value = this % 360f
        if (value < 0f) value += 360f
        return value.roundToInt().toFloat()
    }
}
