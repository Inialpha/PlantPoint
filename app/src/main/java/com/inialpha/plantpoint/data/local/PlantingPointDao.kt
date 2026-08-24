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

    @Insert
    suspend fun insert(point: PlantingPointEntity)

    @Update
    suspend fun update(point: PlantingPointEntity)
}
