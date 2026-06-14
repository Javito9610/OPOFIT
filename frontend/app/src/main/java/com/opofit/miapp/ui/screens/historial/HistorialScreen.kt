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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.SelfImprovement
import com.opofit.miapp.ui.components.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
    "GPS" -> "Salidas GPS"
    else -> tipo.lowercase().replaceFirstChar { it.uppercase() }
}

private fun colorTipo(tipo: String): Color = when (tipo.uppercase()) {
    "FUERZA" -> Color(0xFF1565C0)
    "RESISTENCIA" -> Color(0xFF2E7D32)
    "VELOCIDAD" -> Color(0xFFEF6C00)
    "PERSONAL" -> Color(0xFF6A1B9A)
    "GPS" -> Color(0xFF00838F)
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
    // Diálogos de confirmación para acciones destructivas (Strong/Hevy también
    // los muestran antes de borrar para evitar tragedias por toques accidentales).
    var sesionAEliminar by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<SesionItem?>(null) }
    var mostrarVaciarTodo by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

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

    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
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
                actions = {
                    // Menú overflow con "Vaciar historial" (acción destructiva
                    // con confirmación). El icono solo aparece cuando hay algo
                    // que borrar para no inducir tap sin sentido.
                    if ((ui.sesiones?.size ?: 0) > 0) {
                        var expanded by androidx.compose.runtime.remember {
                            androidx.compose.runtime.mutableStateOf(false)
                        }
                        IconButton(onClick = { expanded = true }) {
                            Icon(
                                androidx.compose.material.icons.Icons.Filled.MoreVert,
                                contentDescription = "Más opciones",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        androidx.compose.material3.DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("Vaciar historial") },
                                onClick = {
                                    expanded = false
                                    mostrarVaciarTodo = true
                                },
                                leadingIcon = {
                                    Icon(
                                        androidx.compose.material.icons.Icons.Filled.DeleteSweep,
                                        contentDescription = null
                                    )
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
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
                        onOpenPlan = onOpenPlan,
                        onOpenGps = onOpenGpsActividad,
                        onEliminarSesion = { ses -> sesionAEliminar = ses }
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
    // Diálogo confirmación de borrar UNA sesión.
    sesionAEliminar?.let { ses ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { sesionAEliminar = null },
            icon = {
                Icon(
                    androidx.compose.material.icons.Icons.Filled.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("¿Eliminar esta sesión?") },
            text = {
                Text(
                    "Vas a borrar este entrenamiento del historial. Las marcas que rompiste con él volverán a estar disponibles. No se puede deshacer."
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    historialAvanzadoViewModel.borrarSesion(ses.id)
                    sesionAEliminar = null
                }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { sesionAEliminar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
    // Diálogo confirmación de vaciar TODO.
    if (mostrarVaciarTodo) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { mostrarVaciarTodo = false },
            icon = {
                Icon(
                    androidx.compose.material.icons.Icons.Filled.DeleteSweep,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("¿Vaciar todo el historial?") },
            text = {
                Text(
                    "Se eliminarán TODAS tus sesiones de entreno y sus marcas. Esto no afecta a las salidas GPS (esas se borran desde su propia pantalla). No se puede deshacer."
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    historialAvanzadoViewModel.vaciarHistorial()
                    mostrarVaciarTodo = false
                }) {
                    Text("Vaciar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { mostrarVaciarTodo = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
    com.opofit.miapp.ui.components.CoachMarkOverlay(
        screenKey = "historial_v1",
        steps = listOf(
            com.opofit.miapp.ui.components.CoachStep(
                title = "Todo lo que has hecho",
                text = "Aquí ves tus sesiones de entreno, simulacros y rutas GPS, juntos en una única línea de tiempo."
            ),
            com.opofit.miapp.ui.components.CoachStep(
                title = "Toca para ver detalle",
                text = "Cada sesión abre una vista con gráficas. Si es de OPO, también te muestra qué pilar trabajaste."
            )
        )
    )
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
                // Sesiones totales = entrenos + GPS. La etiqueta secundaria
                // hace el desglose para que el usuario lo entienda de un vistazo.
                val sub = when {
                    resumen.sesionesGps > 0 && resumen.sesionesEntrenos > 0 ->
                        "${resumen.sesionesEntrenos} entrenos + ${resumen.sesionesGps} GPS"
                    resumen.sesionesGps > 0 -> "${resumen.sesionesGps} GPS"
                    else -> null
                }
                MetricBadge(
                    label = "Sesiones",
                    value = "${resumen.sesiones}",
                    modifier = Modifier.weight(1f),
                    sublabel = sub
                )
                MetricBadge(
                    // "Tiempo" en lugar de "Minutos" porque el valor puede ser
                    // "5h 30 min" o "2 semanas" y eso no son minutos.
                    label = "Tiempo",
                    value = com.opofit.miapp.utils.TimeFormatUtil.formatDuracionLegible(resumen.minutos),
                    modifier = Modifier.weight(1f)
                )
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
            // El número de semanas y la altura cambian según el período para
            // que el calendario tenga sentido: 4 semanas para "Semana", 16
            // para "Mes", 52 para "Año". Antes era siempre 16 y el chip de
            // período no afectaba al heatmap.
            val (weeksMostrar, descripCal) = when (periodo.lowercase()) {
                "week" -> 4 to "Últimas 4 semanas"
                "year" -> 52 to "Últimas 52 semanas"
                else -> 16 to "Últimas 16 semanas"
            }
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Calendario de actividad",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        com.opofit.miapp.ui.components.InfoTip(
                            title = "¿Qué significan los cuadritos?",
                            text = "Cada cuadrito es un día. Cuanto más oscuro, más sesiones (entrenos + salidas GPS) registraste ese día. Los días vacíos se quedan grises. Estilo \"contribuciones de GitHub\"."
                        )
                    }
                    Text(
                        "$descripCal · entrenos + salidas GPS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    CalendarHeatmap(
                        countsByDate = resumen.heatmap.associate { it.dia to it.sesiones },
                        weeks = weeksMostrar,
                        // +20 dp: el componente ahora reserva 28 px arriba para
                        // los meses; sin un extra de alto, las celdas quedan
                        // muy aplastadas y los meses casi sobre la rejilla.
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (weeksMostrar >= 52) 130.dp else 160.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    com.opofit.miapp.ui.components.HeatmapLegend()
                }
            }
        }
        if (resumen.porTipo.isNotEmpty()) {
            item {
                com.opofit.miapp.ui.components.ChartSection(
                    title = "Distribución por tipo de entreno",
                    subtitle = "Sesiones del periodo seleccionado",
                    icon = Icons.Filled.PieChart,
                    hint = "El centro resume el volumen total del periodo"
                ) {
                    DonutChart(
                        slices = resumen.porTipo.map { item ->
                            DonutSlice(
                                label = labelTipo(item.tipo),
                                value = item.sesiones.toDouble(),
                                color = colorTipo(item.tipo)
                            )
                        },
                        centerTitle = "${resumen.porTipo.sumOf { it.sesiones }}",
                        centerSubtitle = if (resumen.minutos > 0) "ses · ${resumen.minutos} min"
                                         else "sesiones",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        // Volumen por grupo muscular: solo cuando hay al menos 5 series para
        // que el chart tenga sentido. Una sola serie ocuparía el ancho entero
        // y daría una impresión engañosa.
        if (resumen.porGrupoMuscular.sumOf { it.series } >= 5) {
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        com.opofit.miapp.ui.components.VolumenMusculosChart(
                            datos = resumen.porGrupoMuscular
                        )
                    }
                }
            }
        }
        if (resumen.topPrs.isNotEmpty()) {
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                Icons.Filled.EmojiEvents,
                                contentDescription = null,
                                tint = Color(0xFFFFA000)
                            )
                            Text(
                                "Tus mejores marcas",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        // Cada PR se muestra como una fila pro con nombre, valor con
                        // unidad legible y un chip de pilar al lado.
                        resumen.topPrs.forEachIndexed { idx, pr ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Posición #1, #2, #3 con color medalla
                                val medalColor = when (idx) {
                                    0 -> Color(0xFFFFC107)
                                    1 -> Color(0xFFBDBDBD)
                                    2 -> Color(0xFFCD7F32)
                                    else -> MaterialTheme.colorScheme.outline
                                }
                                Text(
                                    "#${idx + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = medalColor
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        pr.ejercicio,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    pr.pilar?.let { p ->
                                        Text(
                                            labelTipo(p),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colorTipo(p)
                                        )
                                    }
                                }
                                Text(
                                    formatPrConUnidad(pr.valor, pr.unidad, pr.scoreTipo),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
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

/** Formato pro de una marca con unidad: "120 kg", "12 reps", "0:49 min", etc. */
private fun formatPrConUnidad(valor: Double, unidad: String?, scoreTipo: String?): String {
    val num = if (valor == valor.toLong().toDouble()) valor.toLong().toString()
              else "%.1f".format(valor)
    val u = when {
        unidad != null -> com.opofit.miapp.utils.EntrenoValidation.unidadLegible(unidad)
        scoreTipo == "peso" -> "kg"
        scoreTipo == "tiempo" || scoreTipo == "tiempo_max" -> "seg"
        scoreTipo == "distancia" -> "m"
        scoreTipo == "calorias" -> "kcal"
        scoreTipo == "rondas" || scoreTipo == "rondas_completadas" -> "rondas"
        else -> "reps"
    }
    return "$num $u"
}

@Composable
private fun SesionesTab(
    sesiones: List<SesionItem>,
    filtroTipo: String,
    onFiltroChange: (String) -> Unit,
    onOpenSesion: (Int) -> Unit,
    onOpenPlan: (Int) -> Unit,
    onOpenGps: (String) -> Unit = {},
    onEliminarSesion: (SesionItem) -> Unit = {}
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
                SesionCard(
                    sesion = s,
                    onClick = { onOpenSesion(s.id) },
                    onOpenPlan = onOpenPlan,
                    onOpenGps = onOpenGps,
                    onEliminar = { onEliminarSesion(s) }
                )
            }
        }
    }
}

@Composable
private fun SesionCard(
    sesion: SesionItem,
    onClick: () -> Unit,
    onOpenPlan: (Int) -> Unit,
    onOpenGps: (String) -> Unit = {},
    onEliminar: () -> Unit = {}
) {
    val icon = when {
        sesion.gpsActividadUuid != null -> when (sesion.enfoque?.uppercase()) {
            "BICI", "CICLISMO" -> Icons.AutoMirrored.Filled.DirectionsBike
            "MARCHA", "CAMINAR", "SENDERISMO" -> Icons.AutoMirrored.Filled.DirectionsWalk
            else -> Icons.AutoMirrored.Filled.DirectionsRun
        }
        sesion.tipoRutina == "PERS" -> Icons.Filled.Bolt
        sesion.enfoque?.uppercase() == "RESISTENCIA" -> Icons.AutoMirrored.Filled.DirectionsRun
        sesion.enfoque?.uppercase() == "VELOCIDAD" -> Icons.Filled.Speed
        sesion.enfoque?.uppercase() == "FUERZA" -> Icons.Filled.FitnessCenter
        sesion.enfoque?.uppercase() == "NATACION" || sesion.enfoque?.uppercase() == "NATACIÓN" -> Icons.Filled.Pool
        sesion.enfoque?.uppercase() == "BICI" || sesion.enfoque?.uppercase() == "CICLISMO" -> Icons.AutoMirrored.Filled.DirectionsBike
        sesion.enfoque?.uppercase() == "FLEXIBILIDAD" || sesion.enfoque?.uppercase() == "MOVILIDAD" -> Icons.Filled.SelfImprovement
        sesion.tipoRutina == "OPO" -> Icons.Filled.EmojiEvents
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
            // Acciones de la sesión, en orden de relevancia: ver mapa GPS si
            // hubo ruta, ver plan, eliminar. Antes el icono "Map" se mostraba
            // siempre que había idPlan y abría el plan, lo que confundía
            // (parecía un mapa de GPS pero abría una tabla).
            if (!sesion.gpsActividadUuid.isNullOrBlank()) {
                IconButton(onClick = { onOpenGps(sesion.gpsActividadUuid) }) {
                    Icon(
                        Icons.Filled.Map,
                        contentDescription = "Ver ruta GPS"
                    )
                }
            }
            if (sesion.idPlan != null) {
                IconButton(onClick = { onOpenPlan(sesion.idPlan) }) {
                    Icon(
                        androidx.compose.material.icons.Icons.Filled.Insights,
                        contentDescription = "Ver plan"
                    )
                }
            }
            IconButton(onClick = onEliminar) {
                Icon(
                    androidx.compose.material.icons.Icons.Filled.DeleteOutline,
                    contentDescription = "Eliminar sesión",
                    tint = MaterialTheme.colorScheme.error
                )
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
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Icon(
                                com.opofit.miapp.ui.components.EnfoqueIcons.forActivityType(a.type),
                                contentDescription = a.type.display,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(10.dp)
                                    .size(24.dp)
                            )
                        }
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
