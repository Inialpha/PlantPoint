package com.inialpha.plantpoint

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inialpha.plantpoint.data.local.CropEntity
import com.inialpha.plantpoint.data.local.FarmEntity
import com.inialpha.plantpoint.data.local.PlantPointDatabase
import com.inialpha.plantpoint.data.navigation.CardinalDirection
import com.inialpha.plantpoint.data.navigation.PlantingPointCalculator
import com.inialpha.plantpoint.ui.LocationViewModel
import com.inialpha.plantpoint.ui.PlantPointViewModel
import com.inialpha.plantpoint.ui.PlantingPointViewModel
import java.util.Locale
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PlantPointApp() }
    }
}

private class PlantPointViewModelFactory(context: android.content.Context) : ViewModelProvider.Factory {
    private val database = PlantPointDatabase.getInstance(context.applicationContext)
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlantPointViewModel::class.java)) {
            return PlantPointViewModel(database.farmDao(), database.cropDao()) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}

private class PlantingPointViewModelFactory(context: android.content.Context) : ViewModelProvider.Factory {
    private val database = PlantPointDatabase.getInstance(context.applicationContext)
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlantingPointViewModel::class.java)) {
            return PlantingPointViewModel(database.plantingPointDao()) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}

@Composable
private fun PlantPointApp() {
    Surface(modifier = Modifier.fillMaxSize()) { FarmListScreen() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FarmListScreen() {
    val context = LocalContext.current
    val farmViewModel: PlantPointViewModel = viewModel(factory = PlantPointViewModelFactory(context))
    val farms by farmViewModel.farms.collectAsStateWithLifecycle(initialValue = emptyList())
    var showAddFarm by remember { mutableStateOf(false) }
    var selectedFarmId by remember { mutableStateOf<String?>(null) }
    val selectedFarm = farms.firstOrNull { it.id == selectedFarmId }
    if (selectedFarm != null) {
        FarmDetailScreen(farm = selectedFarm, onBack = { selectedFarmId = null })
        return
    }
    Scaffold(topBar = { TopAppBar(title = { Text("PlantPoint") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("My Farms", style = MaterialTheme.typography.headlineMedium)
            if (farms.isEmpty()) Text("No farms yet. Add your first farm to begin.") else farms.forEach { farm ->
                Card(onClick = { selectedFarmId = farm.id }, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(farm.name, style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { farmViewModel.deleteFarm(farm) }) { Text("Delete") }
                    }
                }
            }
            Button(onClick = { showAddFarm = true }, enabled = farms.size < PlantPointViewModel.MAX_FARMS, modifier = Modifier.fillMaxWidth()) {
                Text(if (farms.size < PlantPointViewModel.MAX_FARMS) "Add Farm" else "Maximum of 3 Farms")
            }
        }
    }
    if (showAddFarm) {
        var name by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { showAddFarm = false }, title = { Text("Add Farm") },
            text = { OutlinedTextField(name, { name = it }, label = { Text("Farm name") }) },
            confirmButton = { TextButton(onClick = { if (name.isNotBlank()) { farmViewModel.addFarm(name.trim()); showAddFarm = false } }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { showAddFarm = false }) { Text("Cancel") } })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FarmDetailScreen(farm: FarmEntity, onBack: () -> Unit) {
    val context = LocalContext.current
    val farmViewModel: PlantPointViewModel = viewModel(factory = PlantPointViewModelFactory(context))
    val crops by farmViewModel.cropsForFarm(farm.id).collectAsStateWithLifecycle(initialValue = emptyList())
    var showAddCrop by remember { mutableStateOf(false) }
    var showDiagnostics by remember { mutableStateOf(false) }
    var plantingCrop by remember { mutableStateOf<CropEntity?>(null) }
    if (showDiagnostics) { LocationDiagnosticsScreen(onBack = { showDiagnostics = false }); return }
    val activeCrop = plantingCrop
    if (activeCrop != null) { PlantingNavigationScreen(farm = farm, crop = activeCrop, onBack = { plantingCrop = null }); return }
    Scaffold(topBar = { TopAppBar(title = { Text(farm.name) }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Crops", style = MaterialTheme.typography.headlineSmall)
            if (crops.isEmpty()) Text("No crops configured for this farm yet.")
            crops.forEach { crop -> CropRow(crop, onDelete = { farmViewModel.deleteCrop(crop) }, onStartPlanting = { plantingCrop = crop }) }
            Button(onClick = { showAddCrop = true }, modifier = Modifier.fillMaxWidth()) { Text("Add Crop") }
            OutlinedButton(onClick = { showDiagnostics = true }, modifier = Modifier.fillMaxWidth()) { Text("Location & Direction Diagnostics") }
        }
    }
    if (showAddCrop) {
        var name by remember { mutableStateOf("") }
        var spacing by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { showAddCrop = false }, title = { Text("Add Crop") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Crop name") })
                OutlinedTextField(spacing, { spacing = it }, label = { Text("Spacing (meters)") })
            } },
            confirmButton = { TextButton(onClick = { val meters = spacing.toDoubleOrNull(); if (name.isNotBlank() && meters != null && meters > 0.0) { farmViewModel.addCrop(farm.id, name.trim(), meters); showAddCrop = false } }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { showAddCrop = false }) { Text("Cancel") } })
    }
}

@Composable
private fun CropRow(crop: CropEntity, onDelete: () -> Unit, onStartPlanting: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column { Text(crop.name, style = MaterialTheme.typography.titleMedium); Text(String.format(Locale.US, "%.2f m spacing", crop.spacingMeters)) }
                TextButton(onClick = onDelete) { Text("Delete") }
            }
            Button(onClick = onStartPlanting, modifier = Modifier.fillMaxWidth()) { Text("Start Planting") }
        }
    }
}

private data class NavigationTarget(val direction: CardinalDirection, val latitude: Double, val longitude: Double)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlantingNavigationScreen(farm: FarmEntity, crop: CropEntity, onBack: () -> Unit) {
    val context = LocalContext.current
    val locationViewModel: LocationViewModel = viewModel()
    val plantingViewModel: PlantingPointViewModel = viewModel(factory = PlantingPointViewModelFactory(context))
    val location by locationViewModel.location.collectAsStateWithLifecycle(initialValue = null)
    val orientation by locationViewModel.orientation.collectAsStateWithLifecycle(initialValue = null)
    val points by plantingViewModel.pointsForCrop(crop.id).collectAsStateWithLifecycle(initialValue = emptyList())
    var permissionGranted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) }
    var selectedDirection by remember { mutableStateOf(CardinalDirection.NORTH) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result -> permissionGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true || result[Manifest.permission.ACCESS_COARSE_LOCATION] == true }
    LaunchedEffect(permissionGranted) { if (permissionGranted) locationViewModel.start() }
    val lastPoint = points.lastOrNull()
    val anchorLatitude = lastPoint?.actualLatitude ?: lastPoint?.plannedLatitude
    val anchorLongitude = lastPoint?.actualLongitude ?: lastPoint?.plannedLongitude
    val targets = if (anchorLatitude != null && anchorLongitude != null) CardinalDirection.entries.map { direction ->
        val destination = PlantingPointCalculator.destination(anchorLatitude, anchorLongitude, crop.spacingMeters, direction.bearingDegrees)
        NavigationTarget(direction, destination.first, destination.second)
    } else emptyList()
    LaunchedEffect(orientation?.headingDegrees, targets) {
        val heading = orientation?.headingDegrees ?: return@LaunchedEffect
        val closest = targets.minByOrNull { circularAngleDifference(heading.toDouble(), it.direction.bearingDegrees) }
        if (closest != null) selectedDirection = closest.direction
    }
    val selectedTarget = targets.firstOrNull { it.direction == selectedDirection }
    val currentLocation = location
    val distanceToTarget: Double? = if (currentLocation != null && selectedTarget != null) {
        val results = FloatArray(3)
        Location.distanceBetween(currentLocation.latitude, currentLocation.longitude, selectedTarget.latitude, selectedTarget.longitude, results)
        results[0].toDouble()
    } else null
    val targetBearing = if (currentLocation != null && selectedTarget != null) {
        val results = FloatArray(3)
        Location.distanceBetween(currentLocation.latitude, currentLocation.longitude, selectedTarget.latitude, selectedTarget.longitude, results)
        normalizeBearing(results[1].toDouble())
    } else null
    val gpsAccuracy = currentLocation?.accuracyMeters?.toDouble()
    val arrivalTolerance = if (gpsAccuracy != null) maxOf(1.0, minOf(3.0, gpsAccuracy)) else 1.5
    val arrived = distanceToTarget != null && distanceToTarget <= arrivalTolerance
    Scaffold(topBar = { TopAppBar(title = { Text("Plant ${crop.name}") }, navigationIcon = { TextButton(onClick = { locationViewModel.stop(); onBack() }) { Text("Back") } }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Spacing: ${String.format(Locale.US, "%.2f m", crop.spacingMeters)}")
            if (!permissionGranted) { Text("Location permission is required to navigate planting points."); Button(onClick = { permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }) { Text("Allow Location") }; return@Column }
            if (location == null) { Text("Waiting for a GPS fix…"); Text("Keep the phone in an open area until a location appears.") }
            else if (lastPoint == null) {
                Text("Stand at the exact spot where planting should begin.")
                Text("GPS accuracy: ${String.format(Locale.US, "%.1f m", location?.accuracyMeters ?: 0f)}")
                Button(onClick = { val current = location ?: return@Button; plantingViewModel.createStartingPoint(farm.id, crop.id, current.latitude, current.longitude) }, modifier = Modifier.fillMaxWidth()) { Text("Set Starting Point") }
            } else if (selectedTarget == null) Text("Unable to calculate the next point yet.")
            else {
                Text("Previous planted point: #${lastPoint.sequenceNumber}")
                Text("Selected direction: ${selectedTarget.direction.label}", style = MaterialTheme.typography.titleLarge)
                Text(if (distanceToTarget != null) "Distance to next point: ${String.format(Locale.US, "%.2f m", distanceToTarget)}" else "Distance to next point: waiting…")
                Text("Target bearing: ${targetBearing?.let { String.format(Locale.US, "%.0f°", it) } ?: "—"}")
                Text("GPS accuracy: ${gpsAccuracy?.let { String.format(Locale.US, "%.1f m", it) } ?: "—"}")
                Text("Phone heading: ${orientation?.headingDegrees?.let { String.format(Locale.US, "%.0f°", it) } ?: "—"}")
                if (arrived) {
                    Text("PLANT HERE", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    Button(onClick = { val current = location ?: return@Button; plantingViewModel.recordPlantedTarget(farm.id, crop.id, selectedTarget.latitude, selectedTarget.longitude, current.latitude, current.longitude) }, modifier = Modifier.fillMaxWidth()) { Text("MARK PLANTED") }
                } else Text("Move toward ${selectedTarget.direction.label}.")
                HorizontalDivider(); Text("Choose next direction")
                CardinalDirection.entries.forEach { direction ->
                    val target = targets.first { it.direction == direction }
                    OutlinedButton(onClick = { selectedDirection = direction }, modifier = Modifier.fillMaxWidth()) { Text(if (direction == selectedDirection) "→ ${direction.label} (selected)" else "→ ${direction.label}") }
                }
            }
        }
    }
}

private fun circularAngleDifference(first: Double, second: Double): Double {
    val difference = abs(normalizeBearing(first) - normalizeBearing(second))
    return minOf(difference, 360.0 - difference)
}

private fun normalizeBearing(value: Double): Double {
    var result = value % 360.0
    if (result < 0.0) result += 360.0
    return result
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationDiagnosticsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val locationViewModel: LocationViewModel = viewModel()
    val location by locationViewModel.location.collectAsStateWithLifecycle(initialValue = null)
    val orientation by locationViewModel.orientation.collectAsStateWithLifecycle(initialValue = null)
    val locationAvailable by locationViewModel.locationAvailable.collectAsStateWithLifecycle(initialValue = false)
    var permissionGranted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result -> permissionGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true || result[Manifest.permission.ACCESS_COARSE_LOCATION] == true }
    LaunchedEffect(permissionGranted) { if (permissionGranted) locationViewModel.start() }
    Scaffold(topBar = { TopAppBar(title = { Text("Location Diagnostics") }, navigationIcon = { TextButton(onClick = { locationViewModel.stop(); onBack() }) { Text("Back") } }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!permissionGranted) {
                Text("Location permission is required for GPS diagnostics.")
                Button(onClick = { permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }) { Text("Allow Location") }
            } else {
                Text("GPS enabled: $locationAvailable")
                if (location == null) Text("Waiting for a location fix…") else {
                    val current = location!!
                    Text("Latitude: ${current.latitude}"); Text("Longitude: ${current.longitude}"); Text("Accuracy: ${current.accuracyMeters ?: 0f} m"); Text("Speed: ${current.speedMetersPerSecond ?: 0f} m/s"); Text("Movement bearing: ${current.movementBearingDegrees ?: "—"}°"); Text("Mock location: ${current.isMock}")
                }
                HorizontalDivider(); Text("Rotation sensor: ${locationViewModel.hasRotationSensor}"); Text("Heading: ${orientation?.headingDegrees?.let { "$it°" } ?: "Waiting…"}"); Text("Orientation timestamp: ${orientation?.timestampMillis ?: "—"}")
                OutlinedButton(onClick = { if (locationViewModel.isLocationEnabled()) locationViewModel.start() else context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }) { Text("Refresh Location") }
            }
        }
    }
}
