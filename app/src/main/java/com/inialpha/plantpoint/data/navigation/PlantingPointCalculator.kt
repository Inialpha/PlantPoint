package com.inialpha.plantpoint.data.navigation

import kotlin.math.cos
import kotlin.math.sin

/** Geographic calculations used by planting navigation. */
object PlantingPointCalculator {
    private const val EARTH_RADIUS_METERS = 6_378_137.0

    fun destination(
        latitude: Double,
        longitude: Double,
        distanceMeters: Double,
        bearingDegrees: Double
    ): Pair<Double, Double> {
        require(distanceMeters >= 0.0) { "Distance must not be negative" }

        val angularDistance = distanceMeters / EARTH_RADIUS_METERS
        val bearing = Math.toRadians(bearingDegrees)
        val lat1 = Math.toRadians(latitude)
        val lon1 = Math.toRadians(longitude)

        val sinLat1 = sin(lat1)
        val cosLat1 = cos(lat1)
        val sinAngular = sin(angularDistance)
        val cosAngular = cos(angularDistance)

        val lat2 = Math.asin(
            sinLat1 * cosAngular + cosLat1 * sinAngular * cos(bearing)
        )
        val lon2 = lon1 + Math.atan2(
            sin(bearing) * sinAngular * cosLat1,
            cosAngular - sinLat1 * sin(lat2)
        )

        return Math.toDegrees(lat2) to normalizeLongitude(Math.toDegrees(lon2))
    }

    fun cardinalBearing(direction: CardinalDirection): Double = direction.bearingDegrees

    private fun normalizeLongitude(longitude: Double): Double {
        var value = longitude
        while (value > 180.0) value -= 360.0
        while (value < -180.0) value += 360.0
        return value
    }
}

enum class CardinalDirection(val label: String, val bearingDegrees: Double) {
    NORTH("North", 0.0),
    EAST("East", 90.0),
    SOUTH("South", 180.0),
    WEST("West", 270.0)
}
