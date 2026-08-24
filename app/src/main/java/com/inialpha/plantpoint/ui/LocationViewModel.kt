package com.inialpha.plantpoint.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.inialpha.plantpoint.data.location.LocationRepository
import com.inialpha.plantpoint.data.sensors.SensorRepository
import kotlinx.coroutines.flow.StateFlow

class LocationViewModel(application: Application) : AndroidViewModel(application) {
    private val locationRepository = LocationRepository(application)
    private val sensorRepository = SensorRepository(application)

    val location = locationRepository.location
    val locationAvailable = locationRepository.isAvailable
    val orientation = sensorRepository.orientation
    val hasRotationSensor = sensorRepository.hasRotationSensor

    fun start() {
        locationRepository.start()
        sensorRepository.start()
    }

    fun stop() {
        locationRepository.stop()
        sensorRepository.stop()
    }

    fun isLocationEnabled(): Boolean = locationRepository.isLocationEnabled()

    override fun onCleared() {
        stop()
        super.onCleared()
    }
}
