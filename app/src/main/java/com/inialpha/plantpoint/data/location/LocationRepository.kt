package com.inialpha.plantpoint.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocationRepository(context: Context) {
    private val appContext = context.applicationContext
    private val client: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(appContext)
    private val locationManager =
        appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val _location = MutableStateFlow<LocationSnapshot?>(null)
    val location: StateFlow<LocationSnapshot?> = _location.asStateFlow()

    private val _isAvailable = MutableStateFlow(false)
    val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.locations.forEach { location ->
                _location.value = LocationSnapshot(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracyMeters = location.takeIf { it.hasAccuracy() }?.accuracy,
                    speedMetersPerSecond = location.takeIf { it.hasSpeed() }?.speed,
                    movementBearingDegrees = location.takeIf { it.hasBearing() && it.speed >= 0.5f }?.bearing,
                    timestampMillis = location.time,
                    isMock = location.isMockLocationCompat()
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun start() {
        _isAvailable.value = isLocationEnabled()
        if (!_isAvailable.value) return

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(500L)
            .setWaitForAccurateLocation(true)
            .build()

        client.requestLocationUpdates(request, callback, appContext.mainLooper)
    }

    fun stop() {
        client.removeLocationUpdates(callback)
    }

    fun isLocationEnabled(): Boolean =
        locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

    @Suppress("DEPRECATION")
    private fun android.location.Location.isMockLocationCompat(): Boolean =
        if (android.os.Build.VERSION.SDK_INT >= 31) isMock else isFromMockProvider
}
