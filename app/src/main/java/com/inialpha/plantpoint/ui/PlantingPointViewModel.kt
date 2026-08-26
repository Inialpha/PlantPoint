package com.inialpha.plantpoint.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inialpha.plantpoint.data.local.PlantingPointDao
import com.inialpha.plantpoint.data.local.PlantingPointEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class PlantingPointViewModel(
    private val plantingPointDao: PlantingPointDao
) : ViewModel() {
    fun pointsForCrop(cropId: String): Flow<List<PlantingPointEntity>> =
        plantingPointDao.observeForCrop(cropId)

    /** Points recorded for other crops on the same farm, for cross-crop proximity warnings. */
    fun otherCropPointsFor(farmId: String, cropId: String): Flow<List<PlantingPointEntity>> =
        plantingPointDao.observeOtherCropPoints(farmId, cropId)

    fun createStartingPoint(
        farmId: String,
        cropId: String,
        latitude: Double,
        longitude: Double,
        onCreated: (PlantingPointEntity) -> Unit = {}
    ) {
        viewModelScope.launch {
            val nextSequence = (plantingPointDao.maxSequenceForCrop(cropId) ?: 0) + 1
            val point = PlantingPointEntity(
                farmId = farmId,
                cropId = cropId,
                sequenceNumber = nextSequence,
                plannedLatitude = latitude,
                plannedLongitude = longitude,
                actualLatitude = latitude,
                actualLongitude = longitude,
                status = PlantingPointEntity.STATUS_PLANTED,
                plantedAt = System.currentTimeMillis()
            )
            plantingPointDao.insert(point)
            onCreated(point)
        }
    }

    /**
     * Establishes the farm/crop's grid origin (row = 0, column = 0) at the farmer's current
     * location, with the reported location accuracy recorded alongside it.
     */
    fun createGridOrigin(
        farmId: String,
        cropId: String,
        latitude: Double,
        longitude: Double,
        accuracyMeters: Double?,
        onCreated: (PlantingPointEntity) -> Unit = {}
    ) {
        viewModelScope.launch {
            val nextSequence = (plantingPointDao.maxSequenceForCrop(cropId) ?: 0) + 1
            val point = PlantingPointEntity(
                farmId = farmId,
                cropId = cropId,
                sequenceNumber = nextSequence,
                plannedLatitude = latitude,
                plannedLongitude = longitude,
                actualLatitude = latitude,
                actualLongitude = longitude,
                status = PlantingPointEntity.STATUS_PLANTED,
                plantedAt = System.currentTimeMillis(),
                gridRow = 0,
                gridColumn = 0,
                recordedAccuracyMeters = accuracyMeters
            )
            plantingPointDao.insert(point)
            onCreated(point)
        }
    }

    fun recordPlantedTarget(
        farmId: String,
        cropId: String,
        latitude: Double,
        longitude: Double,
        actualLatitude: Double,
        actualLongitude: Double
    ) {
        viewModelScope.launch {
            val nextSequence = (plantingPointDao.maxSequenceForCrop(cropId) ?: 0) + 1
            plantingPointDao.insert(
                PlantingPointEntity(
                    farmId = farmId,
                    cropId = cropId,
                    sequenceNumber = nextSequence,
                    plannedLatitude = latitude,
                    plannedLongitude = longitude,
                    actualLatitude = actualLatitude,
                    actualLongitude = actualLongitude,
                    status = PlantingPointEntity.STATUS_PLANTED,
                    plantedAt = System.currentTimeMillis()
                )
            )
        }
    }

    /** Records a planted point at a specific planting-grid cell (row, column). */
    fun plantGridPoint(
        farmId: String,
        cropId: String,
        row: Int,
        column: Int,
        plannedLatitude: Double,
        plannedLongitude: Double,
        actualLatitude: Double,
        actualLongitude: Double,
        accuracyMeters: Double?
    ) {
        viewModelScope.launch {
            val nextSequence = (plantingPointDao.maxSequenceForCrop(cropId) ?: 0) + 1
            plantingPointDao.insert(
                PlantingPointEntity(
                    farmId = farmId,
                    cropId = cropId,
                    sequenceNumber = nextSequence,
                    plannedLatitude = plannedLatitude,
                    plannedLongitude = plannedLongitude,
                    actualLatitude = actualLatitude,
                    actualLongitude = actualLongitude,
                    status = PlantingPointEntity.STATUS_PLANTED,
                    plantedAt = System.currentTimeMillis(),
                    gridRow = row,
                    gridColumn = column,
                    recordedAccuracyMeters = accuracyMeters
                )
            )
        }
    }

    fun markPlanted(
        point: PlantingPointEntity,
        actualLatitude: Double,
        actualLongitude: Double
    ) {
        viewModelScope.launch {
            plantingPointDao.update(
                point.copy(
                    actualLatitude = actualLatitude,
                    actualLongitude = actualLongitude,
                    status = PlantingPointEntity.STATUS_PLANTED,
                    plantedAt = System.currentTimeMillis()
                )
            )
        }
    }
}
