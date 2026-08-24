package com.inialpha.plantpoint.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FarmDao {
    @Query("SELECT * FROM farms ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<FarmEntity>>

    @Query("SELECT * FROM farms WHERE id = :farmId LIMIT 1")
    fun observeById(farmId: String): Flow<FarmEntity?>

    @Insert
    suspend fun insert(farm: FarmEntity)

    @Update
    suspend fun update(farm: FarmEntity)

    @Delete
    suspend fun delete(farm: FarmEntity)

    @Query("SELECT COUNT(*) FROM farms")
    suspend fun count(): Int
}
