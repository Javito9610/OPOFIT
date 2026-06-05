package com.opofit.miapp.ui.screens.rutinas

import android.content.Intent
import android.net.Uri
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
import com.opofit.miapp.ui.components.EntornoEntrenoSheet
import com.opofit.miapp.ui.components.ExerciseStickFigure
import com.opofit.miapp.ui.components.PlanCalendarioMes
import com.opofit.miapp.ui.components.PlanDiaCard
import com.opofit.miapp.ui.components.PlanPersonalizacionCard
import com.opofit.miapp.ui.components.SectionHeader
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
    var unitDist by remember { mutableStateOf("km") }
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
        "💪 Fuerza" to "FUERZA",
        "🏃 Resistencia" to "RESISTENCIA",
        "⚡ Velocidad" to "VELOCIDAD"
    )
    var selectedTab by remember { mutableIntStateOf(0) }
    var vistaPlan by remember { mutableIntStateOf(0) }
    val mesActual = remember { YearMonth.now() }
    var mesCalendario by remember { mutableStateOf(mesActual) }

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
        onConfirmar = { entorno -> rutinasViewModel.guardarEntreno(userId, oposicionId, entorno) }
    )

    Scaffold(
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
                uiState.error.isNotEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                                            ),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                                            Text("Ver rutinas libres mientras tanto")
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
                                            Text("Ver rutinas libres (mientras tanto)")
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
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer
                                            )
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
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                            )
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
                                        if (!uiState.entornoEntreno.isNullOrBlank()) {
                                            OutlinedButton(
                                                onClick = {
                                                    onNavigateToLugaresEntreno(
                                                        MapaEntrenoNav.entornoATipoLugar(uiState.entornoEntreno)
                                                    )
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Ver lugares cerca (${uiState.entornoEntreno})")
                                            }
                                        }
                                    }
                                    items(plan.semana.size) { i ->
                                        val dia = plan.semana[i]
                                        Card(Modifier.fillMaxWidth()) {
                                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                PlanDiaCard(
                                                    dia = dia,
                                                    onEntrenar = onNavigateToEntrenamientos,
                                                    expanded = !dia.es_hoy,
                                                    onOtraOpcion = if (!dia.completada && !uiState.entornoEntreno.isNullOrBlank()) {
                                                        { rutinasViewModel.regenerarDia(userId, oposicionId, dia.id_plan_dia) }
                                                    } else null,
                                                    regenerando = uiState.regenerandoDiaId == dia.id_plan_dia
                                                )
                                                dia.ejercicios.take(4).forEach { ej ->
                                                    val adj = if (ej.personalizado && ej.series_base != null) {
                                                        " (${ej.series_base}→${ej.series})"
                                                    } else ""
                                                    Row(
                                                        Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        ExerciseStickFigure(
                                                            tipoIlustracion = ej.tipo_ilustracion,
                                                            size = 44.dp
                                                        )
                                                        Column(Modifier.weight(1f)) {
                                                            Text(
                                                                "${ej.series}×${PrescripcionFormat.formatRepeticiones(ej.repeticiones, ej.unidad, ej.nombre)} ${ej.nombre}$adj",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = if (ej.personalizado || ej.sustituido)
                                                                    MaterialTheme.colorScheme.primary
                                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                            ej.instrucciones_tecnicas?.takeIf { it.isNotBlank() }?.let { tip ->
                                                                Text(
                                                                    tip,
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                                if (dia.ejercicios.size > 4) {
                                                    Text(
                                                        "+${dia.ejercicios.size - 4} ejercicios más",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
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
                                        val hoy = plan.sesion_hoy
                                        if (hoy != null && !hoy.completada) {
                                            Button(
                                                onClick = {
                                                    onNavigateToEntrenamientos(
                                                        hoy.enfoque,
                                                        hoy.id_plan_dia,
                                                        hoy.id_rutina_opo
                                                    )
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Empezar ${hoy.nombre_dia.lowercase()}")
                                            }
                                        }
                                        OutlinedButton(
                                            onClick = onNavigateToRutinasLibres,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Banco de ejercicios · rutinas libres")
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
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer
                                            )
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
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                            )
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
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                                )
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
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                                        Button(
                                            onClick = {
                                                val sesion = uiState.planSemanal?.semana?.find {
                                                    it.enfoque == selectedEnfoque
                                                }
                                                onNavigateToEntrenamientos(
                                                    selectedEnfoque,
                                                    sesion?.id_plan_dia,
                                                    sesion?.id_rutina_opo
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("💪 Comenzar Entrenamiento")
                                        }
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
}
