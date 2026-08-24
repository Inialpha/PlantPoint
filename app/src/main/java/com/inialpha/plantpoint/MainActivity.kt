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
import com.inialpha.plantpoint.ui.LocationViewModel
import com.inialpha.plantpoint.ui.PlantPointViewModel
import java.util.Locale

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
                enabled = farms.size < PlantPointViewModel.MAX_FARMS,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (farms.size < PlantPointViewModel.MAX_FARMS) "Add Farm" else "Maximum of 3 Farms")
            }
        }
    }

    if (showAddFarm) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddFarm = false },
            title = { Text("Add Farm") },
            text = { OutlinedTextField(name, { name = it }, label = { Text("Farm name") }) },
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
private fun FarmDetailScreen(farm: FarmEntity, onBack: () -> Unit) {
    val context = LocalContext.current
    val farmViewModel: PlantPointViewModel = viewModel(factory = PlantPointViewModelFactory(context))
    val crops by farmViewModel.cropsForFarm(farm.id).collectAsStateWithLifecycle(initialValue = emptyList())
    var showAddCrop by remember { mutableStateOf(false) }
    var showDiagnostics by remember { mutableStateOf(false) }

    if (showDiagnostics) {
        LocationDiagnosticsScreen(onBack = { showDiagnostics = false })
        return
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(farm.name) },
            navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
        )
    }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Crops", style = MaterialTheme.typography.headlineSmall)
            if (crops.isEmpty()) Text("No crops configured for this farm yet.")
            crops.forEach { crop ->
                CropRow(crop = crop, onDelete = { farmViewModel.deleteCrop(crop) })
            }
            Button(onClick = { showAddCrop = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Add Crop")
            }
            OutlinedButton(onClick = { showDiagnostics = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Location & Direction Diagnostics")
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
                    OutlinedTextField(name, { name = it }, label = { Text("Crop name") })
                    OutlinedTextField(spacing, { spacing = it }, label = { Text("Spacing (meters)") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val meters = spacing.toDoubleOrNull()
                    if (name.isNotBlank() && meters != null && meters > 0.0) {
                        farmViewModel.addCrop(farm.id, name.trim(), meters)
                        showAddCrop = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddCrop = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun CropRow(crop: CropEntity, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(crop.name, style = MaterialTheme.typography.titleMedium)
                Text(String.format(Locale.US, "%.2f m spacing", crop.spacingMeters))
            }
            TextButton(onClick = onDelete) { Text("Delete") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationDiagnosticsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val locationViewModel: LocationViewModel = viewModel()
    val location by locationViewModel.location.collectAsStateWithLifecycle(initialValue = null)
    val orientation by locationViewModel.orientation.collectAsStateWithLifecycle(initialValue = null)
    val locationAvailable by locationViewModel.locationAvailable.collectAsStateWithLifecycle(initialValue = false)
    var permissionGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        permissionGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(permissionGranted) {
        if (permissionGranted) locationViewModel.start()
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Location Diagnostics") },
            navigationIcon = { TextButton(onClick = { locationViewModel.stop(); onBack() }) { Text("Back") } }
        )
    }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!permissionGranted) {
                Text("Location permission is required for GPS diagnostics.")
                Button(onClick = {
                    permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                }) { Text("Allow Location") }
            } else {
                Text("GPS enabled: $locationAvailable")
                if (location == null) {
                    Text("Waiting for a location fix…")
                } else {
                    val current = location!!
                    Text("Latitude: ${current.latitude}")
                    Text("Longitude: ${current.longitude}")
                    Text("Accuracy: ${current.accuracyMeters ?: 0f} m")
                    Text("Speed: ${current.speedMetersPerSecond ?: 0f} m/s")
                    Text("Movement bearing: ${current.movementBearingDegrees ?: "—"}°")
                    Text("Mock location: ${current.isMock}")
                }
                HorizontalDivider()
                Text("Rotation sensor: ${locationViewModel.hasRotationSensor}")
                Text("Heading: ${orientation?.headingDegrees?.let { "$it°" } ?: "Waiting…"}")
                Text("Orientation timestamp: ${orientation?.timestampMillis ?: "—"}")
                OutlinedButton(onClick = {
                    if (locationViewModel.isLocationEnabled()) locationViewModel.start()
                    else context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }) { Text("Refresh Location") }
            }
        }
    }
}
