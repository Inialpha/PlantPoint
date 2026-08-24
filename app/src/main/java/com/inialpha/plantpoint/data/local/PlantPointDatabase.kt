package com.inialpha.plantpoint.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [FarmEntity::class, CropEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PlantPointDatabase : RoomDatabase() {
    abstract fun farmDao(): FarmDao
    abstract fun cropDao(): CropDao

    companion object {
        @Volatile
        private var INSTANCE: PlantPointDatabase? = null

        fun getInstance(context: Context): PlantPointDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PlantPointDatabase::class.java,
                    "plantpoint.db"
                ).build().also { INSTANCE = it }
            }
    }
}
