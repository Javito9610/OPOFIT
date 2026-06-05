package com.opofit.miapp.ui.screens.historial

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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Map
import com.opofit.miapp.ui.components.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opofit.miapp.data.responsemodels.ResumenHistorial
import com.opofit.miapp.data.responsemodels.SesionItem
import com.opofit.miapp.gps.util.GpsMetrics
import androidx.compose.ui.graphics.Color
import com.opofit.miapp.ui.components.CalendarHeatmap
import com.opofit.miapp.ui.components.DonutChart
import com.opofit.miapp.ui.components.DonutSlice
import com.opofit.miapp.ui.components.MetricBadge
import com.opofit.miapp.ui.viewmodels.AuthViewModel
import com.opofit.miapp.ui.viewmodels.HistorialAvanzadoViewModel
import com.opofit.miapp.utils.DateFormatUtil
import com.opofit.miapp.gps.ui.GpsViewModel

private enum class HistTab(val label: String) {
    RESUMEN("Resumen"), SESIONES("Sesiones"), GPS("GPS")
}

private fun labelTipo(tipo: String): String = when (tipo.uppercase()) {
    "FUERZA" -> "Fuerza"
    "RESISTENCIA" -> "Resistencia"
    "VELOCIDAD" -> "Velocidad"
    "PERSONAL" -> "Personal"
    else -> tipo.lowercase().replaceFirstChar { it.uppercase() }
}

private fun colorTipo(tipo: String): Color = when (tipo.uppercase()) {
    "FUERZA" -> Color(0xFF1565C0)
    "RESISTENCIA" -> Color(0xFF2E7D32)
    "VELOCIDAD" -> Color(0xFFEF6C00)
    "PERSONAL" -> Color(0xFF6A1B9A)
    else -> Color(0xFF455A64)
}

private fun descripcionPeriodo(p: String): String {
    val cal = java.util.Calendar.getInstance()
    val hoy = cal.time
    when (p.lowercase()) {
        "year" -> cal.add(java.util.Calendar.YEAR, -1)
        "month" -> cal.add(java.util.Calendar.MONTH, -1)
        else -> cal.add(java.util.Calendar.DAY_OF_YEAR, -7)
    }
    val desde = cal.time
    val fmt = java.text.SimpleDateFormat("d MMM", java.util.Locale.forLanguageTag("es-ES"))
    val titulo = when (p.lowercase()) {
        "year" -> "Últimos 12 meses"
        "month" -> "Últimos 30 días"
        else -> "Últimos 7 días"
    }
    return "$titulo · del ${fmt.format(desde)} al ${fmt.format(hoy)}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onOpenSesion: (Int) -> Unit = {},
    onOpenEjercicio: (Int) -> Unit = {},
    onOpenGpsActividad: (String) -> Unit = {},
    onOpenPlan: (Int) -> Unit = {},
    historialAvanzadoViewModel: HistorialAvanzadoViewModel = viewModel(),
    gpsViewModel: GpsViewModel = viewModel()
) {
    val authState by authViewModel.uiState.collectAsState()
    val ui by historialAvanzadoViewModel.uiState.collectAsState()
    val gpsHistory by gpsViewModel.history.collectAsState()
    val pagerState = rememberPagerState(initialPage = 0) { HistTab.entries.size }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(authState.userId) {
        if (authState.userId != null) {
            historialAvanzadoViewModel.cargarResumen()
            historialAvanzadoViewModel.cargarSesiones()
            gpsViewModel.loadHistory()
        }
    }

    LaunchedEffect(ui.syncMessage) {
        ui.syncMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            historialAvanzadoViewModel.consumeSyncMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Tu actividad", fontWeight = FontWeight.Bold) },
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
        PullToRefreshBox(
            isRefreshing = ui.refreshing || gpsHistory.loading,
            onRefresh = {
                historialAvanzadoViewModel.refreshAll { gpsViewModel.loadHistory() }
            },
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                "Desliza abajo para sincronizar: entrenos del reloj y actividades GPS de tu cuenta",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
            PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                HistTab.entries.forEachIndexed { idx, tab ->
                    Tab(
                        selected = pagerState.currentPage == idx,
                        onClick = { scope.launch { pagerState.animateScrollToPage(idx) } },
                        text = { Text(tab.label, fontWeight = FontWeight.Medium) }
                    )
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (HistTab.entries[page]) {
                    HistTab.RESUMEN -> ResumenTab(ui.resumen, ui.periodo) {
                        historialAvanzadoViewModel.setPeriodo(it)
                    }
                    HistTab.SESIONES -> SesionesTab(
                        sesiones = ui.sesiones,
                        filtroTipo = ui.filtroTipo,
                        onFiltroChange = { historialAvanzadoViewModel.setFiltroTipo(it) },
                        onOpenSesion = onOpenSesion,
                        onOpenPlan = onOpenPlan
                    )
                    HistTab.GPS -> GpsTab(
                        actividades = gpsHistory.items,
                        resumen = ui.resumen,
                        onOpen = { onOpenGpsActividad(it) }
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun ResumenTab(resumen: ResumenHistorial?, periodo: String, onPeriodoChange: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("week" to "Semana", "month" to "Mes", "year" to "Año").forEach { (k, label) ->
                        FilterChip(
                            selected = periodo == k,
                            onClick = { onPeriodoChange(k) },
                            label = { Text(label) }
                        )
                    }
                }
                Text(
                    descripcionPeriodo(periodo),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (resumen == null) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Cargando…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            return@LazyColumn
        }
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricBadge("Sesiones", "${resumen.sesiones}", Modifier.weight(1f))
                MetricBadge("Minutos", "${resumen.minutos}", Modifier.weight(1f))
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricBadge("Km recorridos", "%.1f".format(resumen.distanciaTotalKm), Modifier.weight(1f))
                MetricBadge("kcal", "${resumen.kcalTotal}", Modifier.weight(1f))
            }
        }
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        "Calendario de actividad",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Últimos 16 semanas (más oscuro = más entrenos)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    CalendarHeatmap(
                        countsByDate = resumen.heatmap.associate { it.dia to it.sesiones },
                        weeks = 16,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )
                }
            }
        }
        if (resumen.porTipo.isNotEmpty()) {
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Distribución por tipo de entreno",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        DonutChart(
                            slices = resumen.porTipo.map { item ->
                                DonutSlice(
                                    label = labelTipo(item.tipo),
                                    value = item.sesiones.toDouble(),
                                    color = colorTipo(item.tipo)
                                )
                            },
                            centerTitle = "${resumen.porTipo.sumOf { it.sesiones }}",
                            centerSubtitle = "sesiones",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
        if (resumen.topPrs.isNotEmpty()) {
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Mejores marcas",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        resumen.topPrs.forEach { pr ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(pr.ejercicio, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "%.2f".format(pr.valor),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SesionesTab(
    sesiones: List<SesionItem>,
    filtroTipo: String,
    onFiltroChange: (String) -> Unit,
    onOpenSesion: (Int) -> Unit,
    onOpenPlan: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    "TODOS" to "Todas",
                    "OPO" to "Oposición",
                    "PERS" to "Personales"
                ).forEach { (k, label) ->
                    FilterChip(
                        selected = filtroTipo == k,
                        onClick = { onFiltroChange(k) },
                        label = { Text(label) }
                    )
                }
            }
        }
        if (sesiones.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "No hay sesiones registradas en este filtro.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(sesiones, key = { it.id }) { s ->
                SesionCard(s, onClick = { onOpenSesion(s.id) }, onOpenPlan = onOpenPlan)
            }
        }
    }
}

@Composable
private fun SesionCard(
    sesion: SesionItem,
    onClick: () -> Unit,
    onOpenPlan: (Int) -> Unit
) {
    val icon = when {
        sesion.gpsActividadUuid != null -> Icons.AutoMirrored.Filled.DirectionsRun
        sesion.tipoRutina == "PERS" -> Icons.Filled.Bolt
        else -> Icons.Filled.FitnessCenter
    }
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(
                    sesion.fechaEntreno?.let { DateFormatUtil.formatearFechaHora(it) } ?: "Sesión",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    when {
                        sesion.tipoRutina == "PERS" -> sesion.nombrePersonalizado ?: "Rutina personal"
                        sesion.tituloSesion != null -> sesion.tituloSesion
                        sesion.enfoque != null -> "Entreno ${sesion.enfoque.lowercase()}"
                        else -> "Entreno"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2
                )
                val mins = (sesion.duracionSeg ?: 0) / 60
                Text(
                    buildString {
                        if (mins > 0) append("$mins min · ")
                        append("${sesion.nEjercicios} ejercicios")
                        sesion.planNombre?.let { append(" · ").append(it) }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (sesion.idPlan != null) {
                IconButton(onClick = { onOpenPlan(sesion.idPlan) }) {
                    Icon(Icons.Filled.Map, "Ver plan")
                }
            }
        }
    }
}

@Composable
private fun GpsTab(
    actividades: List<com.opofit.miapp.gps.model.ActivitySummary>,
    resumen: com.opofit.miapp.data.responsemodels.ResumenHistorial?,
    onOpen: (String) -> Unit
) {
    val totalKmLocal = actividades.sumOf { it.distanceM } / 1000.0
    val totalKcalLocal = actividades.mapNotNull { it.kcal }.sum()
    val totalKm = if (resumen != null && resumen.distanciaTotalKm > totalKmLocal) resumen.distanciaTotalKm else totalKmLocal
    val totalKcal = if (resumen != null && resumen.kcalTotal > totalKcalLocal) resumen.kcalTotal else totalKcalLocal
    val totalActividades = if (resumen != null) {
        actividades.size + (resumen.gps?.actividades ?: 0)
    } else actividades.size
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricBadge("Total km", "%.1f".format(totalKm), Modifier.weight(1f))
                MetricBadge("Actividades", "$totalActividades", Modifier.weight(1f))
                MetricBadge("kcal", "$totalKcal", Modifier.weight(1f))
            }
        }
        if (actividades.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Aún no tienes actividades GPS.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(actividades, key = { it.id }) { a ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onOpen(a.id) }
                ) {
                    Row(
                        Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(a.type.emoji, style = MaterialTheme.typography.headlineSmall)
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${a.type.display}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "${GpsMetrics.formatDistance(a.distanceM)} · ${GpsMetrics.formatDuration(a.durationSec)} · ${GpsMetrics.formatPace(a.avgPaceSecPerKm)}/km",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
