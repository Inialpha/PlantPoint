package com.inialpha.plantpoint.data.location

/** Immutable location sample exposed to the rest of PlantPoint. */
data class LocationSnapshot(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val speedMetersPerSecond: Float?,
    val movementBearingDegrees: Float?,
    val timestampMillis: Long,
    val isMock: Boolean
)
