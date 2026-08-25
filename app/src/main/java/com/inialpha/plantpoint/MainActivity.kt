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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inialpha.plantpoint.data.local.CropEntity
import com.inialpha.plantpoint.data.local.FarmEntity
import com.inialpha.plantpoint.data.local.PlantPointDatabase
import com.inialpha.plantpoint.data.local.PlantingPointEntity
import com.inialpha.plantpoint.data.navigation.CardinalDirection
import com.inialpha.plantpoint.data.navigation.GridNeighbor
import com.inialpha.plantpoint.data.navigation.PlantingGridEngine
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
    var historyCrop by remember { mutableStateOf<CropEntity?>(null) }
    if (showDiagnostics) { LocationDiagnosticsScreen(onBack = { showDiagnostics = false }); return }
    val activeCrop = plantingCrop
    if (activeCrop != null) { PlantingNavigationScreen(farm = farm, crop = activeCrop, onBack = { plantingCrop = null }); return }
    val historyTargetCrop = historyCrop
    if (historyTargetCrop != null) { PlantingHistoryScreen(crop = historyTargetCrop, onBack = { historyCrop = null }); return }
    Scaffold(topBar = { TopAppBar(title = { Text(farm.name) }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Crops", style = MaterialTheme.typography.headlineSmall)
            if (crops.isEmpty()) Text("No crops configured for this farm yet.")
            crops.forEach { crop ->
                CropRow(
                    crop,
                    onDelete = { farmViewModel.deleteCrop(crop) },
                    onStartPlanting = { plantingCrop = crop },
                    onViewHistory = { historyCrop = crop }
                )
            }
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
private fun CropRow(crop: CropEntity, onDelete: () -> Unit, onStartPlanting: () -> Unit, onViewHistory: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column { Text(crop.name, style = MaterialTheme.typography.titleMedium); Text(String.format(Locale.US, "%.2f m spacing", crop.spacingMeters)) }
                TextButton(onClick = onDelete) { Text("Delete") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onStartPlanting, modifier = Modifier.weight(1f)) { Text("Start Planting") }
                OutlinedButton(onClick = onViewHistory, modifier = Modifier.weight(1f)) { Text("History") }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Planting grid navigation screen (Phase 4 visual navigation).
//
// Coordinate convention: the farm/crop starting point is grid (row 0, column 0). Increasing
// row = geographic north, increasing column = geographic east (see PlantingGridEngine). The
// screen shows this local grid north-up so grid-relative directions and compass directions are
// never in conflict. A separate "turn arrow" shows the bearing to the selected target RELATIVE
// TO THE PHONE'S CURRENT HEADING (i.e. which way to physically turn), which is a different,
// clearly-labeled concept from the grid's fixed north-up layout.
// ---------------------------------------------------------------------------------------------

private data class GridNeighborState(
    val neighbor: GridNeighbor,
    val plannedLatitude: Double,
    val plannedLongitude: Double,
    val existing: PlantingPointEntity?
)

private enum class AccuracyTier { HIGH, MEDIUM, LOW, UNKNOWN }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlantingNavigationScreen(farm: FarmEntity, crop: CropEntity, onBack: () -> Unit) {
    val context = LocalContext.current
    val locationViewModel: LocationViewModel = viewModel()
    val plantingViewModel: PlantingPointViewModel = viewModel(factory = PlantingPointViewModelFactory(context))
    val location by locationViewModel.location.collectAsStateWithLifecycle(initialValue = null)
    val orientation by locationViewModel.orientation.collectAsStateWithLifecycle(initialValue = null)
    val points by plantingViewModel.pointsForCrop(crop.id).collectAsStateWithLifecycle(initialValue = emptyList())
    val otherCropPoints by plantingViewModel.otherCropPointsFor(farm.id, crop.id).collectAsStateWithLifecycle(initialValue = emptyList())
    var permissionGranted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result -> permissionGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true || result[Manifest.permission.ACCESS_COARSE_LOCATION] == true }
    LaunchedEffect(permissionGranted) { if (permissionGranted) locationViewModel.start() }

    Scaffold(topBar = { TopAppBar(title = { Text("Plant ${crop.name}") }, navigationIcon = { TextButton(onClick = { locationViewModel.stop(); onBack() }) { Text("Back") } }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!permissionGranted) {
                Text("Location permission is required to navigate planting points.")
                Button(onClick = { permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }) { Text("Allow Location") }
                return@Column
            }

            val gpsAccuracy = location?.accuracyMeters?.toDouble()
            val accuracyTier = when {
                location == null -> AccuracyTier.UNKNOWN
                gpsAccuracy == null -> AccuracyTier.UNKNOWN
                gpsAccuracy <= 2.0 -> AccuracyTier.HIGH
                gpsAccuracy <= 5.0 -> AccuracyTier.MEDIUM
                else -> AccuracyTier.LOW
            }
            StatusRow(accuracyTier = accuracyTier, accuracyMeters = gpsAccuracy, hasHeading = orientation != null)

            if (location == null) {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Waiting for a GPS fix…", style = MaterialTheme.typography.titleMedium)
                Text("Keep the phone in an open area until a location appears.")
                return@Column
            }

            val originPoint = points.firstOrNull { it.gridRow == 0 && it.gridColumn == 0 }
            if (originPoint == null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Stand at the exact spot where planting should begin.", style = MaterialTheme.typography.titleMedium)
                Text("This becomes grid position (0, 0) for ${crop.name}. Spacing: ${String.format(Locale.US, "%.2f m", crop.spacingMeters)}")
                Button(
                    onClick = {
                        val current = location ?: return@Button
                        plantingViewModel.createGridOrigin(farm.id, crop.id, current.latitude, current.longitude, current.accuracyMeters?.toDouble())
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Set Starting Point") }
                return@Column
            }

            val originLatitude = originPoint.actualLatitude ?: originPoint.plannedLatitude
            val originLongitude = originPoint.actualLongitude ?: originPoint.plannedLongitude
            val pointsByGrid = remember(points) { points.associateBy { it.gridRow to it.gridColumn } }
            val plantedPoints = remember(points) { points.filter { it.status == PlantingPointEntity.STATUS_PLANTED } }
            val anchorPoint = plantedPoints.maxByOrNull { it.sequenceNumber } ?: originPoint

            val neighborStates = remember(anchorPoint.id, originLatitude, originLongitude, crop.spacingMeters, pointsByGrid) {
                PlantingGridEngine.neighbors(anchorPoint.gridRow, anchorPoint.gridColumn).map { n ->
                    val destination = PlantingGridEngine.pointFor(originLatitude, originLongitude, crop.spacingMeters, n.row, n.column)
                    GridNeighborState(n, destination.first, destination.second, pointsByGrid[n.row to n.column])
                }
            }

            var selectedNeighbor by remember(anchorPoint.id) { mutableStateOf<GridNeighbor?>(null) }
            LaunchedEffect(anchorPoint.id, orientation?.headingDegrees, neighborStates) {
                if (selectedNeighbor == null && neighborStates.isNotEmpty()) {
                    val heading = orientation?.headingDegrees
                    selectedNeighbor = if (heading != null) {
                        neighborStates.minByOrNull { circularAngleDifference(heading.toDouble(), it.neighbor.direction.bearingDegrees) }?.neighbor
                    } else {
                        neighborStates.first().neighbor
                    }
                }
            }

            val selectedState = neighborStates.firstOrNull { it.neighbor == selectedNeighbor }
            val currentLocation = location
            val distanceToTarget: Double? = if (currentLocation != null && selectedState != null) {
                val results = FloatArray(3)
                Location.distanceBetween(currentLocation.latitude, currentLocation.longitude, selectedState.plannedLatitude, selectedState.plannedLongitude, results)
                results[0].toDouble()
            } else null
            val targetBearing: Double? = if (currentLocation != null && selectedState != null) {
                val results = FloatArray(3)
                Location.distanceBetween(currentLocation.latitude, currentLocation.longitude, selectedState.plannedLatitude, selectedState.plannedLongitude, results)
                normalizeBearing(results[1].toDouble())
            } else null

            val arrivalTolerance = if (gpsAccuracy != null) maxOf(1.0, minOf(3.0, gpsAccuracy)) else 1.5
            val alreadyPlanted = selectedState?.existing?.status == PlantingPointEntity.STATUS_PLANTED
            val arrived = distanceToTarget != null && distanceToTarget <= arrivalTolerance && !alreadyPlanted

            var previousDistance by remember(selectedNeighbor) { mutableStateOf<Double?>(null) }
            val movingAway = previousDistance != null && distanceToTarget != null && distanceToTarget > (previousDistance ?: 0.0) + 0.3
            LaunchedEffect(distanceToTarget) { if (distanceToTarget != null) previousDistance = distanceToTarget }

            val proximityWarningNeighbor: GridNeighbor? = remember(neighborStates, otherCropPoints, crop.spacingMeters) {
                val warnDistance = maxOf(1.0, crop.spacingMeters * 0.3).toFloat()
                neighborStates.firstOrNull { state ->
                    otherCropPoints.any { other ->
                        val lat = other.actualLatitude ?: other.plannedLatitude
                        val lon = other.actualLongitude ?: other.plannedLongitude
                        val results = FloatArray(3)
                        Location.distanceBetween(state.plannedLatitude, state.plannedLongitude, lat, lon, results)
                        results[0] <= warnDistance
                    }
                }?.neighbor
            }

            val farDistance = maxOf(crop.spacingMeters, 3.0)
            val progress = if (distanceToTarget != null) {
                val denom = (farDistance - arrivalTolerance).coerceAtLeast(0.1)
                (1.0 - ((distanceToTarget - arrivalTolerance) / denom).coerceIn(0.0, 1.0)).toFloat().coerceIn(0f, 1f)
            } else 0f

            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TargetCompassDial(
                    heading = orientation?.headingDegrees,
                    targetBearing = targetBearing,
                    progress = progress,
                    arrived = arrived
                )
            }

            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = selectedState?.neighbor?.direction?.label?.let { "Target: $it" } ?: "Choose a target",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = if (distanceToTarget != null) String.format(Locale.US, "%.2f m away", distanceToTarget) else "Waiting for a stable fix…",
                    style = MaterialTheme.typography.bodyLarge
                )
                if (movingAway && !arrived) {
                    Text("Moving away from target", color = Color(0xFFC62828), style = MaterialTheme.typography.labelLarge)
                }
                if (proximityWarningNeighbor != null && proximityWarningNeighbor == selectedNeighbor) {
                    Text("⚠ Close to a point planted for another crop", color = Color(0xFFF9A825), style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            PlantingGridLayout(
                neighborStates = neighborStates,
                selectedNeighbor = selectedNeighbor,
                warningNeighbor = proximityWarningNeighbor,
                onSelect = { state -> selectedNeighbor = state.neighbor }
            )

            Spacer(modifier = Modifier.height(4.dp))
            if (alreadyPlanted) {
                OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("Already planted") }
            } else {
                Button(
                    onClick = {
                        val current = location ?: return@Button
                        val target = selectedState ?: return@Button
                        plantingViewModel.plantGridPoint(
                            farmId = farm.id,
                            cropId = crop.id,
                            row = target.neighbor.row,
                            column = target.neighbor.column,
                            plannedLatitude = target.plannedLatitude,
                            plannedLongitude = target.plannedLongitude,
                            actualLatitude = current.latitude,
                            actualLongitude = current.longitude,
                            accuracyMeters = current.accuracyMeters?.toDouble()
                        )
                    },
                    enabled = arrived,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text(if (arrived) "PLANT HERE" else "Move toward target", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun StatusRow(accuracyTier: AccuracyTier, accuracyMeters: Double?, hasHeading: Boolean) {
    val (label, color) = when (accuracyTier) {
        AccuracyTier.HIGH -> "HIGH CONFIDENCE" to Color(0xFF2E7D32)
        AccuracyTier.MEDIUM -> "MEDIUM CONFIDENCE" to Color(0xFFF9A825)
        AccuracyTier.LOW -> "LOW CONFIDENCE" to Color(0xFFC62828)
        AccuracyTier.UNKNOWN -> "NO FIX" to Color(0xFF757575)
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(color.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = color)
            if (accuracyMeters != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(String.format(Locale.US, "±%.1fm", accuracyMeters), style = MaterialTheme.typography.labelSmall)
            }
        }
        val headingColor = if (hasHeading) Color(0xFF2E7D32) else Color(0xFF757575)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(headingColor.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Box(modifier = Modifier.size(10.dp).background(headingColor, CircleShape))
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (hasHeading) "COMPASS OK" else "NO COMPASS", style = MaterialTheme.typography.labelMedium, color = headingColor)
        }
    }
}

@Composable
private fun TargetCompassDial(
    heading: Float?,
    targetBearing: Double?,
    progress: Float,
    arrived: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "arrival-pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(animation = tween(900, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "pulse"
    )
    val animatedProgress by animateFloatAsState(targetValue = progress.coerceIn(0f, 1f), label = "progress")
    val ringColor = lerpColor(Color(0xFFC62828), Color(0xFF2E7D32), animatedProgress)
    val relativeBearing = if (heading != null && targetBearing != null) {
        Math.toRadians((targetBearing - heading)).toFloat()
    } else null

    Box(modifier = modifier.size(220.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            drawArc(
                color = ringColor.copy(alpha = 0.25f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            if (arrived) {
                drawCircle(color = ringColor.copy(alpha = 0.18f), radius = size.minDimension / 2f * pulse)
            } else if (relativeBearing != null) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val armLength = size.minDimension / 2f - strokeWidth * 2.2f
                val tip = Offset(
                    center.x + kotlin.math.sin(relativeBearing) * armLength,
                    center.y - kotlin.math.cos(relativeBearing) * armLength
                )
                drawLine(color = ringColor, start = center, end = tip, strokeWidth = strokeWidth * 0.7f, cap = StrokeCap.Round)
                val headSize = strokeWidth * 1.6f
                val leftWing = Offset(
                    tip.x - kotlin.math.sin(relativeBearing + 2.6f) * headSize,
                    tip.y + kotlin.math.cos(relativeBearing + 2.6f) * headSize
                )
                val rightWing = Offset(
                    tip.x - kotlin.math.sin(relativeBearing - 2.6f) * headSize,
                    tip.y + kotlin.math.cos(relativeBearing - 2.6f) * headSize
                )
                drawLine(color = ringColor, start = tip, end = leftWing, strokeWidth = strokeWidth * 0.7f, cap = StrokeCap.Round)
                drawLine(color = ringColor, start = tip, end = rightWing, strokeWidth = strokeWidth * 0.7f, cap = StrokeCap.Round)
            }
        }
        Text(
            text = when {
                arrived -> "PLANT\nHERE"
                heading == null -> "NO\nCOMPASS"
                else -> ""
            },
            style = MaterialTheme.typography.titleMedium,
            color = ringColor,
            textAlign = TextAlign.Center
        )
    }
}

private fun lerpColor(start: Color, end: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * f,
        green = start.green + (end.green - start.green) * f,
        blue = start.blue + (end.blue - start.blue) * f,
        alpha = start.alpha + (end.alpha - start.alpha) * f
    )
}

@Composable
private fun PlantingGridLayout(
    neighborStates: List<GridNeighborState>,
    selectedNeighbor: GridNeighbor?,
    warningNeighbor: GridNeighbor?,
    onSelect: (GridNeighborState) -> Unit
) {
    fun byDirection(direction: CardinalDirection) = neighborStates.firstOrNull { it.neighbor.direction == direction }

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            GridCell(byDirection(CardinalDirection.NORTHWEST), selectedNeighbor, warningNeighbor, onSelect)
            GridCell(byDirection(CardinalDirection.NORTH), selectedNeighbor, warningNeighbor, onSelect)
            GridCell(byDirection(CardinalDirection.NORTHEAST), selectedNeighbor, warningNeighbor, onSelect)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
            GridCell(byDirection(CardinalDirection.WEST), selectedNeighbor, warningNeighbor, onSelect)
            AnchorCell()
            GridCell(byDirection(CardinalDirection.EAST), selectedNeighbor, warningNeighbor, onSelect)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            GridCell(byDirection(CardinalDirection.SOUTHWEST), selectedNeighbor, warningNeighbor, onSelect)
            GridCell(byDirection(CardinalDirection.SOUTH), selectedNeighbor, warningNeighbor, onSelect)
            GridCell(byDirection(CardinalDirection.SOUTHEAST), selectedNeighbor, warningNeighbor, onSelect)
        }
    }
}

@Composable
private fun AnchorCell() {
    Box(
        modifier = Modifier
            .size(60.dp)
            .background(Color(0xFF1565C0), CircleShape)
            .border(2.dp, Color.White, CircleShape)
    )
}

@Composable
private fun GridCell(
    state: GridNeighborState?,
    selectedNeighbor: GridNeighbor?,
    warningNeighbor: GridNeighbor?,
    onSelect: (GridNeighborState) -> Unit
) {
    if (state == null) {
        Spacer(modifier = Modifier.size(60.dp))
        return
    }
    val isSelected = state.neighbor == selectedNeighbor
    val hasWarning = state.neighbor == warningNeighbor
    val plantedByThisCrop = state.existing?.status == PlantingPointEntity.STATUS_PLANTED
    val borderColor = when {
        isSelected -> Color(0xFF1565C0)
        plantedByThisCrop -> Color(0xFF2E7D32)
        else -> Color(0xFFBDBDBD)
    }
    val fillAlpha = if (plantedByThisCrop) 0.85f else 0f
    Box(
        modifier = Modifier
            .size(60.dp)
            .clickable { onSelect(state) }
            .background(color = Color(0xFF2E7D32).copy(alpha = fillAlpha), shape = CircleShape)
            .border(width = if (isSelected) 3.dp else 2.dp, color = borderColor, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (plantedByThisCrop) {
            Canvas(modifier = Modifier.size(22.dp)) {
                val w = size.width
                val h = size.height
                drawLine(Color.White, Offset(w * 0.15f, h * 0.55f), Offset(w * 0.4f, h * 0.8f), strokeWidth = 5f, cap = StrokeCap.Round)
                drawLine(Color.White, Offset(w * 0.4f, h * 0.8f), Offset(w * 0.85f, h * 0.2f), strokeWidth = 5f, cap = StrokeCap.Round)
            }
        }
        if (hasWarning) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(14.dp)
                    .background(Color(0xFFF9A825), CircleShape)
            )
        }
    }
}

@Composable
private fun PlantingHistoryScreen(crop: CropEntity, onBack: () -> Unit) {
    val context = LocalContext.current
    val plantingViewModel: PlantingPointViewModel = viewModel(factory = PlantingPointViewModelFactory(context))
    val points by plantingViewModel.pointsForCrop(crop.id).collectAsStateWithLifecycle(initialValue = emptyList())
    Scaffold(topBar = { TopAppBar(title = { Text("${crop.name} History") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (points.isEmpty()) {
                Text("No planting points recorded yet for ${crop.name}.")
            } else {
                Text("${points.size} point(s) recorded", style = MaterialTheme.typography.titleMedium)
                points.sortedByDescending { it.sequenceNumber }.forEach { point -> PlantingHistoryRow(point) }
            }
        }
    }
}

@Composable
private fun PlantingHistoryRow(point: PlantingPointEntity) {
    val planted = point.status == PlantingPointEntity.STATUS_PLANTED
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("#${point.sequenceNumber} · grid (${point.gridRow}, ${point.gridColumn})", style = MaterialTheme.typography.titleSmall)
                Text(if (planted) "Planted" else "Planned", style = MaterialTheme.typography.bodySmall)
            }
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(if (planted) Color(0xFF2E7D32) else Color(0xFFBDBDBD), CircleShape)
            )
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
