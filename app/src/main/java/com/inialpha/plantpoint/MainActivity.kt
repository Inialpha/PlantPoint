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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inialpha.plantpoint.data.location.LocationSnapshot
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PlantPointApp() }
    }
}

@Composable
private fun PlantPointApp() {
    Surface(modifier = Modifier.fillMaxSize()) {
        FarmListScreen()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FarmListScreen(
    farmViewModel: PlantPointViewModel = viewModel()
) {
    val farms by farmViewModel.farms.collectAsStateWithLifecycle()
    var showAddFarm by remember { mutableStateOf(false) }
    var selectedFarmId by remember { mutableStateOf<Long?>(null) }
    val selectedFarm = farms.firstOrNull { it.id == selectedFarmId }

    if (selectedFarm != null) {
        FarmDetailScreen(
            farm = selectedFarm,
            onBack = { selectedFarmId = null },
            onOpenLocationDiagnostics = { /* Navigation is added without replacing Phase 2 state. */ }
        )
        return
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("PlantPoint") }) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("My Farms", style = MaterialTheme.typography.headlineMedium)
            if (farms.isEmpty()) {
                Text("No farms yet. Add your first farm to begin.")
            } else {
                farms.forEach { farm ->
                    Card(onClick = { selectedFarmId = farm.id }, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(farm.name, style = MaterialTheme.typography.titleMedium)
                            TextButton(onClick = { farmViewModel.deleteFarm(farm) }) { Text("Delete") }
                        }
                    }
                }
            }
            Button(
                onClick = { showAddFarm = true },
                enabled = farms.size < 3,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (farms.size < 3) "Add Farm" else "Maximum of 3 Farms") }
        }
    }

    if (showAddFarm) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddFarm = false },
            title = { Text("Add Farm") },
            text = {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Farm name") })
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        farmViewModel.addFarm(name.trim())
                        showAddFarm = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddFarm = false }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FarmDetailScreen(
    farm: FarmEntity,
    onBack: () -> Unit,
    onOpenLocationDiagnostics: () -> Unit,
    farmViewModel: PlantPointViewModel = viewModel()
) {
    val crops by farmViewModel.cropsForFarm(farm.id).collectAsState(initial = emptyList())
    var showAddCrop by remember { mutableStateOf(false) }
    var showDiagnostics by remember { mutableStateOf(false) }

    if (showDiagnostics) {
        LocationDiagnosticsScreen(onBack = { showDiagnostics = false })
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(farm.name) },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Crops", style = MaterialTheme.typography.headlineSmall)
            if (crops.isEmpty()) Text("No crops added yet.")
            crops.forEach { crop ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(crop.name, style = MaterialTheme.typography.titleMedium)
                            Text("Spacing: ${crop.spacingMeters} m")
                        }
                        TextButton(onClick = { farmViewModel.deleteCrop(crop) }) { Text("Delete") }
                    }
                }
            }
            Button(onClick = { showAddCrop = true }, modifier = Modifier.fillMaxWidth()) { Text("Add Crop") }
            OutlinedButton(onClick = { showDiagnostics = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Check Location & Sensors")
            }
            Button(onClick = { /* Phase 4 */ }, enabled = crops.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                Text("Start Planting")
            }
        }
    }

    if (showAddCrop) {
        var name by remember { mutableStateOf("") }
        var spacing by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCrop = false },
            title = { Text("Add Crop") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Crop name") })
                    OutlinedTextField(value = spacing, onValueChange = { spacing = it }, label = { Text("Spacing in metres") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val metres = spacing.toDoubleOrNull()
                    if (name.isNotBlank() && metres != null && metres > 0) {
                        farmViewModel.addCrop(farm.id, name.trim(), metres)
                        showAddCrop = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddCrop = false }) { Text("Cancel") } }
        )
    }
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

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Location & Sensors") }, navigationIcon = {
                TextButton(onClick = onBack) { Text("Back") }
            })
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Phase 3 diagnostics", style = MaterialTheme.typography.headlineSmall)
            if (!permissionGranted) {
                Text("Precise location permission is required for planting measurements.")
                Button(onClick = {
                    permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                }) { Text("Grant location permission") }
            } else {
                Text("Location permission: granted")
                Text("Location available: ${if (locationAvailable) "Yes" else "No"}")
                if (!locationAvailable) {
                    OutlinedButton(onClick = {
                        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }) { Text("Open Location Settings") }
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
                    Text("Heading: ${orientation?.headingDegrees?.let { "%.0f°".format(Locale.US, it) } ?: "waiting…"}")
                } else {
                    Text("Rotation-vector sensor: NOT AVAILABLE")
                    Text("PlantPoint will not assume device orientation is available on every phone.")
                }
            }
        }
    }
}

private fun hasFineLocationPermission(context: android.content.Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

private fun formatCoordinate(value: Double): String = "%.7f".format(Locale.US, value)
