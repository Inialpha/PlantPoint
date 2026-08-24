package com.inialpha.plantpoint.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CropDao {
    @Query("SELECT * FROM crops WHERE farmId = :farmId ORDER BY name COLLATE NOCASE ASC")
    fun observeForFarm(farmId: String): Flow<List<CropEntity>>

    @Insert
    suspend fun insert(crop: CropEntity)

    @Update
    suspend fun update(crop: CropEntity)

    @Delete
    suspend fun delete(crop: CropEntity)
}
