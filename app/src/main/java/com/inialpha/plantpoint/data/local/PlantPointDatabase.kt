package com.inialpha.plantpoint.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [FarmEntity::class, CropEntity::class, PlantingPointEntity::class],
    version = 3,
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

        // Adds logical planting-grid coordinates and recorded location accuracy to existing
        // planting_points rows. Non-destructive: existing rows default to (0, 0) / null, which
        // is safe because they predate the grid feature and were tracked purely by sequence.
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE planting_points ADD COLUMN gridRow INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE planting_points ADD COLUMN gridColumn INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE planting_points ADD COLUMN recordedAccuracyMeters REAL")
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
