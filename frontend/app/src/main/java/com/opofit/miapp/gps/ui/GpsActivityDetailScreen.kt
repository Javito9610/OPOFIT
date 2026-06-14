package com.opofit.miapp.gps.ui

import com.opofit.miapp.ui.components.ElevatedCard
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.opofit.miapp.gps.model.SplitMile
import com.opofit.miapp.gps.model.SplitTime
import com.opofit.miapp.gps.service.ShareActivityContext
import com.opofit.miapp.gps.service.buildPendingShareFromGps
import com.opofit.miapp.gps.util.GpsMetrics
import com.opofit.miapp.ui.components.HeartRateZoneBreakdown
import com.opofit.miapp.gps.util.GpxExport
import com.opofit.miapp.ui.components.LineAreaChart
import com.opofit.miapp.ui.components.MetricBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val SpeedPalette = listOf(
    Color(0xFFB71C1C),
    Color(0xFFEF6C00),
    Color(0xFFFBC02D),
    Color(0xFF388E3C),
    Color(0xFF1565C0)
)

private enum class SplitMode(val label: String) {
    KM("Por km"), MILE("Por milla"), TIME("Cada 5 min")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpsActivityDetailScreen(
    activityId: String,
    onNavigateBack: () -> Unit,
    onShareToProfile: () -> Unit = {},
    viewModel: GpsViewModel = viewModel()
) {
    val activity: ActivitySummary? = remember(activityId) { viewModel.get(activityId) }
    var showDelete by remember { mutableStateOf(false) }
    val context = LocalContext.current

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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                com.opofit.miapp.ui.components.EnfoqueIcons.forActivityType(activity.type),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(activity.type.display, fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            SimpleDateFormat("EEEE d MMM · HH:mm", Locale.forLanguageTag("es-ES"))
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
                    IconButton(onClick = {
                        val intent = GpxExport.shareIntent(context, activity)
                        if (intent != null) {
                            context.startActivity(
                                android.content.Intent.createChooser(intent, "Compartir GPX")
                            )
                        }
                    }) {
                        Icon(Icons.Filled.Share, "Compartir GPX")
                    }
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
            item {
                OutlinedButton(
                    onClick = {
                        ShareActivityContext.set(buildPendingShareFromGps(activity))
                        onShareToProfile()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Share, null, Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Publicar en mi perfil")
                }
            }
            item { OverviewCard(activity) }
            item { MetricsGrid(activity) }
            if (activity.bestSegments.isNotEmpty()) {
                item { BestSegmentsCard(activity) }
            }
            if (activity.points.any { it.altitude != null }) {
                item { ElevationChartCard(activity) }
            }
            if (activity.points.any { it.hrBpm != null }) {
                item { HrChartCard(activity) }
            }
            item { PaceChartCard(activity) }
            item { SplitsCard(activity) }
        }
    }
}

@Composable
private fun RouteMap(activity: ActivitySummary) {
    val pts = remember(activity.id) { activity.points.map { LatLng(it.lat, it.lng) } }
    val buckets = remember(activity.id) { GpsMetrics.colorBucketsBySpeed(activity.points) }
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
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(240.dp)) {
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
                        // Pintamos la polilínea por segmentos coloreada según velocidad.
                        val segCount = (pts.size - 1).coerceAtMost(buckets.size - 1)
                        for (i in 0 until segCount) {
                            val bucket = buckets.getOrNull(i + 1)?.coerceIn(0, SpeedPalette.size - 1) ?: 2
                            Polyline(
                                points = listOf(pts[i], pts[i + 1]),
                                color = SpeedPalette[bucket],
                                width = 12f
                            )
                        }
                        Marker(state = MarkerState(pts.first()), title = "Inicio")
                        Marker(state = MarkerState(pts.last()), title = "Fin")
                    }
                }
                if (pts.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Sin trazado GPS", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (pts.size >= 2) {
                // Antes: 3 textos en Row con SpaceBetween → "lento → rápido" se rompía
                // vertical en pantallas estrechas. Ahora: leyenda en Column con la frase
                // dividida a izq/der de las barras, todo siempre horizontal.
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Color por velocidad",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "lento",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            SpeedPalette.forEach { c ->
                                Box(
                                    Modifier
                                        .size(width = 22.dp, height = 6.dp)
                                        .background(c, MaterialTheme.shapes.small)
                                )
                            }
                        }
                        Text(
                            "rápido",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewCard(a: ActivitySummary) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
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
            a.kcal?.takeIf { it > 0 }?.let {
                Text(
                    "$it kcal estimadas",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun MetricsGrid(a: ActivitySummary) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricBadge("Velocidad media", GpsMetrics.formatSpeedKmh(a.avgSpeedMps), Modifier.weight(1f))
            MetricBadge("Vel. máxima", GpsMetrics.formatSpeedKmh(a.maxSpeedMps), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricBadge("Ritmo mejor", "${GpsMetrics.formatPace(a.minPaceSecPerKm)}/km", Modifier.weight(1f))
            MetricBadge("Ritmo peor", "${GpsMetrics.formatPace(a.maxPaceSecPerKm)}/km", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricBadge(
                "Altitud máx",
                a.elevationMaxM?.let { "${it.toInt()} m" } ?: "—",
                Modifier.weight(1f)
            )
            MetricBadge(
                "Altitud mín",
                a.elevationMinM?.let { "${it.toInt()} m" } ?: "—",
                Modifier.weight(1f)
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricBadge("Desnivel +", "${a.elevationGainM.toInt()} m", Modifier.weight(1f))
            MetricBadge("Desnivel −", "${a.elevationLossM.toInt()} m", Modifier.weight(1f))
        }
        if (a.avgHrBpm != null || a.maxHrBpm != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricBadge("♥ medio", a.avgHrBpm?.let { "$it bpm" } ?: "—", Modifier.weight(1f))
                MetricBadge("♥ máx", a.maxHrBpm?.let { "$it bpm" } ?: "—", Modifier.weight(1f))
                MetricBadge("♥ mín", a.minHrBpm?.let { "$it bpm" } ?: "—", Modifier.weight(1f))
            }
        }
        a.avgCadenceSpm?.let {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricBadge("Cadencia media", "${it.toInt()} ppm", Modifier.weight(1f))
                a.maxCadenceSpm?.let { mx ->
                    MetricBadge("Cadencia máx", "$mx ppm", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun BestSegmentsCard(a: ActivitySummary) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Mejores parciales",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            a.bestSegments.forEach { seg ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(seg.label, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${GpsMetrics.formatDuration(seg.durationSec)}  ·  ${GpsMetrics.formatPace(seg.paceSecPerKm)}/km",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ElevationChartCard(activity: ActivitySummary) {
    val altitudes = remember(activity.id) { activity.points.mapNotNull { it.altitude } }
    if (altitudes.size < 2) return
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text("Altitud", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                "${altitudes.min().toInt()} – ${altitudes.max().toInt()} m · +${activity.elevationGainM.toInt()} / −${activity.elevationLossM.toInt()} m",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LineAreaChart(
                values = altitudes,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .padding(top = 8.dp),
                lineColor = Color(0xFF2E7D32),
                fillTop = Color(0xFF2E7D32).copy(alpha = 0.35f),
                fillBottom = Color(0xFF2E7D32).copy(alpha = 0.05f),
                yFormatter = { "${it.toInt()}m" }
            )
        }
    }
}

@Composable
private fun HrChartCard(activity: ActivitySummary) {
    val hrs = remember(activity.id) { activity.points.mapNotNull { it.hrBpm?.toDouble() } }
    if (hrs.size < 2) return
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text("Pulso", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                "${activity.minHrBpm ?: hrs.min().toInt()} – ${activity.maxHrBpm ?: hrs.max().toInt()} bpm · medio ${activity.avgHrBpm ?: hrs.average().toInt()} bpm",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LineAreaChart(
                values = hrs,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .padding(top = 8.dp),
                lineColor = Color(0xFFD32F2F),
                fillTop = Color(0xFFD32F2F).copy(alpha = 0.35f),
                fillBottom = Color(0xFFD32F2F).copy(alpha = 0.0f),
                yFormatter = { "${it.toInt()}" }
            )
            Spacer(Modifier.height(12.dp))
            HeartRateZoneBreakdown(
                samples = hrs.map { it.toInt() },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PaceChartCard(activity: ActivitySummary) {
    val paceValues = remember(activity.id, activity.splits, activity.points) {
        val fromSplits = activity.splits.filter { it.paceSecPerKm > 0 }.map { it.paceSecPerKm }
        val flat = fromSplits.size >= 2 && fromSplits.distinct().size <= 1
        when {
            fromSplits.size >= 2 && !flat -> fromSplits
            activity.points.size >= 4 -> {
                GpsMetrics.computeSplits(activity.points)
                    .map { it.paceSecPerKm }
                    .filter { it > 0 }
            }
            else -> fromSplits
        }
    }
    if (paceValues.size >= 2) {
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("Ritmo por km", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                LineAreaChart(
                    values = paceValues,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .padding(top = 8.dp),
                    lineColor = Color(0xFF6A1B9A),
                    fillTop = Color(0xFF6A1B9A).copy(alpha = 0.3f),
                    fillBottom = Color(0xFF6A1B9A).copy(alpha = 0.0f),
                    invertY = true,
                    yFormatter = { GpsMetrics.formatPace(it) },
                    xLabels = paceValues.indices.map { idx -> "km${idx + 1}" }
                )
            }
        }
    } else {
        val speeds = activity.points.mapNotNull { it.speedMps?.toDouble() }
        if (speeds.size >= 4) {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("Velocidad", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    LineAreaChart(
                        values = speeds.map { it * 3.6 },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .padding(top = 8.dp),
                        lineColor = Color(0xFF1565C0),
                        yFormatter = { "%.1f".format(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SplitsCard(activity: ActivitySummary) {
    val hasKm = activity.splits.isNotEmpty()
    val hasMile = activity.splitsMile.isNotEmpty()
    val hasTime = activity.splitsTime.isNotEmpty()
    if (!hasKm && !hasMile && !hasTime) return
    var mode by remember {
        mutableStateOf(
            when {
                hasKm -> SplitMode.KM
                hasMile -> SplitMode.MILE
                else -> SplitMode.TIME
            }
        )
    }
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Parciales", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (hasKm) FilterChip(
                    selected = mode == SplitMode.KM,
                    onClick = { mode = SplitMode.KM },
                    label = { Text(SplitMode.KM.label) }
                )
                if (hasMile) FilterChip(
                    selected = mode == SplitMode.MILE,
                    onClick = { mode = SplitMode.MILE },
                    label = { Text(SplitMode.MILE.label) }
                )
                if (hasTime) FilterChip(
                    selected = mode == SplitMode.TIME,
                    onClick = { mode = SplitMode.TIME },
                    label = { Text(SplitMode.TIME.label) }
                )
            }
            HorizontalDivider()
            when (mode) {
                SplitMode.KM -> activity.splits.forEach { SplitKmRow(it) }
                SplitMode.MILE -> activity.splitsMile.forEach { SplitMileRow(it) }
                SplitMode.TIME -> activity.splitsTime.forEach { SplitTimeRow(it) }
            }
        }
    }
}

@Composable
private fun SplitKmRow(split: SplitKm) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("KM ${split.km}", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(GpsMetrics.formatDuration(split.durationSec), modifier = Modifier.weight(1f))
            Text(
                "${GpsMetrics.formatPace(split.paceSecPerKm)}/km",
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1.2f)
            )
            split.avgHrBpm?.let {
                Text("♥$it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun SplitMileRow(split: SplitMile) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Milla ${split.mile}", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(GpsMetrics.formatDuration(split.durationSec), modifier = Modifier.weight(1f))
            val paceSecPerKm = split.durationSec / 1.609344
            Text("${GpsMetrics.formatPace(paceSecPerKm)}/km", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.2f))
        }
    }
}

@Composable
private fun SplitTimeRow(split: SplitTime) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Bloque ${split.index}", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(GpsMetrics.formatDistance(split.distanceM), modifier = Modifier.weight(1f))
            Text(
                "${GpsMetrics.formatPace(split.avgPaceSecPerKm)}/km",
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1.2f)
            )
        }
    }
}
