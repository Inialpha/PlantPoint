package com.inialpha.plantpoint.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [FarmEntity::class, CropEntity::class, PlantingPointEntity::class],
    version = 2,
    exportSchema = false
)
abstract class PlantPointDatabase : RoomDatabase() {
    abstract fun farmDao(): FarmDao
    abstract fun cropDao(): CropDao
    abstract fun plantingPointDao(): PlantingPointDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS planting_points (" +
                        "id TEXT NOT NULL PRIMARY KEY, " +
                        "farmId TEXT NOT NULL, " +
                        "cropId TEXT NOT NULL, " +
                        "sequenceNumber INTEGER NOT NULL, " +
                        "plannedLatitude REAL NOT NULL, " +
                        "plannedLongitude REAL NOT NULL, " +
                        "actualLatitude REAL, " +
                        "actualLongitude REAL, " +
                        "status TEXT NOT NULL, " +
                        "createdAt INTEGER NOT NULL, " +
                        "plantedAt INTEGER)"
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_planting_points_farmId ON planting_points(farmId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_planting_points_cropId ON planting_points(cropId)")
            }
        }

        @Volatile
        private var INSTANCE: PlantPointDatabase? = null

        fun getInstance(context: Context): PlantPointDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PlantPointDatabase::class.java,
                    "plantpoint.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
