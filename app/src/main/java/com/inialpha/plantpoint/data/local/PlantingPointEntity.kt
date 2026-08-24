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
    val plantedAt: Long? = null
) {
    companion object {
        const val STATUS_PLANNED = "PLANNED"
        const val STATUS_PLANTED = "PLANTED"
    }
}
