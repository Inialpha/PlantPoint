package com.inialpha.plantpoint.data.sensors

data class OrientationSnapshot(
    val headingDegrees: Float,
    val headingAccuracyDegrees: Float? = null,
    val timestampMillis: Long = System.currentTimeMillis()
)
