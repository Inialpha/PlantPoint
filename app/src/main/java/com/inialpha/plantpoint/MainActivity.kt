package com.inialpha.plantpoint

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.DisposableEffect
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
import com.inialpha.plantpoint.ui.LocationViewModel
import com.inialpha.plantpoint.ui.PlantPointViewModel
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = PlantPointDatabase.getInstance(applicationContext)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val factory = remember { PlantPointViewModelFactory(database) }
                    val viewModel: PlantPointViewModel = viewModel(factory = factory)
                    PlantPointApp(viewModel)
                }
            }
        }
    }
}

private class PlantPointViewModelFactory(
    private val database: PlantPointDatabase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlantPointViewModel::class.java)) {
            return PlantPointViewModel(database.farmDao(), database.cropDao()) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

private enum class AppScreen { Farms, FarmDetail, LocationDiagnostics }

@Composable
private fun PlantPointApp(viewModel: PlantPointViewModel) {
    var selectedFarm by remember { mutableStateOf<FarmEntity?>(null) }
    var screen by remember { mutableStateOf(AppScreen.Farms) }

    when (screen) {
        AppScreen.Farms -> FarmListScreen(viewModel) {
            selectedFarm = it
            screen = AppScreen.FarmDetail
        }
        AppScreen.FarmDetail -> selectedFarm?.let { farm ->
            FarmDetailScreen(
                farm = farm,
                viewModel = viewModel,
                onBack = {
                    selectedFarm = null
                    screen = AppScreen.Farms
                },
                onOpenLocation = { screen = AppScreen.LocationDiagnostics }
            )
        }
        AppScreen.LocationDiagnostics -> LocationDiagnosticsScreen(
            onBack = { screen = AppScreen.FarmDetail }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FarmListScreen(viewModel: PlantPointViewModel, onOpenFarm: (FarmEntity) -> Unit) {
    val farms by viewModel.farms.collectAsStateWithLifecycle(initialValue = emptyList())
    var showAddFarm by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text("PlantPoint") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("My Farms", style = MaterialTheme.typography.headlineMedium)
            Text("Your farms are stored on this device and remain available offline.", style = MaterialTheme.typography.bodyMedium)
            Button(
                onClick = { showAddFarm = true },
                enabled = farms.size < PlantPointViewModel.MAX_FARMS,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (farms.size < PlantPointViewModel.MAX_FARMS) "Add Farm" else "Maximum of 3 Farms")
            }
            if (farms.isEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text("No farms yet. Add your first farm to begin.", style = MaterialTheme.typography.bodyLarge)
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(farms, key = { it.id }) { farm ->
                        FarmCard(farm, { onOpenFarm(farm) }, { viewModel.deleteFarm(farm) })
                    }
                }
            }
        }
    }
    if (showAddFarm) {
        AddFarmDialog({ showAddFarm = false }) { name ->
            viewModel.addFarm(name) { if (it) showAddFarm = false }
        }
    }
}

@Composable
private fun FarmCard(farm: FarmEntity, onOpen: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(farm.name, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Offline farm", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onOpen) { Text("Open Farm") }
                TextButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FarmDetailScreen(
    farm: FarmEntity,
    viewModel: PlantPointViewModel,
    onBack: () -> Unit,
    onOpenLocation: () -> Unit
) {
    val crops by viewModel.cropsForFarm(farm.id).collectAsStateWithLifecycle(initialValue = emptyList())
    var showAddCrop by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(farm.name) },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Crops", style = MaterialTheme.typography.headlineMedium)
            Text("Each crop keeps its own planting configuration and records.", style = MaterialTheme.typography.bodyMedium)
            Button(onClick = { showAddCrop = true }, modifier = Modifier.fillMaxWidth()) { Text("Add Crop") }
            HorizontalDivider()
            if (crops.isEmpty()) {
                Text("No crops have been added to this farm yet.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(crops, key = { it.id }) { crop -> CropCard(crop) { viewModel.deleteCrop(crop) } }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onOpenLocation, modifier = Modifier.fillMaxWidth()) { Text("Check Location & Sensors") }
            OutlinedButton(onClick = { /* Phase 4 */ }, modifier = Modifier.fillMaxWidth()) { Text("Start Planting (Next Phase)") }
        }
    }
    if (showAddCrop) {
        AddCropDialog({ showAddCrop = false }) { name, spacing ->
            viewModel.addCrop(farm.id, name, spacing) { if (it) showAddCrop = false }
        }
    }
}

@Composable
private fun CropCard(crop: CropEntity, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(crop.name, style = MaterialTheme.typography.titleMedium)
                Text("Plant spacing: ${formatMeters(crop.spacingMeters)} m", style = MaterialTheme.typography.bodyMedium)
            }
            TextButton(onClick = onDelete) { Text("Delete") }
        }
    }
}

@Composable
private fun AddFarmDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Farm") },
        text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Farm name") }, singleLine = true) },
        confirmButton = { Button(onClick = { onAdd(name) }, enabled = name.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AddCropDialog(onDismiss: () -> Unit, onAdd: (String, Double) -> Unit) {
    var name by remember { mutableStateOf("") }
    var spacing by remember { mutableStateOf("") }
    val spacingValue = spacing.toDoubleOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Crop") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Crop name") }, singleLine = true)
                OutlinedTextField(value = spacing, onValueChange = { spacing = it }, label = { Text("Plant spacing (metres)") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = { onAdd(name, spacingValue ?: 0.0) }, enabled = name.isNotBlank() && spacingValue != null && spacingValue > 0.0) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationDiagnosticsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val locationViewModel: LocationViewModel = viewModel()
    val location by locationViewModel.location.collectAsStateWithLifecycle()
    val orientation by locationViewModel.orientation.collectAsStateWithLifecycle()
    val locationAvailable by locationViewModel.locationAvailable.collectAsStateWithLifecycle()
    var permissionGranted by remember { mutableStateOf(hasFineLocationPermission(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        permissionGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }

    LaunchedEffect(permissionGranted) {
        if (permissionGranted) locationViewModel.start()
    }

    DisposableEffect(Unit) {
        onDispose { locationViewModel.stop() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Location & Sensors") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Phase 3 diagnostics", style = MaterialTheme.typography.headlineSmall)
            Text("This screen validates the on-device positioning and orientation subsystem before planting navigation is added.")
            if (!permissionGranted) {
                Text("Precise location permission is required for PlantPoint's planting measurements.")
                Button(onClick = {
                    permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                }, modifier = Modifier.fillMaxWidth()) { Text("Allow Precise Location") }
            } else {
                Text("Location permission: GRANTED")
                Text("Location providers: ${if (locationAvailable) "AVAILABLE" else "OFF / UNAVAILABLE"}")
                if (!locationAvailable) {
                    Button(onClick = { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Open Location Settings")
                    }
                }
                HorizontalDivider()
                Text("Position", style = MaterialTheme.typography.titleLarge)
                location?.let { sample ->
                    Text("Latitude: ${formatCoordinate(sample.latitude)}")
                    Text("Longitude: ${formatCoordinate(sample.longitude)}")
                    Text("Accuracy: ${sample.accuracyMeters?.let { "%.1f m".format(Locale.US, it) } ?: "unknown"}")
                    Text("Speed: ${sample.speedMetersPerSecond?.let { "%.2f m/s".format(Locale.US, it) } ?: "unknown"}")
                    Text("Movement bearing: ${sample.movementBearingDegrees?.let { "%.0f°".format(Locale.US, it) } ?: "not moving / unavailable"}")
                    Text("Timestamp: ${sample.timestampMillis}")
                } ?: Text("Waiting for a fresh location fix…")
                HorizontalDivider()
                Text("Device heading", style = MaterialTheme.typography.titleLarge)
                if (locationViewModel.hasRotationSensor) {
                    Text("Rotation-vector sensor: AVAILABLE")
                    Text("Heading: ${orientation.headingDegrees?.let { "%.0f°".format(Locale.US, it) } ?: "waiting…"}")
                } else {
                    Text("Rotation-vector sensor: NOT AVAILABLE")
                    Text("PlantPoint will not assume that device orientation is available on every phone.")
                }
                HorizontalDivider()
                Text("Keep the phone still for a few seconds to observe heading stability. Walk slowly to test movement bearing. GPS accuracy is an uncertainty estimate, not a guarantee of exact position.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun hasFineLocationPermission(context: android.content.Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

private fun formatCoordinate(value: Double): String = "%.6f".format(Locale.US, value)

private fun formatMeters(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(Locale.US, value)
