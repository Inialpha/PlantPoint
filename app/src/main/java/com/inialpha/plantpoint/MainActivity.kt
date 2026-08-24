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
    val farmViewModel: PlantPointViewModel = viewModel(
        factory = PlantPointViewModelFactory(context)
    )
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
                    Card(
                        onClick = { selectedFarmId = farm.id },
                        modifier = Modifier.fillMaxWidth()
                    ) {
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
    val farmViewModel: PlantPointViewModel = viewModel(
        factory = PlantPointViewModelFactory(context)
    )
    val crops by farmViewModel.cropsForFarm(farm.id).collectAsStateWithLifecycle(initialValue = emptyList())
    var showAddCrop by remember { mutableStateOf(false) }
    var showDiagnostics by remember { mutableStateOf(false) }

    if (showDiagnostics) {
        LocationDiagnosticsScreen(onBack = { showDiagnostics = false })
        return
    }
