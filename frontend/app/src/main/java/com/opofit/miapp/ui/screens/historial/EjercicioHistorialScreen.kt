package com.opofit.miapp.ui.screens.historial

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opofit.miapp.data.responsemodels.HistorialEjercicio
import com.opofit.miapp.data.responsemodels.PuntoEjercicio
import com.opofit.miapp.gps.util.GpsMetrics
import com.opofit.miapp.ui.components.LineAreaChart
import com.opofit.miapp.ui.components.MetricBadge
import com.opofit.miapp.ui.viewmodels.HistorialAvanzadoViewModel
import com.opofit.miapp.utils.DateFormatUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EjercicioHistorialScreen(
    idEjercicio: Int,
    onNavigateBack: () -> Unit,
    onOpenGpsActividad: (String) -> Unit = {},
    viewModel: HistorialAvanzadoViewModel = viewModel()
) {
    LaunchedEffect(idEjercicio) { viewModel.cargarHistorialEjercicio(idEjercicio) }
    val hist by viewModel.historialEjercicio.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Evolución", fontWeight = FontWeight.Bold)
                        hist?.let {
                            Text(it.ejercicio, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        if (hist == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("Cargando…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }
        val h = hist!!
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { HeaderHero(h) }
            item { KpisGrid(h) }
            if (h.puntos.size >= 2) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Text(
                                if (h.menorEsMejor) "Mejor cuanto más bajo" else "Evolución del valor",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            LineAreaChart(
                                values = h.puntos.map { it.valor },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .padding(top = 8.dp),
                                showDots = true,
                                yFormatter = { "%.1f".format(it) },
                                invertY = h.menorEsMejor,
                                lineColor = colorPilar(h.pilar)
                            )
                        }
                    }
                }
            }
            if (h.esCardio && h.puntos.any { it.duracionSeg != null && it.duracionSeg > 0 } && h.puntos.size >= 2) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Text(
                                "Ritmo por sesión",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            val ritmos = h.puntos.mapNotNull { p ->
                                val dist = if (h.unidad == "km") p.valor else p.valor / 1000.0
                                val secs = p.duracionSeg ?: return@mapNotNull null
                                if (dist <= 0 || secs <= 0) null else secs / dist
                            }
                            if (ritmos.size >= 2) {
                                LineAreaChart(
                                    values = ritmos,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .padding(top = 8.dp),
                                    showDots = true,
                                    invertY = true,
                                    yFormatter = { GpsMetrics.formatPace(it) },
                                    lineColor = Color(0xFF6A1B9A)
                                )
                            } else {
                                Text(
                                    "Necesitas al menos 2 sesiones con duración para ver el ritmo.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            item {
                Text(
                    "Detalle por sesión",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            items(h.puntos.reversed()) { punto ->
                SesionRow(punto, h, onOpenGpsActividad)
            }
        }
    }
}

@Composable
private fun HeaderHero(h: HistorialEjercicio) {
    val color = colorPilar(h.pilar)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(color.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(iconPilar(h.pilar, h.ejercicio), null, tint = color, modifier = Modifier.size(28.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(h.ejercicio, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2)
                Text(
                    listOfNotNull(h.pilar?.lowercase()?.replaceFirstChar { it.uppercase() }, h.categoria).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun KpisGrid(h: HistorialEjercicio) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricBadge(
                if (h.menorEsMejor) "Mejor (menor)" else "Mejor",
                "%.2f %s".format(h.mejor, unidadCorta(h.unidad)),
                Modifier.weight(1f)
            )
            MetricBadge("Media", "%.2f".format(h.media), Modifier.weight(1f))
            MetricBadge("Sesiones", "${h.sesiones}", Modifier.weight(1f))
        }
        if (h.esCardio) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricBadge(
                    "Total recorrido",
                    if (h.totalDistanciaKm > 0) "%.1f km".format(h.totalDistanciaKm) else "—",
                    Modifier.weight(1f)
                )
                MetricBadge(
                    "Mejor ritmo",
                    h.mejorRitmoSpKm?.let { "${GpsMetrics.formatPace(it)} /km" } ?: "—",
                    Modifier.weight(1f)
                )
            }
            if (h.mejorVelMps != null || h.totalDesnivelM > 0) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricBadge(
                        "Vel. máx",
                        h.mejorVelMps?.let { "%.1f km/h".format(it * 3.6) } ?: "—",
                        Modifier.weight(1f)
                    )
                    MetricBadge(
                        "Desnivel +",
                        if (h.totalDesnivelM > 0) "${h.totalDesnivelM} m" else "—",
                        Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SesionRow(
    punto: PuntoEjercicio,
    h: HistorialEjercicio,
    onOpenGpsActividad: (String) -> Unit
) {
    val esMejor = punto.valor == h.mejor
    val sortedPrev = h.puntos.takeWhile { it.fechaEntreno != punto.fechaEntreno }
    val previo = sortedPrev.lastOrNull()
    val delta = previo?.let { punto.valor - it.valor }
    val mejora = delta != null && ((h.menorEsMejor && delta < 0) || (!h.menorEsMejor && delta > 0))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (esMejor) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    punto.fechaEntreno?.let { DateFormatUtil.formatearFechaHora(it) } ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "%.2f %s".format(punto.valor, unidadCorta(h.unidad)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (esMejor) {
                        Icon(
                            Icons.Filled.EmojiEvents,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                punto.duracionSeg?.takeIf { it > 0 }?.let { secs ->
                    if (h.esCardio) {
                        val dist = if (h.unidad == "km") punto.valor else punto.valor / 1000.0
                        val pace = if (dist > 0) secs / dist else 0.0
                        Text(
                            "${GpsMetrics.formatDuration(secs)} · ${GpsMetrics.formatPace(pace)}/km",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            "Duración: ${GpsMetrics.formatDuration(secs)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                delta?.let { d ->
                    val tint = if (mejora) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (d >= 0) Icons.AutoMirrored.Filled.TrendingUp
                            else Icons.AutoMirrored.Filled.TrendingDown,
                            null,
                            modifier = Modifier.size(14.dp),
                            tint = tint
                        )
                        Text(
                            "${if (d >= 0) "+" else ""}%.2f".format(d),
                            style = MaterialTheme.typography.labelSmall,
                            color = tint
                        )
                    }
                }
                punto.gpsActividadUuid?.let { uuid ->
                    IconButton(onClick = { onOpenGpsActividad(uuid) }) {
                        Icon(Icons.Filled.Map, "Abrir GPS", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

private fun colorPilar(pilar: String?): Color = when (pilar?.uppercase()) {
    "FUERZA" -> Color(0xFF1565C0)
    "RESISTENCIA" -> Color(0xFF2E7D32)
    "VELOCIDAD" -> Color(0xFFEF6C00)
    "CORE" -> Color(0xFF6A1B9A)
    else -> Color(0xFF455A64)
}

private fun iconPilar(pilar: String?, nombre: String) = when {
    pilar?.uppercase() == "RESISTENCIA" || nombre.contains("carrera", true) || nombre.contains("trote", true) || nombre.contains("rodaje", true) -> Icons.Filled.DirectionsRun
    nombre.contains("nat", true) -> Icons.Filled.Pool
    pilar?.uppercase() == "VELOCIDAD" -> Icons.Filled.Speed
    else -> Icons.Filled.FitnessCenter
}

private fun unidadCorta(u: String): String = when (u) {
    "km" -> "km"
    "m" -> "m"
    else -> "reps"
}
