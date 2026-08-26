package com.inialpha.plantpoint.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantingPointDao {
    @Query("SELECT * FROM planting_points WHERE cropId = :cropId ORDER BY sequenceNumber ASC")
    fun observeForCrop(cropId: String): Flow<List<PlantingPointEntity>>

    @Query("SELECT MAX(sequenceNumber) FROM planting_points WHERE cropId = :cropId")
    suspend fun maxSequenceForCrop(cropId: String): Int?

    @Query("SELECT * FROM planting_points WHERE cropId = :cropId AND gridRow = :row AND gridColumn = :column LIMIT 1")
    suspend fun findAtGrid(cropId: String, row: Int, column: Int): PlantingPointEntity?

    // Points recorded for OTHER crops in the same farm, used only to surface a proximity
    // warning in the UI (never to block or auto-modify this crop's own planting records).
    @Query("SELECT * FROM planting_points WHERE farmId = :farmId AND cropId != :cropId")
    fun observeOtherCropPoints(farmId: String, cropId: String): Flow<List<PlantingPointEntity>>

    @Insert
    suspend fun insert(point: PlantingPointEntity)

    @Update
    suspend fun update(point: PlantingPointEntity)
}
