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
