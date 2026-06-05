package com.opofit.miapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opofit.miapp.gps.model.GpsTrackingState
import com.opofit.miapp.gps.service.GpsTracker
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
    val gps by GpsTracker.state.collectAsState()
    val g = gpsState ?: gps
    val gpsActivo = g.active && !g.paused
    val distM = if (gpsActivo) g.distanceM else (distanciaKm?.times(1000.0) ?: 0.0)
    val durSec = if (gpsActivo) g.durationSec else TimeFormatUtil.secondsFromMs(elapsedMs).toInt()
    val pace = if (gpsActivo && g.avgPaceSecPerKm > 0) g.avgPaceSecPerKm
    else GpsMetrics.paceSecPerKm(distM, durSec.coerceAtLeast(1))
    val velKmh = if (gpsActivo) g.currentSpeedMps * 3.6
    else if (distM > 0 && durSec > 0) (distM / durSec) * 3.6 else 0.0

    if (!cronometroActivo && !gpsActivo) return

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                if (gpsActivo) "📍 GPS en vivo" else "⏱ Sesión en curso",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricChip("Tiempo", TimeFormatUtil.formatElapsedMs(
                    if (gpsActivo) durSec * 1000L else elapsedMs
                ))
                MetricChip("Distancia", if (distM > 0) GpsMetrics.formatDistance(distM) else "—")
                MetricChip("Ritmo", pace?.let { "${GpsMetrics.formatPace(it)}/km" } ?: "—")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricChip("Velocidad", if (velKmh > 0.1) "%.1f km/h".format(velKmh) else "—")
                MetricChip("Desnivel +", if (gpsActivo) "${g.elevationGainM.toInt()} m" else "—")
                MetricChip(
                    "♥ Pulso",
                    g.currentHrBpm?.let { "$it bpm" }
                        ?: g.avgHrBpm?.let { "ø $it" }
                        ?: if (g.hrDeviceConnected) "…" else "—"
                )
            }
            if (tipoCardio != null && g.avgCadenceSpm != null) {
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
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.75f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
