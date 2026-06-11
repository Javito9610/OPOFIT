package com.opofit.miapp.gps.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import kotlinx.coroutines.launch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.opofit.miapp.gps.model.GpsTrackingState
import com.opofit.miapp.gps.service.GpsRecordingContext
import com.opofit.miapp.gps.service.RoutePreferences
import com.opofit.miapp.gps.service.ShareActivityContext
import com.opofit.miapp.gps.service.buildPendingShareFromGps
import com.opofit.miapp.gps.util.GpsMetrics
import com.opofit.miapp.ui.components.HeartRateZoneLive
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

private val SpeedPalette = listOf(
    Color(0xFFD32F2F),
    Color(0xFFEF6C00),
    Color(0xFFFBC02D),
    Color(0xFF388E3C),
    Color(0xFF0288D1)
)

@Composable
fun GpsRecordingScreen(
    onFinishSaved: (activityId: String) -> Unit,
    onDiscarded: () -> Unit,
    viewModel: GpsViewModel = viewModel()
) {
    val state by viewModel.tracking.collectAsState()
    val liveHr by viewModel.hrManager().heartRate.collectAsState()
    val displayHr = state.currentHrBpm ?: liveHr
    val context = LocalContext.current
    val cameraPositionState = rememberCameraPositionState()
    var plannedRoute by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var plannedNombre by remember { mutableStateOf<String?>(null) }
    var startedSession by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val conRuta = GpsRecordingContext.consumeConRuta()
        if (conRuta) {
            val route = RoutePreferences.load(context)
            if (route != null && route.puntos.size >= 2) {
                plannedRoute = route.puntos.map { LatLng(it.lat, it.lng) }
                plannedNombre = route.nombre
            }
        } else {
            RoutePreferences.clear(context)
            plannedRoute = emptyList()
            plannedNombre = null
        }
    }
    var showFinishDialog by remember { mutableStateOf(false) }
    var noPointsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.active) {
        if (!startedSession && !state.active) {
            viewModel.startTracking()
        }
        startedSession = true
    }

    // Zoom inicial: la primera vez que aparezca un punto centramos a nivel 17.
    // Defensivo: `cameraPositionState.animate()` requiere el GoogleMap montado;
    // si crashea (puede pasar la 1ª vez antes del compose), caemos al
    // asignment síncrono que NUNCA falla. Antes el crash sacaba al usuario
    // de la app al pulsar "Empezar carrera".
    var primerPunto by remember { mutableStateOf(true) }
    LaunchedEffect(state.points.lastOrNull()?.timestampMs) {
        val last = state.points.lastOrNull() ?: return@LaunchedEffect
        val zoomActual = runCatching { cameraPositionState.position.zoom }.getOrDefault(0f)
        val zoomDeseado = when {
            primerPunto -> 17f
            zoomActual < 10f -> 16f
            else -> zoomActual
        }
        val targetLatLng = LatLng(last.lat, last.lng)
        // Intento 1: animate (requiere mapa montado).
        val animOk = runCatching {
            cameraPositionState.animate(
                update = com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(
                    targetLatLng,
                    zoomDeseado
                ),
                durationMs = if (primerPunto) 700 else 250
            )
        }.isSuccess
        // Intento 2 (fallback): asignación directa.
        if (!animOk) {
            runCatching {
                cameraPositionState.position =
                    com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(
                        targetLatLng, zoomDeseado
                    )
            }
        }
        primerPunto = false
    }

    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            confirmButton = {
                Button(onClick = {
                    showFinishDialog = false
                    val id = viewModel.stopAndSave()
                    if (id != null) {
                        viewModel.get(id)?.let { act ->
                            ShareActivityContext.set(buildPendingShareFromGps(act))
                        }
                        onFinishSaved(id)
                    } else noPointsDialog = true
                }) { Text("Guardar") }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showFinishDialog = false
                    viewModel.discard()
                    onDiscarded()
                }) { Text("Descartar") }
            },
            title = { Text("¿Finalizar actividad?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Distancia: ${GpsMetrics.formatDistance(state.distanceM)}")
                    Text("Duración: ${GpsMetrics.formatDuration(state.durationSec)}")
                    Text("Ritmo medio: ${GpsMetrics.formatPace(state.avgPaceSecPerKm)}/km")
                    if (state.kcal > 0) Text("Calorías estimadas: ${state.kcal} kcal")
                    state.avgHrBpm?.let { Text("Pulso medio: $it bpm") }
                }
            }
        )
    }
    if (noPointsDialog) {
        AlertDialog(
            onDismissRequest = { noPointsDialog = false; onDiscarded() },
            confirmButton = {
                Button(onClick = { noPointsDialog = false; onDiscarded() }) { Text("Cerrar") }
            },
            title = { Text("Sin datos suficientes") },
            text = { Text("No se ha registrado distancia suficiente. Asegúrate de tener el GPS activo y prueba de nuevo.") }
        )
    }

    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    Box(Modifier.fillMaxSize()) {
        MapWithRoute(state, cameraPositionState, plannedRoute)

        // FAB "Centrar en mi posición". Si el usuario hace zoom out y el mapa
        // se queda lejos, este botón lo trae de vuelta a la última posición
        // GPS con zoom de calle (17). Antes solo se centraba automáticamente
        // tras cada punto y si lo arrastrabas no había forma de volver.
        androidx.compose.material3.SmallFloatingActionButton(
            onClick = {
                val last = state.points.lastOrNull() ?: return@SmallFloatingActionButton
                val target = LatLng(last.lat, last.lng)
                coroutineScope.launch {
                    // Mismo patrón defensivo: animate falla a veces si el
                    // mapa aún no está completamente listo. Catch + fallback.
                    val ok = runCatching {
                        cameraPositionState.animate(
                            update = com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(
                                target, 17f
                            ),
                            durationMs = 400
                        )
                    }.isSuccess
                    if (!ok) {
                        runCatching {
                            cameraPositionState.position =
                                com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(target, 17f)
                        }
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                // statusBarsPadding empuja el FAB POR DEBAJO del notch/cámara.
                // Antes con padding fijo de 16dp se cortaba en móviles modernos
                // con notch (Pixel, S22+, etc.). Ahora respeta el inset real.
                .statusBarsPadding()
                .padding(top = 8.dp, end = 14.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            androidx.compose.material3.Icon(
                androidx.compose.material.icons.Icons.Filled.MyLocation,
                contentDescription = "Centrar en mi posición"
            )
        }

        Surface(
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Column(
                Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricBlock("Distancia", GpsMetrics.formatDistance(state.distanceM))
                    MetricBlock("Tiempo", GpsMetrics.formatDuration(state.durationSec))
                    MetricBlock("Ritmo", "${GpsMetrics.formatPace(state.avgPaceSecPerKm)}/km")
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricBlock(
                        "Ritmo ahora",
                        "${GpsMetrics.formatPace(state.instantPaceSecPerKm)}/km"
                    )
                    MetricBlock("Velocidad", GpsMetrics.formatSpeedKmh(state.currentSpeedMps.toDouble()))
                    MetricBlock(
                        "Altitud",
                        state.currentAltitudeM?.let { "${it.toInt()} m" } ?: "—"
                    )
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricBlock("Desnivel +", "${state.elevationGainM.toInt()} m")
                    MetricBlock("Desnivel −", "${state.elevationLossM.toInt()} m")
                    MetricBlock("kcal", state.kcal.toString())
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricBlock(
                        "Cadencia",
                        state.currentCadenceSpm?.let { "$it ppm" } ?: "—"
                    )
                    MetricBlock(
                        "♥ Pulso",
                        displayHr?.let { "$it bpm" }
                            ?: if (state.hrDeviceConnected) "esperando…" else "—"
                    )
                    MetricBlock(
                        "Vel. máx",
                        GpsMetrics.formatSpeedKmh(state.maxSpeedMps.toDouble())
                    )
                }
                if (displayHr != null) {
                    HeartRateZoneLive(
                        bpm = displayHr,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricBlock("Zancada", GpsMetrics.formatStride(state.currentStrideM ?: state.avgStrideM))
                    MetricBlock("Pendiente", GpsMetrics.formatIncline(state.currentInclinePct ?: state.avgInclinePct))
                    MetricBlock(
                        "♥ media",
                        state.avgHrBpm?.let { "$it bpm" } ?: "—"
                    )
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (state.paused) {
                        Button(
                            onClick = { viewModel.resume() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Filled.PlayArrow, null)
                            Spacer(Modifier.size(6.dp))
                            Text("Reanudar")
                        }
                    } else {
                        OutlinedButton(
                            onClick = { viewModel.pause() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Pause, null)
                            Spacer(Modifier.size(6.dp))
                            Text("Pausar")
                        }
                    }
                    Button(
                        onClick = { showFinishDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Filled.Stop, null)
                        Spacer(Modifier.size(6.dp))
                        Text("Finalizar")
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.hrDeviceConnected) {
                        Icon(
                            Icons.Filled.Favorite,
                            null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.size(4.dp))
                    }
                    Text(
                        state.lastErrorMsg
                            ?: buildString {
                                append("${state.type.emoji} ${state.type.display}")
                                if (plannedNombre != null) append(" · Ruta: $plannedNombre")
                                else append(" · Libre")
                                if (state.hrDeviceConnected) append(" · ${state.hrDeviceName ?: "Banda BLE"}")
                                if (state.paused) append(" · En pausa")
                            },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricBlock(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun MapWithRoute(
    state: GpsTrackingState,
    cameraPositionState: CameraPositionState,
    plannedRoute: List<LatLng> = emptyList()
) {
    val context = LocalContext.current
    val hasLocation = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val pts = state.points.map { LatLng(it.lat, it.lng) }
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = hasLocation,
                mapType = MapType.NORMAL
            ),
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = hasLocation,
                zoomControlsEnabled = false,
                tiltGesturesEnabled = false,
                compassEnabled = true
            )
        ) {
            if (plannedRoute.size >= 2) {
                Polyline(
                    points = plannedRoute,
                    color = Color(0xFF66BB6A),
                    width = 10f
                )
            }
            if (pts.size >= 2) {
                Polyline(
                    points = pts,
                    color = Color(0xFF1565C0),
                    width = 14f
                )
            }
            pts.firstOrNull()?.let {
                Marker(state = MarkerState(it), title = "Inicio")
            }
            pts.lastOrNull()?.let {
                Marker(state = MarkerState(it), title = if (pts.size > 1) "Ahora" else "Tú")
            }
        }
        if (pts.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Buscando señal GPS…",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

internal val SpeedPaletteRef = SpeedPalette
