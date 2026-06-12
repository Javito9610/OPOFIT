package com.opofit.miapp.ui.screens.rutinas

import com.opofit.miapp.ui.components.ElevatedCard
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.TabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opofit.miapp.data.local.TokenManager
import com.opofit.miapp.data.responsemodels.EjercicioPlan
import com.opofit.miapp.ui.components.EntornoEntrenoSheet
import com.opofit.miapp.ui.components.ExerciseDetailSheet
import com.opofit.miapp.ui.components.PlanCalendarioMes
import com.opofit.miapp.ui.components.EntrenoHoyHeroCard
import com.opofit.miapp.ui.components.PlanDiaCard
import com.opofit.miapp.ui.components.PlanEjercicioRow
import com.opofit.miapp.ui.components.PlanPersonalizacionCard
import com.opofit.miapp.ui.components.PlanSemanaResumenRow
import com.opofit.miapp.ui.components.PrimaryActionButton
import com.opofit.miapp.ui.components.SectionHeader
import androidx.compose.material.icons.filled.PlayArrow
import com.opofit.miapp.ui.components.enfoqueLabel
import java.time.YearMonth
import com.opofit.miapp.ui.viewmodels.AuthViewModel
import com.opofit.miapp.ui.viewmodels.RutinasViewModel
import com.opofit.miapp.utils.FitnessMode
import com.opofit.miapp.utils.MapaEntrenoNav
import com.opofit.miapp.utils.PrescripcionFormat
import com.opofit.miapp.utils.Units
import com.opofit.miapp.utils.UrlOpener
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RutinasScreen(
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEntrenamientos: (enfoque: String?, idPlanDia: Int?, idRutinaOpo: Int?) -> Unit,
    onNavigateToRutinasLibres: () -> Unit,
    onNavigateToEditarPerfil: () -> Unit,
    onNavigateToPlanHistorial: (Int) -> Unit = {},
    onNavigateToLugaresEntreno: (tipo: String) -> Unit = {},
    rutinasViewModel: RutinasViewModel = viewModel()
) {
    val authState by authViewModel.uiState.collectAsState()
    val uiState by rutinasViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    var unitDist by remember { mutableStateOf("km") }

    LaunchedEffect(uiState.msgExito) {
        uiState.msgExito?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            rutinasViewModel.consumirMsgExito()
        }
    }
    LaunchedEffect(uiState.msgAviso) {
        uiState.msgAviso?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            rutinasViewModel.consumirMsgAviso()
        }
    }
    LaunchedEffect(Unit) {
        tokenManager.getUnitDistancia().collectLatest { u ->
            if (!u.isNullOrBlank()) unitDist = u
        }
    }

    val userId = authState.userId ?: 0
    val esFitness = FitnessMode.isFitness(authState.modoUso)
    val oposicionId = FitnessMode.planOposicionId(authState.oposicionId, authState.modoUso)

    LaunchedEffect(userId) {
        if (userId > 0) {
            rutinasViewModel.cargarRutina(userId, oposicionId)
        }
    }

    
    val enfoqueTabs = listOf(
        "Fuerza" to "FUERZA",
        "Resistencia" to "RESISTENCIA",
        "Velocidad" to "VELOCIDAD"
    )
    var selectedTab by remember { mutableIntStateOf(0) }
    var vistaPlan by remember { mutableIntStateOf(0) }
    val mesActual = remember { YearMonth.now() }
    var mesCalendario by remember { mutableStateOf(mesActual) }
    var ejercicioDetalle by remember { mutableStateOf<EjercicioPlan?>(null) }
    var prescripcionDetalle by remember { mutableStateOf("") }

    LaunchedEffect(userId, oposicionId, mesCalendario) {
        if (userId > 0) {
            rutinasViewModel.cargarCalendario(userId, oposicionId, mesCalendario.year, mesCalendario.monthValue)
        }
    }

    EntornoEntrenoSheet(
        visible = uiState.mostrarSheetEntorno,
        opciones = uiState.entornosOpciones,
        seleccionado = uiState.entornoEntreno,
        onDismiss = { rutinasViewModel.cerrarSheetEntorno() },
        onConfirmar = { entorno -> rutinasViewModel.guardarEntreno(userId, oposicionId, entorno) },
        // Flujo 2 pasos: para CASA/PISTA/MIXTO pregunta el material y lo
        // guarda junto al entorno → la IA genera el plan con ese material.
        onConfirmarConMaterial = { entorno, material ->
            rutinasViewModel.guardarEntrenoConMaterial(userId, oposicionId, entorno, material)
        }
    )

    ExerciseDetailSheet(
        ejercicio = ejercicioDetalle,
        prescripcion = prescripcionDetalle,
        visible = ejercicioDetalle != null,
        onDismiss = { ejercicioDetalle = null }
    )

    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        topBar = {
            TopAppBar(
                title = { Text("Plan de entrenamiento") },
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
        PullToRefreshBox(
            isRefreshing = uiState.isLoading && (uiState.planSemanal != null || uiState.rutinaCompleta.isNotEmpty()),
            onRefresh = {
                rutinasViewModel.cargarRutina(userId, oposicionId)
                rutinasViewModel.cargarCalendario(userId, oposicionId, mesCalendario.year, mesCalendario.monthValue)
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.error.isNotEmpty() &&
                    uiState.planSemanal?.semana.isNullOrEmpty() &&
                    uiState.rutinaCompleta.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(), containerColor = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = "No se pudo cargar el entrenamiento",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = uiState.error,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Start
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { rutinasViewModel.cargarRutina(userId, oposicionId) }) {
                            Text("Reintentar")
                        }
                    }
                }
                else -> {
                    if (
                            uiState.rutinaCompleta.isEmpty() &&
                            uiState.planSemanal?.semana.isNullOrEmpty() &&
                            !uiState.isLoading
                        ) {
                            val faltan = uiState.pruebasFaltantes ?: 0
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 24.dp, horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (faltan > 0) {
                                        ElevatedCard(
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(20.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.FactCheck,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(44.dp),
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Text(
                                                    text = "Marcas pendientes",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    textAlign = TextAlign.Center
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = if (faltan == 1) {
                                                        "Falta registrar una prueba oficial en tu perfil. Al guardarla, calcularemos tu nivel y te mostraremos la rutina recomendada para tu oposición."
                                                    } else {
                                                        "Faltan registrar $faltan pruebas oficiales en tu perfil. Cuando estén completas, podremos asignarte un nivel y personalizar tu entrenamiento."
                                                    },
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                        FilledTonalButton(
                                            onClick = onNavigateToEditarPerfil,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                        ) {
                                            Text("Ir a añadir marcas")
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedButton(
                                            onClick = { rutinasViewModel.cargarRutina(userId, oposicionId) },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Actualizar (si ya las guardé)")
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedButton(
                                            onClick = onNavigateToRutinasLibres,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            com.opofit.miapp.ui.components.ButtonText("Ver rutinas libres")
                                        }
                                    } else {
                                        Text(
                                            text = "Aún no podemos generar tu rutina",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Añade tus marcas en Perfil → Editar perfil para que podamos calcular tu nivel.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        FilledTonalButton(
                                            onClick = onNavigateToEditarPerfil,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Ir al perfil")
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedButton(
                                            onClick = onNavigateToRutinasLibres,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            com.opofit.miapp.ui.components.ButtonText("Ver rutinas libres")
                                        }
                                    }
                                }
                            }
                        } else if (uiState.planSemanal?.semana?.isNotEmpty() == true) {
                            val plan = uiState.planSemanal!!
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (!esFitness && uiState.notaActual.isNotEmpty()) {
                                    item {
                                        // Cabecera pro tipo Strong/Hevy: dos filas claras.
                                        //   1) Nivel REAL del plan que se está haciendo (el activo).
                                        //   2) Nota + nivel calculado debajo, en texto secundario.
                                        // Antes solo se veía "Nota 10.00 · AVANZADO" y el usuario pensaba
                                        // que estaba entrenando AVANZADO cuando en realidad sin Premium
                                        // se le sirve BASICO.
                                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                                            Column(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        "Plan activo:",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    androidx.compose.material3.AssistChip(
                                                        onClick = {},
                                                        label = {
                                                            Text(
                                                                "Nivel ${(uiState.nivelRutinasMostradas ?: uiState.nivelAsignado).lowercase().replaceFirstChar { it.uppercase() }}",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                        },
                                                        colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
                                                            containerColor = MaterialTheme.colorScheme.primaryContainer
                                                        )
                                                    )
                                                }
                                                Text(
                                                    "Tu nota actual: ${uiState.notaActual} (nivel calculado ${uiState.nivelAsignado.lowercase()})",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                                if (!uiState.msgPremium.isNullOrBlank()) {
                                    item {
                                        ElevatedCard(
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    androidx.compose.material.icons.Icons.Filled.Lock,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.tertiary
                                                )
                                                Text(
                                                    text = uiState.msgPremium!!,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                }
                                item {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(onClick = { onNavigateToPlanHistorial(plan.id_plan) }) {
                                            Text("Historial del plan")
                                        }
                                    }
                                }
                                item {
                                    TabRow(selectedTabIndex = vistaPlan) {
                                        Tab(selected = vistaPlan == 0, onClick = { vistaPlan = 0 }, text = { Text("Semana") })
                                        Tab(selected = vistaPlan == 1, onClick = { vistaPlan = 1 }, text = { Text("Mes") })
                                    }
                                }
                                if (vistaPlan == 1) {
                                    item {
                                        val cal = uiState.calendario
                                        Row(
                                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedButton(onClick = { mesCalendario = mesCalendario.minusMonths(1) }) {
                                                Text("‹")
                                            }
                                            Text(
                                                "${mesCalendario.monthValue}/${mesCalendario.year}",
                                                fontWeight = FontWeight.Bold
                                            )
                                            OutlinedButton(onClick = { mesCalendario = mesCalendario.plusMonths(1) }) {
                                                Text("›")
                                            }
                                        }
                                        if (cal != null) {
                                            PlanCalendarioMes(
                                                year = cal.year,
                                                month = cal.month,
                                                dias = cal.dias,
                                                onDiaClick = { d ->
                                                    val dia = plan.semana.find { it.id_plan_dia == d.id_plan_dia }
                                                    if (dia != null) {
                                                        onNavigateToEntrenamientos(
                                                            dia.enfoque,
                                                            dia.id_plan_dia,
                                                            dia.id_rutina_opo
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                                if (vistaPlan == 0) {
                                    plan.sesion_hoy?.let { hoy ->
                                        if (!hoy.completada) {
                                            item {
                                                EntrenoHoyHeroCard(
                                                    titulo = hoy.nombre_dia,
                                                    subtitulo = hoy.titulo,
                                                    enfoque = hoy.enfoque,
                                                    onEmpezar = {
                                                        onNavigateToEntrenamientos(
                                                            hoy.enfoque,
                                                            hoy.id_plan_dia,
                                                            hoy.id_rutina_opo
                                                        )
                                                    },
                                                    onPrepararRuta = if (hoy.enfoque.equals("RESISTENCIA", ignoreCase = true)) {
                                                        {
                                                            onNavigateToEntrenamientos(
                                                                hoy.enfoque,
                                                                hoy.id_plan_dia,
                                                                hoy.id_rutina_opo
                                                            )
                                                        }
                                                    } else null
                                                )
                                            }
                                        }
                                    }
                                    item {
                                        PlanSemanaResumenRow(dias = plan.semana)
                                    }
                                    plan.personalizacion?.let { perso ->
                                        item { PlanPersonalizacionCard(personalizacion = perso) }
                                    }
                                    item {
                                        SectionHeader(
                                            title = "Microciclo · ${plan.dias_por_semana} días",
                                            subtitle = plan.personalizacion?.resumen
                                                ?: "Plan adaptado a tus marcas y nivel"
                                        )
                                        Row(
                                            Modifier.fillMaxWidth().padding(top = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedButton(onClick = { rutinasViewModel.abrirSheetEntorno() }) {
                                                Text(
                                                    if (uiState.entornoEntreno.isNullOrBlank()) "¿Dónde entrenas?"
                                                    else "Cambiar entorno"
                                                )
                                            }
                                            FilledTonalButton(
                                                onClick = { rutinasViewModel.regenerarPlan(userId, oposicionId) },
                                                enabled = !uiState.regenerandoPlan && !uiState.entornoEntreno.isNullOrBlank()
                                            ) {
                                                if (uiState.regenerandoPlan) {
                                                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                                } else {
                                                    Text("Generar otra semana")
                                                }
                                            }
                                        }
                                        Text(
                                            "«Generar otra semana» cambia todos los días. «Otra variante» solo en un día.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                        if (!uiState.entornoEntreno.isNullOrBlank()) {
                                            OutlinedButton(
                                                onClick = {
                                                    onNavigateToLugaresEntreno(
                                                        MapaEntrenoNav.entornoATipoLugar(uiState.entornoEntreno)
                                                    )
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                com.opofit.miapp.ui.components.ButtonText("Ver lugares cerca")
                                            }
                                        }
                                    }
                                    items(
                                        plan.semana.size,
                                        key = { i ->
                                            val d = plan.semana[i]
                                            val ejKey = d.ejercicios.joinToString("|") {
                                                "${it.nombre}:${it.series}:${it.repeticiones}"
                                            }
                                            "${d.id_plan_dia}_${plan.personalizacion?.variacion_seed ?: 0}_${d.titulo}_$ejKey"
                                        }
                                    ) { i ->
                                        val dia = plan.semana[i]
                                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            PlanDiaCard(
                                                dia = dia,
                                                onEntrenar = onNavigateToEntrenamientos,
                                                expanded = !dia.es_hoy,
                                                onOtraOpcion = if (!dia.completada && !uiState.entornoEntreno.isNullOrBlank()) {
                                                    { rutinasViewModel.regenerarDia(userId, oposicionId, dia.id_plan_dia) }
                                                } else null,
                                                regenerando = uiState.regenerandoDiaId == dia.id_plan_dia
                                            )
                                            if (dia.ejercicios.isNotEmpty()) {
                                                ElevatedCard(Modifier.fillMaxWidth()) {
                                                    Column(
                                                        Modifier.padding(12.dp),
                                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        dia.ejercicios.take(4).forEach { ej ->
                                                            val prescripcion = "${ej.series}×${PrescripcionFormat.formatRepeticiones(ej.repeticiones, ej.unidad, ej.nombre)}"
                                                            PlanEjercicioRow(
                                                                prescripcion = prescripcion,
                                                                nombre = ej.nombre,
                                                                destacado = ej.personalizado || ej.sustituido,
                                                                onInfoClick = {
                                                                    ejercicioDetalle = ej
                                                                    prescripcionDetalle = prescripcion
                                                                }
                                                            )
                                                        }
                                                        if (dia.ejercicios.size > 4) {
                                                            Text(
                                                                "+${dia.ejercicios.size - 4} más",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                item {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = onNavigateToRutinasLibres,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            com.opofit.miapp.ui.components.ButtonText("Banco de ejercicios libres")
                                        }
                                    }
                                }
                            }
                        } else if (uiState.rutinaCompleta.isNotEmpty()) {
                            val selectedEnfoque = enfoqueTabs[selectedTab].second
                            val filteredBlocks = uiState.rutinaCompleta.filter {
                                it.bloque.equals(selectedEnfoque, ignoreCase = true)
                            }
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (!esFitness && uiState.notaActual.isNotEmpty()) {
                                    item {
                                        // Mismo bloque pro que en la rama de plan-semanal arriba.
                                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                                            Column(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        "Plan activo:",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    androidx.compose.material3.AssistChip(
                                                        onClick = {},
                                                        label = {
                                                            Text(
                                                                "Nivel ${(uiState.nivelRutinasMostradas ?: uiState.nivelAsignado).lowercase().replaceFirstChar { it.uppercase() }}",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                        },
                                                        colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
                                                            containerColor = MaterialTheme.colorScheme.primaryContainer
                                                        )
                                                    )
                                                }
                                                Text(
                                                    "Tu nota actual: ${uiState.notaActual} (nivel calculado ${uiState.nivelAsignado.lowercase()})",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                                if (false) {  // bloque legacy reemplazado arriba
                                    item {
                                        ElevatedCard(
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "Nota ${uiState.notaActual} · ${uiState.nivelAsignado}",
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }
                                if (!uiState.msgPremium.isNullOrBlank()) {
                                    item {
                                        ElevatedCard(
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = uiState.msgPremium!!,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                        }
                                    }
                                }
                                item {
                                ScrollableTabRow(
                                    selectedTabIndex = selectedTab,
                                        edgePadding = 0.dp
                                ) {
                                    enfoqueTabs.forEachIndexed { index, (label, _) ->
                                        Tab(
                                            selected = selectedTab == index,
                                            onClick = { selectedTab = index },
                                            text = { Text(label) }
                                        )
                                    }
                                }
                                }
                                    if (filteredBlocks.isEmpty()) {
                                        item {
                                            ElevatedCard(
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Box(
                                                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "No hay ejercicios de ${selectedEnfoque.lowercase()} disponibles para tu nivel.",
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    items(filteredBlocks) { bloque ->
                                        ElevatedCard(
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Text(
                                                    text = bloque.bloque,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                bloque.ejercicios.forEachIndexed { index, ejercicio ->
                                                    if (index > 0) Divider(modifier = Modifier.padding(vertical = 4.dp))
                                                    val nombreEj = Units.nombreConEquivalenciaDistancia(
                                                        ejercicio.nombre,
                                                        unitDist
                                                    )
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = nombreEj,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        Text(
                                                            text = "${ejercicio.series}x${PrescripcionFormat.formatRepeticiones(ejercicio.repeticiones, ejercicio.unidad, ejercicio.nombre)}",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.secondary
                                                        )
                                                    }
                                                    if (!ejercicio.video_url.isNullOrBlank()) {
                                                        TextButton(
                                                            onClick = {
                                                                UrlOpener.open(context, ejercicio.video_url)
                                                            }
                                                        ) {
                                                            Text("Ver vídeo")
                                                        }
                                                    }
                                                    Text(
                                                        text = "Descanso: ${ejercicio.descanso}s",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                item {
                                Column(
                                        modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PrimaryActionButton(
                                        text = "Comenzar Entrenamiento",
                                        icon = Icons.Filled.PlayArrow,
                                        onClick = {
                                            val sesion = uiState.planSemanal?.semana?.find {
                                                it.enfoque == selectedEnfoque
                                            }
                                            onNavigateToEntrenamientos(
                                                selectedEnfoque,
                                                sesion?.id_plan_dia,
                                                sesion?.id_rutina_opo
                                            )
                                        }
                                    )
                                    OutlinedButton(
                                        onClick = onNavigateToRutinasLibres,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Ver rutinas libres")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }
    com.opofit.miapp.ui.components.CoachMarkOverlay(
        screenKey = "rutinas_v1",
        steps = listOf(
            com.opofit.miapp.ui.components.CoachStep(
                title = "Tu plan de hoy",
                text = "Arriba aparece la sesión recomendada para hoy. Si está marcada en verde, ya la has completado. Tira hacia abajo para refrescar."
            ),
            com.opofit.miapp.ui.components.CoachStep(
                title = "Plan semanal completo",
                text = "Debajo ves toda la semana. Cada día con su pilar (Fuerza, Resistencia, Velocidad). Toca un día para ver detalle."
            ),
            com.opofit.miapp.ui.components.CoachStep(
                title = "Si no te convence, regenera",
                text = "El botón «Otra opción» te propone una variante distinta. Útil si hoy no tienes material para hacer un ejercicio en concreto."
            )
        )
    )
    }
}
