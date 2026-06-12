package com.opofit.miapp.ui.components

import com.opofit.miapp.ui.components.ElevatedCard
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opofit.miapp.gps.model.GpsTrackingState
import com.opofit.miapp.gps.service.GpsTracker
import com.opofit.miapp.gps.service.HrBleManager
import com.opofit.miapp.gps.util.GpsMetrics
import com.opofit.miapp.utils.TimeFormatUtil

@Composable
fun EntrenoLiveMetricsBar(
    elapsedMs: Long,
    cronometroActivo: Boolean,
    distanciaKm: Double? = null,
    tipoCardio: String? = null,
    modifier: Modifier = Modifier,
    gpsState: GpsTrackingState? = null
) {
    val context = LocalContext.current
    val gps by GpsTracker.state.collectAsState()
    val liveHr by HrBleManager.get(context).heartRate.collectAsState()
    val g = gpsState ?: gps
    val gpsActivo = g.active && !g.paused
    val esCardio = tipoCardio != null || gpsActivo
    val distM = if (gpsActivo) g.distanceM else (distanciaKm?.times(1000.0) ?: 0.0)
    val durSec = if (gpsActivo) g.durationSec else TimeFormatUtil.secondsFromMs(elapsedMs).toInt()
    val pace = if (gpsActivo && g.avgPaceSecPerKm > 0) g.avgPaceSecPerKm
    else GpsMetrics.paceSecPerKm(distM, durSec.coerceAtLeast(1))
    val velKmh = if (gpsActivo) g.currentSpeedMps * 3.6
    else if (distM > 0 && durSec > 0) (distM / durSec) * 3.6 else 0.0
    val displayHr = g.currentHrBpm ?: liveHr

    if (!cronometroActivo && !gpsActivo) return

    ElevatedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (gpsActivo) Icons.Outlined.LocationOn else Icons.Outlined.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    if (gpsActivo) "GPS en vivo" else "Sesión en curso",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricChip("Tiempo", TimeFormatUtil.formatElapsedMs(
                    if (gpsActivo) durSec * 1000L else elapsedMs
                ))
                MetricChip(
                    "Pulso",
                    displayHr?.let { "$it bpm" }
                        ?: g.avgHrBpm?.let { "ø $it" }
                        ?: if (g.hrDeviceConnected) "…" else "—"
                )
                if (esCardio) {
                    MetricChip("Distancia", if (distM > 0) GpsMetrics.formatDistance(distM) else "—")
                }
            }
            if (esCardio) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MetricChip("Ritmo", pace?.let { "${GpsMetrics.formatPace(it)}/km" } ?: "—")
                    MetricChip("Velocidad", if (velKmh > 0.1) "%.1f km/h".format(velKmh) else "—")
                    MetricChip("Desnivel +", if (gpsActivo) "${g.elevationGainM.toInt()} m" else "—")
                }
            }
            if (esCardio && g.avgCadenceSpm != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MetricChip("Cadencia", "${g.avgCadenceSpm} ppm")
                    g.currentStrideM?.let { MetricChip("Zancada", GpsMetrics.formatStride(it)) }
                    g.avgInclinePct?.let { MetricChip("Pendiente", GpsMetrics.formatIncline(it)) }
                }
            }
        }
    }
}

@Composable
private fun MetricChip(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
