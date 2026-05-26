package com.opofit.miapp.gps.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.opofit.miapp.gps.model.ActivitySummary
import com.opofit.miapp.gps.model.SplitKm
import com.opofit.miapp.gps.util.GpsMetrics
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpsActivityDetailScreen(
    activityId: String,
    onNavigateBack: () -> Unit,
    viewModel: GpsViewModel = viewModel()
) {
    val activity: ActivitySummary? = remember(activityId) { viewModel.get(activityId) }
    var showDelete by remember { mutableStateOf(false) }

    if (activity == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Actividad") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                        }
                    }
                )
            }
        ) { pad ->
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Text("Actividad no encontrada")
            }
        }
        return
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            confirmButton = {
                Button(onClick = {
                    showDelete = false
                    viewModel.delete(activity.id)
                    onNavigateBack()
                }) { Text("Borrar") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDelete = false }) { Text("Cancelar") }
            },
            title = { Text("Borrar actividad") },
            text = { Text("Esta acción no se puede deshacer.") }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("${activity.type.emoji} ${activity.type.display}", fontWeight = FontWeight.SemiBold)
                        Text(
                            SimpleDateFormat("EEEE d MMM · HH:mm", Locale("es", "ES"))
                                .format(Date(activity.startedAtMs)),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showDelete = true }) {
                        Icon(Icons.Filled.Delete, "Borrar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { RouteMap(activity) }
            item { OverviewCard(activity) }
            item { MetricsGrid(activity) }
            if (activity.points.any { it.altitude != null }) {
                item { ElevationChartCard(activity) }
            }
            item { PaceChartCard(activity) }
            if (activity.splits.isNotEmpty()) {
                item {
                    Text(
                        "Parciales por km",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                items(activity.splits) { split -> SplitRow(split) }
            }
        }
    }
}

@Composable
private fun RouteMap(activity: ActivitySummary) {
    val pts = remember(activity.id) { activity.points.map { LatLng(it.lat, it.lng) } }
    val cameraPositionState = rememberCameraPositionState {
        val first = pts.firstOrNull() ?: LatLng(40.4168, -3.7038)
        position = CameraPosition.fromLatLngZoom(first, 14f)
    }
    androidx.compose.runtime.LaunchedEffect(activity.id) {
        if (pts.size > 1) {
            val builder = LatLngBounds.builder()
            pts.forEach { builder.include(it) }
            runCatching {
                cameraPositionState.move(CameraUpdateFactory.newLatLngBounds(builder.build(), 64))
            }
        }
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(mapType = MapType.NORMAL),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    compassEnabled = false,
                    scrollGesturesEnabled = false,
                    zoomGesturesEnabled = false
                )
            ) {
                if (pts.size >= 2) {
                    Polyline(points = pts, color = Color(0xFF1565C0), width = 10f)
                    Marker(state = MarkerState(pts.first()), title = "Inicio")
                    Marker(state = MarkerState(pts.last()), title = "Fin")
                }
            }
            if (pts.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Sin trazado GPS",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewCard(a: ActivitySummary) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                GpsMetrics.formatDistance(a.distanceM),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${GpsMetrics.formatDuration(a.durationSec)} · ${GpsMetrics.formatPace(a.avgPaceSecPerKm)}/km",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MetricsGrid(a: ActivitySummary) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("Velocidad media", GpsMetrics.formatSpeedKmh(a.avgSpeedMps), Modifier.weight(1f))
            MetricCard("Vel. máxima", GpsMetrics.formatSpeedKmh(a.maxSpeedMps), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("Ritmo mejor", "${GpsMetrics.formatPace(a.minPaceSecPerKm)}/km", Modifier.weight(1f))
            MetricCard("Ritmo peor", "${GpsMetrics.formatPace(a.maxPaceSecPerKm)}/km", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(
                "Altitud máx",
                a.elevationMaxM?.let { "${it.toInt()} m" } ?: "—",
                Modifier.weight(1f)
            )
            MetricCard(
                "Altitud mín",
                a.elevationMinM?.let { "${it.toInt()} m" } ?: "—",
                Modifier.weight(1f)
            )
            MetricCard(
                "Desnivel +",
                "${a.elevationGainM.toInt()} m",
                Modifier.weight(1f)
            )
        }
        a.avgCadenceSpm?.let {
            MetricCard("Cadencia media", "${it.toInt()} ppm", Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ElevationChartCard(activity: ActivitySummary) {
    val altitudes = remember(activity.id) { activity.points.mapNotNull { it.altitude } }
    if (altitudes.size < 2) return
    val minA = altitudes.min()
    val maxA = altitudes.max()
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text("Altitud", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                "${minA.toInt()} – ${maxA.toInt()} m  ·  desnivel +${activity.elevationGainM.toInt()} m",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LineChart(
                values = altitudes,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(top = 8.dp),
                strokeColor = Color(0xFF2E7D32)
            )
        }
    }
}

@Composable
private fun PaceChartCard(activity: ActivitySummary) {
    val splits = activity.splits.filter { it.paceSecPerKm > 0 }
    if (splits.size < 2) {
        if (activity.points.size >= 4) {
            val speeds = activity.points.mapNotNull { it.speedMps?.toDouble() }
            if (speeds.size >= 4) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            "Velocidad",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        LineChart(
                            values = speeds.map { it * 3.6 },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .padding(top = 8.dp),
                            strokeColor = Color(0xFF1565C0)
                        )
                    }
                }
            }
        }
        return
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text("Ritmo por km", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            LineChart(
                values = splits.map { it.paceSecPerKm },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(top = 8.dp),
                strokeColor = Color(0xFF6A1B9A),
                invert = true
            )
        }
    }
}

@Composable
private fun LineChart(
    values: List<Double>,
    modifier: Modifier = Modifier,
    strokeColor: Color,
    invert: Boolean = false
) {
    if (values.size < 2) return
    val minV = values.min()
    val maxV = values.max()
    val range = (maxV - minV).coerceAtLeast(0.0001)
    Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))) {
        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val path = Path()
            val w = size.width
            val h = size.height
            values.forEachIndexed { i, v ->
                val x = if (values.size == 1) w / 2 else w * i / (values.size - 1)
                val normalized = ((v - minV) / range).toFloat().coerceIn(0f, 1f)
                val y = if (invert) normalized * h else (1f - normalized) * h
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color = strokeColor, style = Stroke(width = 4f))
            val firstX = 0f
            val firstNorm = ((values.first() - minV) / range).toFloat().coerceIn(0f, 1f)
            val firstY = if (invert) firstNorm * h else (1f - firstNorm) * h
            drawCircle(strokeColor, radius = 4f, center = Offset(firstX, firstY))
        }
    }
}

@Composable
private fun SplitRow(split: SplitKm) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("KM ${split.km}", fontWeight = FontWeight.SemiBold, modifier = Modifier.size(width = 70.dp, height = 24.dp))
            Text(GpsMetrics.formatDuration(split.durationSec))
            Text("${GpsMetrics.formatPace(split.paceSecPerKm)}/km", fontWeight = FontWeight.SemiBold)
            Text(
                "+${split.elevationGainM.toInt()} m",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall
            )
        }
        HorizontalDivider()
    }
}
