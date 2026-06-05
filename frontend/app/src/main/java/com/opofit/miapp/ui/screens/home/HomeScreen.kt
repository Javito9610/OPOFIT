package com.opofit.miapp.ui.screens.home

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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

    LaunchedEffect(oposicionId) {
        homeViewModel.cargarResumen(oposicionId)
    }

    val quickLinks = buildList {
        add(QuickLink("Plan", "Entreno semanal", Icons.Filled.FitnessCenter, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary, onNavigateToRutinas))
        add(QuickLink("Rutas GPS", "Carrera · Bici", Icons.Filled.Explore, MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.onTertiary, onNavigateToGps))
        if (esFitness) {
            add(QuickLink("Comunidad", "Grupos · Chat", Icons.Filled.Groups, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.onSecondary, onNavigateToComunidad))
            add(QuickLink("Rutinas libres", "Crea la tuya", Icons.Filled.FitnessCenter, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, onNavigateToRutinasLibres))
        } else {
            add(QuickLink("Simulacro", "Pruebas", Icons.Filled.Timer, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.onSecondary, onNavigateToSimulacro))
            add(QuickLink("Dispositivos", "Reloj · banda", Icons.Filled.Watch, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, onNavigateToMisDispositivos))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OpoFitLogo(size = 36.dp, cornerRadius = 8.dp)
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
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
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
                        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.extraLarge,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    ProfileAvatar(displayName, sizeDp = 64)
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            "Hola, $displayName",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            if (esFitness) {
                                                "Plan personalizado de fuerza y cardio"
                                            } else {
                                                resumen?.nivel?.let { nivelLabel(it) }
                                                    ?: "Completa tu perfil para calcular nivel"
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                        )
                                        if (!esFitness) {
                                            resumen?.notaMedia?.let { nota ->
                                                Text(
                                                    "Nota media oficial: $nota / 10",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.primary
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
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                StatCard(
                                    label = "Sesiones",
                                    value = "${resumen?.sesionesSemana ?: 0}",
                                    supporting = "últimos 7 días",
                                    icon = Icons.Filled.FitnessCenter,
                                    modifier = Modifier.weight(1f)
                                )
                                StatCard(
                                    label = "Minutos",
                                    value = "${resumen?.minutosSemana ?: 0}",
                                    supporting = "entrenando",
                                    icon = Icons.Filled.Timer,
                                    modifier = Modifier.weight(1f)
                                )
                                StatCard(
                                    label = "Racha",
                                    value = "${resumen?.rachaDias ?: 0}",
                                    supporting = if ((resumen?.rachaDias ?: 0) > 0) "días seguidos" else "sin racha",
                                    icon = Icons.Filled.LocalFireDepartment,
                                    modifier = Modifier.weight(1f)
                                )
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
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatCard(
                                        label = "Simulacro",
                                        value = resumen?.ultimoSimulacro?.notaMedia?.let { "$it" } ?: "—",
                                        supporting = "última nota /10",
                                        icon = Icons.Filled.TrendingUp,
                                        modifier = Modifier.weight(1f)
                                    )
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
                                        onClick = onNavigateToSimulacro,
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondary
                                        )
                                    ) {
                                        Icon(Icons.Filled.Timer, null, Modifier.size(20.dp))
                                        Text("Simulacro", modifier = Modifier.padding(start = 8.dp))
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
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = onNavigateToComunidad,
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    )
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
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    )
                                ) {
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
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, null, tint = contentColor, modifier = Modifier.size(28.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.85f))
        }
    }
}
