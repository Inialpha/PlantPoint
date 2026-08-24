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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PlantPointApp() }
    }
}

@Composable
private fun PlantPointApp() {
    Surface(modifier = Modifier.fillMaxSize()) {
        LocationDiagnosticsScreen(onBack = {})
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

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
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
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Phase 3 diagnostics", style = MaterialTheme.typography.headlineSmall)
            Text("This screen validates the on-device positioning and orientation subsystem before planting navigation is added.")

            if (!permissionGranted) {
                Text("Precise location permission is required for PlantPoint's planting measurements.")
                Button(
                    onClick = {
                        permissionLauncher.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                        )
                    }
                ) { Text("Grant location permission") }
            } else {
                Text("Location permission: granted")
                Text("Location available: ${if (locationAvailable) "Yes" else "No"}")
                if (!locationAvailable) {
                    OutlinedButton(onClick = {
                        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }) { Text("Open Location Settings") }
                }
                HorizontalDivider()
                Text("Latitude: ${location?.latitude?.formatCoordinate() ?: "Waiting…"}")
                Text("Longitude: ${location?.longitude?.formatCoordinate() ?: "Waiting…"}")
                Text("GPS accuracy: ${location?.accuracy?.let { "%.1f m".format(it) } ?: "Waiting…"}")
                Text("Speed: ${location?.speed?.let { "%.2f m/s".format(it) } ?: "Waiting…"}")
                Text("Movement bearing: ${location?.bearing?.let { "%.1f°".format(it) } ?: "Unavailable"}")
                HorizontalDivider()
                Text("Rotation-vector sensor: ${if (orientation.sensorAvailable) "Available" else "Unavailable"}")
                Text("Device heading: ${orientation.headingDegrees?.let { "%.1f°".format(it) } ?: "Unavailable"}")
            }
        }
    }
}

private fun hasFineLocationPermission(context: android.content.Context): Boolean =
    ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

private fun Double.formatCoordinate(): String = "%.7f".format(this)
