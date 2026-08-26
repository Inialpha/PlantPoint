package com.inialpha.plantpoint.data.position

import android.content.Context
import com.inialpha.plantpoint.data.location.LocationRepository
import com.inialpha.plantpoint.data.sensors.SensorRepository

/**
 * Smartphone-only implementation of [PositionProvider]. This is a thin composition of the
 * existing [LocationRepository] (GNSS via FusedLocationProviderClient) and [SensorRepository]
 * (rotation-vector compass) — neither is reimplemented here, only exposed behind the shared
 * interface so the planting grid does not depend on Android's Location APIs directly.
 *
 * Ordinary smartphone GNSS/IMU hardware cannot provide survey-grade accuracy; this provider
 * reports whatever accuracy Android supplies and never manufactures a more precise answer.
 */
class SmartphonePositionProvider(context: Context) : PositionProvider {
    private val locationRepository = LocationRepository(context)
    private val sensorRepository = SensorRepository(context)

    override val location = locationRepository.location
    override val orientation = sensorRepository.orientation
    override val isAvailable = locationRepository.isAvailable
    override val hasHeadingSensor: Boolean get() = sensorRepository.hasRotationSensor

    override fun start() {
        locationRepository.start()
        sensorRepository.start()
    }

    override fun stop() {
        locationRepository.stop()
        sensorRepository.stop()
    }

    override fun isLocationEnabled(): Boolean = locationRepository.isLocationEnabled()
}
