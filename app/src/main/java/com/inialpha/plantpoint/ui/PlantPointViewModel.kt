package com.inialpha.plantpoint.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inialpha.plantpoint.data.local.CropDao
import com.inialpha.plantpoint.data.local.CropEntity
import com.inialpha.plantpoint.data.local.FarmDao
import com.inialpha.plantpoint.data.local.FarmEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class PlantPointViewModel(
    private val farmDao: FarmDao,
    private val cropDao: CropDao
) : ViewModel() {
    val farms: Flow<List<FarmEntity>> = farmDao.observeAll()

    fun cropsForFarm(farmId: String): Flow<List<CropEntity>> = cropDao.observeForFarm(farmId)

    fun addFarm(name: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val cleanName = name.trim()
            if (cleanName.isEmpty() || farmDao.count() >= MAX_FARMS) {
                onResult(false)
                return@launch
            }
            farmDao.insert(FarmEntity(name = cleanName))
            onResult(true)
        }
    }

    fun addCrop(
        farmId: String,
        name: String,
        spacingMeters: Double,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val cleanName = name.trim()
            if (cleanName.isEmpty() || spacingMeters <= 0.0) {
                onResult(false)
                return@launch
            }
            cropDao.insert(
                CropEntity(
                    farmId = farmId,
                    name = cleanName,
                    spacingMeters = spacingMeters
                )
            )
            onResult(true)
        }
    }

    fun deleteFarm(farm: FarmEntity) {
        viewModelScope.launch { farmDao.delete(farm) }
    }

    fun deleteCrop(crop: CropEntity) {
        viewModelScope.launch { cropDao.delete(crop) }
    }

    companion object {
        const val MAX_FARMS = 3
    }
}
