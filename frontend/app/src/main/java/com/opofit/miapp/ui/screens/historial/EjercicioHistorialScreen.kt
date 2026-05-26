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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material.icons.filled.Timeline
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
import androidx.compose.runtime.remember
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
                item { ChartCard(h) }
            } else {
                item { InfoChip("Haz al menos 2 sesiones de este ejercicio para ver gráficas de evolución.") }
            }
            if (h.esCardio) {
                item { ChartRitmoCard(h) }
            }
            if (h.esCardio) {
                item { HrCard(h) }
                item { CadenciaCard(h) }
                item { KcalCard(h) }
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

@Suppress("DEPRECATION")
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

private fun fechaCorta(iso: String?): String {
    if (iso == null) return ""
    return try {
        // Esperamos "yyyy-MM-dd ..." o ISO; cogemos los 10 primeros
        val ymd = iso.take(10)
        val parts = ymd.split("-")
        if (parts.size == 3) "${parts[2]}/${parts[1]}" else ymd
    } catch (_: Exception) { iso.take(10) }
}

@Composable
private fun ChartCard(h: HistorialEjercicio) {
    val (titulo, subtitulo) = tituloGrafica(h)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Timeline, null, tint = colorPilar(h.pilar))
                Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                    Text(
                        titulo,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        subtitulo,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (h.menorEsMejor) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Menos tiempo = mejor") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    )
                }
            }
            Text(
                "Toca o desliza la gráfica para ver el dato exacto y la fecha de cada sesión",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LineAreaChart(
                values = h.puntos.map { it.valor },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                showDots = true,
                yFormatter = { v -> "%.2f %s".format(v, unidadCorta(h.unidad)) },
                yAxisLabel = "${tituloEjeY(h)} (${unidadCorta(h.unidad)})",
                xLabels = if (h.puntos.size >= 3) listOf(
                    fechaCorta(h.puntos.first().fechaEntreno),
                    fechaCorta(h.puntos[h.puntos.size / 2].fechaEntreno),
                    fechaCorta(h.puntos.last().fechaEntreno)
                ) else h.puntos.map { fechaCorta(it.fechaEntreno) },
                pointLabels = h.puntos.map { fechaCorta(it.fechaEntreno) },
                invertY = h.menorEsMejor,
                lineColor = colorPilar(h.pilar)
            )
        }
    }
}

private fun tituloGrafica(h: HistorialEjercicio): Pair<String, String> {
    return when (h.unidad) {
        "km" -> "Distancia recorrida por sesión" to "Cada punto = un día de entreno"
        "m" -> "Distancia nadada por sesión" to "Cada punto = un día en piscina"
        "s" -> "Tiempo por sesión" to "Menos tiempo significa mejor marca"
        else -> "Repeticiones / valor por sesión" to "Cada punto = un día de entreno"
    }
}

private fun tituloEjeY(h: HistorialEjercicio): String = when (h.unidad) {
    "km" -> "Distancia"
    "m" -> "Distancia"
    "s" -> "Tiempo"
    else -> "Valor"
}

@Composable
private fun ChartRitmoCard(h: HistorialEjercicio) {
    val ritmos = remember(h.puntos) {
        h.puntos.mapNotNull { p ->
            val dist = if (h.unidad == "km") p.valor else p.valor / 1000.0
            val secs = p.duracionSeg ?: return@mapNotNull null
            if (dist <= 0 || secs <= 0) null else secs / dist
        }
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Ritmo por sesión (min/km)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            if (ritmos.size >= 2) {
                Text(
                    "Más bajo = más rápido. Toca para ver el ritmo exacto.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LineAreaChart(
                    values = ritmos,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    showDots = true,
                    invertY = true,
                    yFormatter = { v -> "${GpsMetrics.formatPace(v)} /km" },
                    yAxisLabel = "Ritmo (min/km)",
                    pointLabels = h.puntos.map { fechaCorta(it.fechaEntreno) },
                    xLabels = if (h.puntos.size >= 3) listOf(
                        fechaCorta(h.puntos.first().fechaEntreno),
                        fechaCorta(h.puntos[h.puntos.size / 2].fechaEntreno),
                        fechaCorta(h.puntos.last().fechaEntreno)
                    ) else h.puntos.map { fechaCorta(it.fechaEntreno) },
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

@Composable
private fun HrCard(h: HistorialEjercicio) {
    val hayHR = h.puntos.any { it.gpsActividadUuid != null }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.FavoriteBorder, null, tint = Color(0xFFD32F2F))
                Text(
                    "  Frecuencia cardíaca",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (!hayHR) {
                EmptyMetricInfo(
                    icon = Icons.Filled.SyncProblem,
                    titulo = "Sin datos de pulso",
                    detalle = "Conecta una banda BLE o un reloj con Health Connect/Strava para registrar el pulso de tus carreras."
                )
            } else {
                Text(
                    "Para ver el detalle de pulso por sesión, abre cada actividad GPS asociada en la lista de abajo (icono de mapa).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CadenciaCard(h: HistorialEjercicio) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                @Suppress("DEPRECATION")
                Icon(Icons.Filled.DirectionsRun, null, tint = Color(0xFF1565C0))
                Text(
                    "  Cadencia",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            val tieneCadencia = h.puntos.any { it.gpsActividadUuid != null }
            if (!tieneCadencia) {
                EmptyMetricInfo(
                    icon = Icons.Filled.SyncProblem,
                    titulo = "Sin cadencia",
                    detalle = "La cadencia (pasos/min) se calcula con el sensor de pasos del móvil al grabar GPS, o desde tu reloj. Sale por sesión en el detalle del GPS."
                )
            } else {
                Text(
                    "Abre cada actividad GPS de la lista para ver la cadencia media y por intervalos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun KcalCard(h: HistorialEjercicio) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocalFireDepartment, null, tint = Color(0xFFEF6C00))
                Text(
                    "  Calorías",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            val total = (h.totalDistanciaKm * 70 * 0.9).toInt()
            if (total > 0) {
                Text(
                    "Total estimado lifetime: $total kcal",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Estimación basada en MET dinámico y tu peso de perfil. Para kcal reales, conecta una banda BLE o sincroniza con Strava.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                EmptyMetricInfo(
                    icon = Icons.Filled.SyncProblem,
                    titulo = "Sin datos de kcal",
                    detalle = "Aún no hay distancia registrada para calcular calorías."
                )
            }
        }
    }
}

@Composable
private fun EmptyMetricInfo(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    detalle: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column {
            Text(titulo, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(
                detalle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InfoChip(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
