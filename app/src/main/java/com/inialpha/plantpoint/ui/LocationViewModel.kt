package com.inialpha.plantpoint.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.inialpha.plantpoint.data.position.PositionProvider
import com.inialpha.plantpoint.data.position.SmartphonePositionProvider

class LocationViewModel(application: Application) : AndroidViewModel(application) {
    // Depends on the PositionProvider abstraction rather than Android's Location/Sensor APIs
    // directly, so a future precision positioning source can be substituted here without
    // touching any screen that consumes this ViewModel.
    private val positionProvider: PositionProvider = SmartphonePositionProvider(application)

    val location = positionProvider.location
    val locationAvailable = positionProvider.isAvailable
    val orientation = positionProvider.orientation
    val hasRotationSensor = positionProvider.hasHeadingSensor

    fun start() {
        positionProvider.start()
    }

    fun stop() {
        positionProvider.stop()
    }

    fun isLocationEnabled(): Boolean = positionProvider.isLocationEnabled()

    override fun onCleared() {
        stop()
        super.onCleared()
    }
}
