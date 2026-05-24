package com.opofit.miapp.ui.screens.historial

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opofit.miapp.data.local.TokenManager
import com.opofit.miapp.ui.viewmodels.AuthViewModel
import com.opofit.miapp.ui.viewmodels.HistorialViewModel
import com.opofit.miapp.ui.viewmodels.RutinasLibresViewModel
import com.opofit.miapp.ui.viewmodels.RutinasViewModel
import com.opofit.miapp.utils.DateFormatUtil
import com.opofit.miapp.utils.Units
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    historialViewModel: HistorialViewModel = viewModel(),
    rutinasViewModel: RutinasViewModel = viewModel(),
    rutinasLibresViewModel: RutinasLibresViewModel = viewModel()
) {
    val authState by authViewModel.uiState.collectAsState()
    val uiState by historialViewModel.uiState.collectAsState()
    val rutinasState by rutinasViewModel.uiState.collectAsState()
    val rutinasLibresState by rutinasLibresViewModel.uiState.collectAsState()

    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    var unitDist by remember { mutableStateOf("km") }
    LaunchedEffect(Unit) {
        tokenManager.getUnitDistancia().collectLatest { u ->
            if (!u.isNullOrBlank()) unitDist = u
        }
    }

    val userId = authState.userId ?: 0
    val oposicionId = authState.oposicionId ?: 1

    var ejercicioSeleccionado by remember { mutableStateOf("") }
    var tipoSeleccionado by remember { mutableStateOf("FUERZA") }

    fun normalizarTipoBloque(bloque: String): String {
        val b = bloque.lowercase()
        return when {
            b.contains("fuerza") -> "FUERZA"
            b.contains("resist") -> "RESISTENCIA"
            b.contains("veloc") || b.contains("sprint") -> "VELOCIDAD"
            else -> "OTRO"
        }
    }

    
    val ejerciciosPorTipo = remember(rutinasState.rutinaCompleta, rutinasLibresState.rutinas) {
        val map = linkedMapOf<String, MutableList<Pair<Int, String>>>(
            "FUERZA" to mutableListOf(),
            "RESISTENCIA" to mutableListOf(),
            "VELOCIDAD" to mutableListOf(),
            "PERSONAL" to mutableListOf(),
            "OTRO" to mutableListOf()
        )
        rutinasState.rutinaCompleta.forEach { bloque ->
            val tipo = normalizarTipoBloque(bloque.bloque)
            bloque.ejercicios.forEach { ej ->
                val id = ej.id_ejercicio ?: return@forEach
                map.getOrPut(tipo) { mutableListOf() }.add(id to ej.nombre)
            }
        }
        
        rutinasLibresState.rutinas.forEach { r ->
            r.ejercicios.forEach { ej ->
                map.getOrPut("PERSONAL") { mutableListOf() }.add(ej.id_ejercicio to (ej.nombre_ejercicio ?: "Ejercicio"))
            }
        }
        
        map.mapValues { (_, list) -> list.distinctBy { it.first }.toMutableList() }
    }

    val ejerciciosFiltrados = remember(ejerciciosPorTipo, tipoSeleccionado) {
        (ejerciciosPorTipo[tipoSeleccionado] ?: emptyList()).distinctBy { it.first }
    }

    val stats = remember(uiState.evolucion) {
        val values = uiState.evolucion.map { it.valor_conseguido }
        val last = values.lastOrNull()
        val best = values.maxOrNull()
        val avg = if (values.isNotEmpty()) values.sum() / values.size else null
        val delta = if (values.size >= 2) values.last() - values.first() else null
        Triple(last, best, avg) to delta
    }

    fun esCarrera(nombre: String): Boolean {
        val n = nombre.lowercase()
        return n.contains("carrera") || n.contains("trote") || n.contains("rodaje") || n.contains("fartlek")
    }
    fun esNatacion(nombre: String): Boolean {
        val n = nombre.lowercase()
        return n.contains("natación") || n.contains("natacion") || n.contains("nadar")
    }

    fun duracionEnSegundos(p: com.opofit.miapp.data.responsemodels.PuntoEvolucion): Int? {
        val d = p.duracion_oficial ?: return null
        
        return if (d in 1..300) d * 60 else d
    }

    fun ritmoVelocidadHistorial(nombreEj: String, p: com.opofit.miapp.data.responsemodels.PuntoEvolucion): Pair<String, String> {
        val secs = duracionEnSegundos(p) ?: return "-" to "-"
        val dist = p.valor_conseguido
        if (secs <= 0 || dist <= 0.0) return "-" to "-"
        return when {
            esCarrera(nombreEj) -> {
                val distKm = dist
                val paceSecPerKm = secs / distKm
                val hours = secs / 3600.0
                val speedKmh = distKm / hours
                if (unitDist == "mi") {
                    val paceSecPerMi = paceSecPerKm * 1.609344
                    Units.formatPace(paceSecPerMi) + " min/mi" to "%.2f mph".format(Units.kmhToMph(speedKmh))
                } else {
                    Units.formatPace(paceSecPerKm) + " min/km" to "%.2f km/h".format(speedKmh)
                }
            }
            esNatacion(nombreEj) -> {
                val distM = dist
                if (unitDist == "mi") {
                    val distYd = Units.mToYd(distM)
                    val per100 = secs / (distYd / 100.0)
                    val speed = distYd / secs
                    Units.formatPace(per100) + " min/100yd" to "%.2f yd/s".format(speed)
                } else {
                    val per100 = secs / (distM / 100.0)
                    val speed = distM / secs
                    Units.formatPace(per100) + " min/100m" to "%.2f m/s".format(speed)
                }
            }
            else -> "-" to "-"
        }
    }

    LaunchedEffect(userId) {
        if (userId > 0) {
            rutinasViewModel.cargarRutina(userId, oposicionId)
            rutinasLibresViewModel.cargarRutinas(userId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tu actividad") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Ver progreso de un ejercicio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val tipos = listOf("FUERZA", "RESISTENCIA", "VELOCIDAD", "PERSONAL")
                        val selectedIndex = tipos.indexOf(tipoSeleccionado).coerceAtLeast(0)
                        ScrollableTabRow(
                            selectedTabIndex = selectedIndex,
                            edgePadding = 0.dp
                        ) {
                            tipos.forEachIndexed { idx, tipo ->
                                Tab(
                                    selected = idx == selectedIndex,
                                    onClick = { tipoSeleccionado = tipo },
                                    text = { Text(tipo.lowercase().replaceFirstChar { it.uppercase() }) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        if (ejerciciosFiltrados.isNotEmpty()) {
                            Text(
                                text = "Ejercicios ($tipoSeleccionado):",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            ejerciciosFiltrados.forEach { (id, nombre) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = nombre,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Button(
                                        onClick = {
                                            ejercicioSeleccionado = nombre
                                            historialViewModel.cargarEvolucion(userId, id)
                                        }
                                    ) {
                                        Text("Ver")
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        } else {
                            Text(
                                text = "No hay ejercicios para este tipo (según tu rutina).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (uiState.evolucion.isNotEmpty()) {
                val (triple, delta) = stats
                val (last, best, avg) = triple
                val lastPoint = uiState.evolucion.lastOrNull()
                val (ritmoMedio, velocidadMedia) = if (lastPoint != null && (esCarrera(ejercicioSeleccionado) || esNatacion(ejercicioSeleccionado))) {
                    ritmoVelocidadHistorial(ejercicioSeleccionado, lastPoint)
                } else {
                    "-" to "-"
                }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = ejercicioSeleccionado.ifEmpty { "Progreso" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Último", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                                    Text(last?.toString() ?: "-", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Mejor", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                                    Text(best?.toString() ?: "-", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Media", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                                    Text(avg?.let { String.format("%.2f", it) } ?: "-", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }

                            delta?.let {
                                val label = if (it >= 0) "Mejora" else "Baja"
                                Text(
                                    text = "$label: ${String.format("%.2f", it)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            if (esCarrera(ejercicioSeleccionado) || esNatacion(ejercicioSeleccionado)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Ritmo medio", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                                        Text(ritmoMedio, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Velocidad media", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                                        Text(velocidadMedia, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                }
                            }

                            MiniLineChart(
                                values = uiState.evolucion.map { it.valor_conseguido },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                            )
                        }
                    }
                }
            }

            if (uiState.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }

            if (uiState.error.isNotEmpty()) {
                item {
                    Text(
                        text = uiState.error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            if (!uiState.isLoading && uiState.error.isEmpty() && uiState.evolucion.isEmpty() && ejercicioSeleccionado.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📊", style = MaterialTheme.typography.displayMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Selecciona un ejercicio para ver tu progreso.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (uiState.evolucion.isNotEmpty()) {
                val agrupado = uiState.evolucion.groupBy { punto ->
                    punto.fecha_entreno.take(10)
                }
                agrupado.entries.sortedByDescending { it.key }.forEach { (fecha, puntos) ->
                    item {
                        Text(
                            text = DateFormatUtil.formatearSoloFecha(fecha),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(puntos) { punto ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text(
                                    DateFormatUtil.formatearFechaHora(punto.fecha_entreno),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Resultado: ${punto.valor_conseguido}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                punto.duracion_oficial?.let { d ->
                                    Text(
                                        "Duración sesión: ${if (d > 300) "${d / 60} min" else "$d s"}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniLineChart(
    values: List<Double>,
    modifier: Modifier = Modifier
) {
    val color = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

    if (values.size < 2) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Aún no hay suficientes datos para el gráfico", style = MaterialTheme.typography.bodySmall)
        }
        return
    }

    val min = values.minOrNull() ?: 0.0
    val max = values.maxOrNull() ?: 1.0
    val range = (max - min).takeIf { it != 0.0 } ?: 1.0

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val padding = 8f

        
        drawLine(gridColor, Offset(padding, h - padding), Offset(w - padding, h - padding), strokeWidth = 2f)
        drawLine(gridColor, Offset(padding, padding), Offset(padding, h - padding), strokeWidth = 2f)

        val stepX = (w - padding * 2) / (values.size - 1)
        val points = values.mapIndexed { i, v ->
            val x = padding + stepX * i
            val y = (h - padding) - (((v - min) / range).toFloat() * (h - padding * 2))
            Offset(x, y)
        }

        
        for (i in 0 until points.size - 1) {
            drawLine(
                color = color,
                start = points[i],
                end = points[i + 1],
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )
        }

        
        points.forEach { p ->
            drawCircle(color = color, radius = 7f, center = p)
        }
    }
}
