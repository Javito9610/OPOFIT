package com.opofit.miapp.ui.screens.home

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.foundation.clickable
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.opofit.miapp.gps.util.GpsPermissionRequest
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.opofit.miapp.ui.theme.AccentIndigo
import com.opofit.miapp.ui.theme.AccentOrange
import com.opofit.miapp.ui.theme.AccentSlate
import com.opofit.miapp.ui.theme.AccentTeal
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.opofit.miapp.ui.utils.isCompactScreen
import com.opofit.miapp.ui.utils.isVeryCompactScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opofit.miapp.ui.components.CoachMarkOverlay
import com.opofit.miapp.ui.components.CoachStep
import com.opofit.miapp.ui.components.ElevatedCard
import com.opofit.miapp.ui.components.EntrenoHoyHeroCard
import com.opofit.miapp.ui.components.OpoFitLogo
import com.opofit.miapp.ui.components.ErrorState
import com.opofit.miapp.ui.components.ProfileAvatar
import com.opofit.miapp.ui.components.SectionHeader
import com.opofit.miapp.ui.components.enfoqueLabel
import com.opofit.miapp.ui.components.StatCard
import com.opofit.miapp.ui.components.WeekActivityChart
import androidx.compose.material3.TextButton
import com.opofit.miapp.ui.viewmodels.HomeViewModel
import com.opofit.miapp.utils.AppEvents
import com.opofit.miapp.utils.MapaEntrenoNav

private data class QuickLink(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val containerColor: Color,
    val contentColor: Color,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenDrawer: () -> Unit = {},
    onNavigateToRutinas: () -> Unit,
    onNavigateToEntrenamientos: (enfoque: String?, idPlanDia: Int?, idRutinaOpo: Int?) -> Unit = { _, _, _ -> },
    onNavigateToPerfil: () -> Unit,
    onNavigateToHistorial: () -> Unit,
    onNavigateToAjustes: () -> Unit,
    onNavigateToInfoOposicion: () -> Unit,
    onNavigateToRutinasLibres: () -> Unit,
    onNavigateToSimulacro: () -> Unit = {},
    onNavigateToRanking: () -> Unit = {},
    onNavigateToComunidad: () -> Unit = {},
    onNavigateToPremium: () -> Unit = {},
    onNavigateToGps: () -> Unit = {},
    onNavigateToMapaRuta: (distKm: Double?, titulo: String?, enfoque: String?) -> Unit = { _, _, _ -> },
    onNavigateToMisDispositivos: () -> Unit = {},
    onLogout: () -> Unit,
    userName: String? = null,
    oposicionId: Int = 1,
    esFitness: Boolean = false,
    homeViewModel: HomeViewModel = viewModel()
) {
    val uiState by homeViewModel.uiState.collectAsState()
    val resumen = uiState.resumen
    val displayName = userName?.trim().orEmpty().ifBlank { if (esFitness) "Atleta" else "Opositor" }
    val compact = isCompactScreen()
    val veryCompact = isVeryCompactScreen()
    val context = LocalContext.current
    var showGpsPermDialog by remember { mutableStateOf(false) }

    val gpsPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        val ok = granted[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (ok) onNavigateToGps() else showGpsPermDialog = true
    }

    fun startGpsFromHome() {
        if (GpsPermissionRequest.hasLocationPermission(context)) {
            onNavigateToGps()
        } else {
            gpsPermLauncher.launch(GpsPermissionRequest.requiredPermissions())
        }
    }

    LaunchedEffect(oposicionId) {
        homeViewModel.cargarResumen(oposicionId)
    }

    LaunchedEffect(Unit) {
        AppEvents.homeRefresh.collect {
            homeViewModel.refresh(oposicionId)
        }
    }

    val quickLinks = buildList {
        add(QuickLink("Plan", "Entreno semanal", Icons.Filled.FitnessCenter, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary, onNavigateToRutinas))
        add(QuickLink("Empezar carrera", "GPS al aire libre", Icons.Filled.Explore, MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.onTertiary, { startGpsFromHome() }))
        if (esFitness) {
            add(QuickLink("Comunidad", "Grupos · Chat", Icons.Filled.Groups, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.onSecondary, onNavigateToComunidad))
            add(QuickLink("Rutinas libres", "Crea la tuya", Icons.Filled.FitnessCenter, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, onNavigateToRutinasLibres))
        } else {
            add(QuickLink("Historial", "Tu actividad", Icons.Filled.BarChart, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.onSecondary, onNavigateToHistorial))
            add(QuickLink("Conexiones", "Reloj · sync", Icons.Filled.Watch, AccentSlate, Color.White, onNavigateToMisDispositivos))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OpoFitLogo(size = 36.dp, onDarkBackground = true)
                        Column {
                            Text("OpoFit", fontWeight = FontWeight.ExtraBold)
                            Text(
                                if (esFitness) "Modo fitness" else (resumen?.oposicionNombre ?: "Tu oposición"),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    scrolledContainerColor = MaterialTheme.colorScheme.primary
                ),
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, "Menú", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToAjustes) {
                        Icon(Icons.Filled.Settings, "Ajustes", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.loading && resumen != null,
            onRefresh = { homeViewModel.refresh(oposicionId) },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                uiState.loading && resumen == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error.isNotBlank() && resumen == null -> {
                    ErrorState(uiState.error, onRetry = { homeViewModel.refresh(oposicionId) })
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = if (compact) 12.dp else 16.dp,
                            top = 0.dp,
                            end = if (compact) 12.dp else 16.dp,
                            bottom = 28.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(20.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                AccentOrange.copy(alpha = 0.18f),
                                                MaterialTheme.colorScheme.background
                                            )
                                        )
                                    )
                            )
                        }

                        item {
                            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                                val avatarSize = if (compact) 52 else 64
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(if (compact) 14.dp else 20.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp)
                                ) {
                                    Box(
                                        Modifier
                                            .width(4.dp)
                                            .height(avatarSize.dp)
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        AccentOrange,
                                                        MaterialTheme.colorScheme.primary
                                                    )
                                                ),
                                                MaterialTheme.shapes.small
                                            )
                                    )
                                    ProfileAvatar(displayName, sizeDp = avatarSize)
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            "Hola, $displayName",
                                            style = if (compact) MaterialTheme.typography.titleMedium
                                            else MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            if (esFitness) {
                                                "Plan personalizado de fuerza y cardio"
                                            } else {
                                                resumen?.nivel?.let { nivelLabel(it) }
                                                    ?: "Completa tu perfil para calcular nivel"
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (!esFitness) {
                                            resumen?.notaMedia?.let { nota ->
                                                Text(
                                                    "Nota media oficial: $nota / 10",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        resumen?.entrenoHoy?.let { hoy ->
                            if (!hoy.completada && !hoy.enfoque.isNullOrBlank()) {
                                item {
                                    EntrenoHoyHeroCard(
                                        titulo = if (hoy.esHoy) "Entreno de hoy" else "Próximo entreno",
                                        subtitulo = "${hoy.nombreDia ?: ""} · ${hoy.titulo ?: enfoqueLabel(hoy.enfoque!!)}",
                                        enfoque = hoy.enfoque!!,
                                        onEmpezar = {
                                            onNavigateToEntrenamientos(
                                                hoy.enfoque,
                                                hoy.id_plan_dia,
                                                hoy.id_rutina_opo
                                            )
                                        },
                                        onPrepararRuta = {
                                            val km = MapaEntrenoNav.distanciaKmDesdeTexto(
                                                "${hoy.titulo.orEmpty()} ${hoy.descripcion.orEmpty()}"
                                            )
                                            onNavigateToMapaRuta(
                                                km,
                                                hoy.titulo ?: hoy.nombreDia,
                                                hoy.enfoque
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        item {
                            WeekActivityChart(dias = resumen?.graficaSemanal.orEmpty())
                        }

                        item {
                            SectionHeader(
                                title = "Tu semana",
                                subtitle = "Resumen rápido"
                            )
                            Spacer(Modifier.height(8.dp))
                            if (compact) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        StatCard(
                                            label = "Sesiones",
                                            value = "${resumen?.sesionesSemana ?: 0}",
                                            supporting = if (veryCompact) "7 días" else "últimos 7 días",
                                            icon = Icons.Filled.FitnessCenter,
                                            accentColor = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        // "Minutos" pasa a "Tiempo" para alojar
                                        // valores grandes (horas / días) sin romper
                                        // la card. El formatter elige la unidad.
                                        StatCard(
                                            label = "Tiempo",
                                            value = com.opofit.miapp.utils.TimeFormatUtil.formatDuracionLegible(
                                                resumen?.minutosSemana ?: 0
                                            ),
                                            supporting = "entrenando",
                                            icon = Icons.Filled.Timer,
                                            accentColor = AccentTeal,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    StatCard(
                                        label = "Racha",
                                        value = "${resumen?.rachaDias ?: 0}",
                                        supporting = if ((resumen?.rachaDias ?: 0) > 0) {
                                            if (veryCompact) "días" else "días seguidos"
                                        } else "¡entrena hoy para empezar!",
                                        icon = Icons.Filled.LocalFireDepartment,
                                        accentColor = AccentOrange,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            } else {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    StatCard(
                                        label = "Sesiones",
                                        value = "${resumen?.sesionesSemana ?: 0}",
                                        supporting = "últimos 7 días",
                                        icon = Icons.Filled.FitnessCenter,
                                        accentColor = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatCard(
                                        label = "Tiempo",
                                        value = com.opofit.miapp.utils.TimeFormatUtil.formatDuracionLegible(
                                            resumen?.minutosSemana ?: 0
                                        ),
                                        supporting = "entrenando",
                                        icon = Icons.Filled.Timer,
                                        accentColor = AccentTeal,
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatCard(
                                        label = "Racha",
                                        value = "${resumen?.rachaDias ?: 0}",
                                        supporting = if ((resumen?.rachaDias ?: 0) > 0) "días seguidos" else "¡entrena hoy para empezar!",
                                        icon = Icons.Filled.LocalFireDepartment,
                                        accentColor = AccentOrange,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        if (!esFitness) {
                            item {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    StatCard(
                                        label = "Ranking",
                                        value = resumen?.rankingPosicion?.let { "#$it" } ?: "—",
                                        supporting = resumen?.rankingTotal?.let { "de $it opositores" },
                                        icon = Icons.Filled.Leaderboard,
                                        accentColor = AccentIndigo,
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatCard(
                                        label = "Simulacro",
                                        value = resumen?.ultimoSimulacro?.notaMedia?.let { "$it" } ?: "—",
                                        supporting = "toca para repetir",
                                        icon = Icons.Filled.TrendingUp,
                                        accentColor = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.weight(1f),
                                        onClick = onNavigateToSimulacro
                                    )
                                }
                            }

                            if (uiState.noticiasRss.isNotEmpty()) {
                                item {
                                    ElevatedCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(onClick = onNavigateToInfoOposicion)
                                    ) {
                                        Column(
                                            Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        Icons.Filled.Info,
                                                        null,
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        "Tu oposición",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                TextButton(onClick = onNavigateToInfoOposicion) {
                                                    Text("Ver todo")
                                                }
                                            }
                                            uiState.noticiasRss.forEach { noticia ->
                                                val context = androidx.compose.ui.platform.LocalContext.current
                                                Row(
                                                    verticalAlignment = Alignment.Top,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    // Click directo: abre el enlace si lo trae;
                                                    // si no, lleva a la pantalla de Info.
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            if (noticia.enlace.isNotBlank()) {
                                                                com.opofit.miapp.utils.UrlOpener.open(context, noticia.enlace)
                                                            } else {
                                                                onNavigateToInfoOposicion()
                                                            }
                                                        }
                                                        .padding(vertical = 2.dp)
                                                ) {
                                                    if (noticia.urgente) {
                                                        AssistChip(
                                                            onClick = {},
                                                            label = { Text("Urgente", style = MaterialTheme.typography.labelSmall) },
                                                            enabled = false
                                                        )
                                                    }
                                                    Text(
                                                        noticia.titulo,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }
                                            Text(
                                                "Noticias oficiales y baremos de prueba",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val h = resumen?.entrenoHoy
                                        onNavigateToEntrenamientos(h?.enfoque, h?.id_plan_dia, h?.id_rutina_opo)
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(Icons.Filled.PlayArrow, null, Modifier.size(20.dp))
                                    Text("Entrenar", modifier = Modifier.padding(start = 8.dp))
                                }
                                if (esFitness) {
                                    Button(
                                        onClick = onNavigateToComunidad,
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondary
                                        )
                                    ) {
                                        Icon(Icons.Filled.Groups, null, Modifier.size(20.dp))
                                        Text("Comunidad", modifier = Modifier.padding(start = 8.dp))
                                    }
                                } else {
                                    Button(
                                        onClick = onNavigateToRanking,
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondary
                                        )
                                    ) {
                                        Icon(Icons.Filled.Leaderboard, null, Modifier.size(20.dp))
                                        Text("Ranking", modifier = Modifier.padding(start = 8.dp))
                                    }
                                }
                            }
                        }

                        val feed = uiState.feedAmigos
                        if (feed.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = "Actividad de amigos",
                                    subtitle = "Feed estilo Strava"
                                )
                            }
                            items(feed.size) { i ->
                                val f = feed[i]
                                ElevatedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = onNavigateToComunidad
                                ) {
                                    Row(
                                        Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ProfileAvatar(f.usuarioNombre ?: "?", sizeDp = 36)
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                f.usuarioNombre ?: "Opositor",
                                                fontWeight = FontWeight.SemiBold,
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                            Text(
                                                f.detalle ?: "",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            if (f.tipo == "SIMULACRO") "🎯" else "💪",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }
                            }
                            item {
                                TextButton(onClick = onNavigateToComunidad, modifier = Modifier.fillMaxWidth()) {
                                    Text("Ver todo en Comunidad")
                                }
                            }
                        }

                        item {
                            SectionHeader(title = "Explorar", subtitle = "Todo tu preparador en un sitio")
                        }

                        item {
                            val rows = quickLinks.chunked(2)
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                rows.forEach { row ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        row.forEach { link ->
                                            NavCard(
                                                icon = link.icon,
                                                title = link.title,
                                                subtitle = link.subtitle,
                                                containerColor = link.containerColor,
                                                contentColor = link.contentColor,
                                                onClick = link.onClick,
                                                compact = compact,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        if (row.size == 1) {
                                            Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }

                        resumen?.ultimaSesion?.let { sesion ->
                            item {
                                ElevatedCard {
                                    Column(Modifier.padding(14.dp)) {
                                        Text(
                                            "Última actividad",
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            "${sesion.tipo ?: "Entreno"} · ${sesion.duracionMin} min",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        item { Spacer(Modifier.height(8.dp)) }
                    }
                }
            }
        }
    }

    // Coach marks la primera vez: 3 burbujas explicando lo esencial.
    // Se guarda en SharedPrefs y nunca más vuelven a aparecer (a menos que el
    // usuario resetee desde Ajustes → "Ver tutorial otra vez").
    CoachMarkOverlay(
        screenKey = if (esFitness) "home_fitness_v1" else "home_opo_v1",
        steps = if (esFitness) listOf(
            CoachStep(
                title = "Hola, atleta 👋",
                text = "Aquí ves tu progreso: tu última actividad, tu plan y accesos rápidos. Tira hacia abajo para refrescar."
            ),
            CoachStep(
                title = "Empieza una actividad",
                text = "Pulsa «Empezar carrera» para abrir el GPS y registrar carrera, paseo o bici con mapa en vivo, ritmo y pulso."
            ),
            CoachStep(
                title = "Comunidad y rutinas",
                text = "Crea tu rutina personalizada o entra a la comunidad: chat con amigos, grupos por intereses y feed de actividades."
            )
        ) else listOf(
            CoachStep(
                title = "Hola, opositor 👋",
                text = "Esta es tu pantalla principal. Aquí verás tu plan de hoy, tu progreso y accesos a lo importante."
            ),
            CoachStep(
                title = "Tu plan se adapta a ti",
                text = "Cuanto más entrenes y más marcas registres en tu perfil, mejor se ajusta el plan a tus puntos débiles. La sesión de hoy aparece arriba."
            ),
            CoachStep(
                title = "Simulacro y ranking",
                text = "Practica con un simulacro oficial cuando estés listo. Tu nota aparece en el ranking de tu oposición (puedes desactivarlo en Ajustes)."
            )
        )
    )

    if (showGpsPermDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showGpsPermDialog = false },
            title = { Text("Permiso de ubicación") },
            text = {
                Text(
                    "OpoFit necesita acceso a tu ubicación (y notificaciones en Android 13+) " +
                        "para grabar la carrera con GPS. Actívalos en Ajustes del teléfono."
                )
            },
            confirmButton = {
                TextButton(onClick = { showGpsPermDialog = false }) { Text("Entendido") }
            }
        )
    }
    }
}

private fun nivelLabel(nivel: String): String = when (nivel.uppercase()) {
    "AVANZADO" -> "Nivel avanzado"
    "INTERMEDIO" -> "Nivel intermedio"
    "BASICO" -> "Nivel básico"
    "INCOMPLETO" -> "Perfil incompleto — registra todas las pruebas"
    else -> nivel
}

@Composable
private fun NavCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        onClick = onClick,
        containerColor = containerColor
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(if (compact) 10.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp)
        ) {
            Icon(
                icon,
                null,
                tint = contentColor,
                modifier = Modifier.size(if (compact) 22.dp else 28.dp)
            )
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
