package com.inialpha.plantpoint.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "planting_points",
    indices = [Index("farmId"), Index("cropId")]
)
data class PlantingPointEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val farmId: String,
    val cropId: String,
    val sequenceNumber: Int,
    val plannedLatitude: Double,
    val plannedLongitude: Double,
    val actualLatitude: Double? = null,
    val actualLongitude: Double? = null,
    val status: String = STATUS_PLANNED,
    val createdAt: Long = System.currentTimeMillis(),
    val plantedAt: Long? = null,
    // Logical planting-grid coordinates. The farm/crop starting point is always (0, 0);
    // see PlantingGridEngine for how these map to plannedLatitude/plannedLongitude.
    val gridRow: Int = 0,
    val gridColumn: Int = 0,
    // Location accuracy (meters) reported by the device at the moment this point was recorded,
    // if any. Stored so past records can be interpreted with the confidence they were taken at.
    val recordedAccuracyMeters: Double? = null
) {
    companion object {
        const val STATUS_PLANNED = "PLANNED"
        const val STATUS_PLANTED = "PLANTED"
    }
}
