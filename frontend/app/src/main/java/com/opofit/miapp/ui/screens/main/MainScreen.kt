package com.opofit.miapp.ui.screens.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.opofit.miapp.ui.components.ProfileAvatar
import com.opofit.miapp.ui.screens.historial.HistorialScreen
import com.opofit.miapp.ui.screens.home.HomeScreen
import com.opofit.miapp.ui.screens.perfil.PerfilScreen
import com.opofit.miapp.ui.screens.rutinas.RutinasScreen
import com.opofit.miapp.ui.viewmodels.AuthViewModel
import com.opofit.miapp.utils.FitnessMode
import kotlinx.coroutines.launch

private sealed class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    object Inicio : BottomTab("tab_home", "Inicio", Icons.Filled.Home)
    object Rutinas : BottomTab("tab_rutinas", "Plan", Icons.Filled.FitnessCenter)
    object Perfil : BottomTab("tab_perfil", "Perfil", Icons.Filled.Person)
    object Historial : BottomTab("tab_historial", "Actividad", Icons.Filled.BarChart)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    authViewModel: AuthViewModel,
    onNavigateToEntrenamientos: (enfoque: String?, idPlanDia: Int?, idRutinaOpo: Int?) -> Unit,
    onNavigateToAjustes: () -> Unit,
    onNavigateToInfoOposicion: () -> Unit,
    onNavigateToRutinasLibres: () -> Unit,
    onNavigateToCrearRutina: () -> Unit,
    onNavigateToDetallesRutina: (Int) -> Unit,
    onNavigateToEditarPerfil: () -> Unit,
    onNavigateToSimulacro: () -> Unit,
    onNavigateToRanking: () -> Unit,
    onNavigateToComunidad: () -> Unit,
    onNavigateToPremium: () -> Unit,
    onNavigateToGps: () -> Unit,
    onNavigateToMapaRuta: (distKm: Double?, titulo: String?, enfoque: String?) -> Unit = { _, _, _ -> },
    onNavigateToLugaresEntreno: (tipo: String) -> Unit = {},
    onNavigateToSesionDetalle: (Int) -> Unit,
    onNavigateToEjercicioHistorial: (Int) -> Unit,
    onNavigateToPlanHistorial: (Int) -> Unit,
    onNavigateToMisDispositivos: () -> Unit,
    onNavigateToPost: (Int) -> Unit = {},
    onLogout: () -> Unit
) {
    val innerNavController = rememberNavController()
    val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val authState by authViewModel.uiState.collectAsState()
    val esFitness = FitnessMode.isFitness(authState.modoUso)
    val planOposicionId = FitnessMode.planOposicionId(authState.oposicionId, authState.modoUso)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val tabs = listOf(
        BottomTab.Inicio,
        BottomTab.Rutinas,
        BottomTab.Perfil,
        BottomTab.Historial
    )

    fun navigateToTab(route: String) {
        innerNavController.navigate(route) {
            popUpTo(innerNavController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
        scope.launch { drawerState.close() }
    }

    fun runAndClose(action: () -> Unit) {
        scope.launch { drawerState.close() }
        action()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ProfileAvatar(authState.userName.orEmpty().ifBlank { "Opositor" }, sizeDp = 48)
                        Column {
                            Text(
                                authState.userName.orEmpty().ifBlank { "Opositor" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (esFitness) "Modo fitness" else "Cuenta OpoFit",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))

                    DrawerSectionLabel("Navegación")
                    NavigationDrawerItem(
                        label = { Text("Inicio") },
                        icon = { Icon(Icons.Filled.Home, null) },
                        selected = false,
                        onClick = { navigateToTab(BottomTab.Inicio.route) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        label = { Text("Plan de entrenamiento") },
                        icon = { Icon(Icons.Filled.FitnessCenter, null) },
                        selected = false,
                        onClick = { navigateToTab(BottomTab.Rutinas.route) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        label = { Text("Dónde entrenar") },
                        icon = { Icon(Icons.Filled.Explore, null) },
                        selected = false,
                        onClick = { runAndClose { onNavigateToLugaresEntreno("GYM") } },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        label = { Text("Actividad e historial") },
                        icon = { Icon(Icons.Filled.BarChart, null) },
                        selected = false,
                        onClick = { navigateToTab(BottomTab.Historial.route) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        label = { Text("Perfil y marcas") },
                        icon = { Icon(Icons.Filled.Person, null) },
                        selected = false,
                        onClick = { navigateToTab(BottomTab.Perfil.route) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    DrawerSectionLabel("Entrenamiento libre")
                    NavigationDrawerItem(
                        label = { Text("Rutinas libres") },
                        icon = { Icon(Icons.Filled.Bookmark, null) },
                        selected = false,
                        onClick = { runAndClose(onNavigateToRutinasLibres) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        label = { Text("Rutas GPS") },
                        icon = { Icon(Icons.AutoMirrored.Filled.DirectionsRun, null) },
                        selected = false,
                        onClick = { runAndClose(onNavigateToGps) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    if (!esFitness) {
                        NavigationDrawerItem(
                            label = { Text("Simulacro") },
                            icon = { Icon(Icons.Filled.Timer, null) },
                            selected = false,
                            onClick = { runAndClose(onNavigateToSimulacro) },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }

                    DrawerSectionLabel(if (esFitness) "Social" else "Social y oposición")
                    if (!esFitness) {
                        NavigationDrawerItem(
                            label = { Text("Info oposición") },
                            icon = { Icon(Icons.Filled.Info, null) },
                            selected = false,
                            onClick = { runAndClose(onNavigateToInfoOposicion) },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                        NavigationDrawerItem(
                            label = { Text("Ranking") },
                            icon = { Icon(Icons.Filled.Leaderboard, null) },
                            selected = false,
                            onClick = { runAndClose(onNavigateToRanking) },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                    NavigationDrawerItem(
                        label = { Text("Comunidad") },
                        icon = { Icon(Icons.Filled.Groups, null) },
                        selected = false,
                        onClick = { runAndClose(onNavigateToComunidad) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    DrawerSectionLabel("Conexión")
                    NavigationDrawerItem(
                        label = { Text("Mis dispositivos") },
                        icon = { Icon(Icons.Filled.Watch, null) },
                        selected = false,
                        onClick = { runAndClose(onNavigateToMisDispositivos) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        label = { Text("OpoFit Premium") },
                        icon = { Icon(Icons.Filled.Star, null) },
                        selected = false,
                        onClick = { runAndClose(onNavigateToPremium) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    DrawerSectionLabel("Cuenta")
                    NavigationDrawerItem(
                        label = { Text("Ajustes") },
                        icon = { Icon(Icons.Filled.Settings, null) },
                        selected = false,
                        onClick = { runAndClose(onNavigateToAjustes) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        label = { Text("Cerrar sesión") },
                        icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, null) },
                        selected = false,
                        onClick = { runAndClose(onLogout) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    ) {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    tabs.forEach { tab ->
                        val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigateToTab(tab.route) },
                            icon = { Icon(imageVector = tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            alwaysShowLabel = selected
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = innerNavController,
                startDestination = BottomTab.Inicio.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(BottomTab.Inicio.route) {
                    HomeScreen(
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onNavigateToRutinas = { navigateToTab(BottomTab.Rutinas.route) },
                        onNavigateToEntrenamientos = { e, p, r -> onNavigateToEntrenamientos(e, p, r) },
                        onNavigateToPerfil = { navigateToTab(BottomTab.Perfil.route) },
                        onNavigateToHistorial = { navigateToTab(BottomTab.Historial.route) },
                        onNavigateToAjustes = onNavigateToAjustes,
                        onNavigateToInfoOposicion = onNavigateToInfoOposicion,
                        onNavigateToRutinasLibres = onNavigateToRutinasLibres,
                        onNavigateToSimulacro = onNavigateToSimulacro,
                        onNavigateToRanking = onNavigateToRanking,
                        onNavigateToComunidad = onNavigateToComunidad,
                        onNavigateToPremium = onNavigateToPremium,
                        onNavigateToGps = onNavigateToGps,
                        onNavigateToMapaRuta = onNavigateToMapaRuta,
                        onNavigateToMisDispositivos = onNavigateToMisDispositivos,
                        onLogout = onLogout,
                        userName = authState.userName,
                        oposicionId = planOposicionId,
                        esFitness = esFitness
                    )
                }

                composable(BottomTab.Rutinas.route) {
                    RutinasScreen(
                        authViewModel = authViewModel,
                        onNavigateBack = { navigateToTab(BottomTab.Inicio.route) },
                        onNavigateToEntrenamientos = { e, p, r -> onNavigateToEntrenamientos(e, p, r) },
                        onNavigateToRutinasLibres = onNavigateToRutinasLibres,
                        onNavigateToEditarPerfil = onNavigateToEditarPerfil,
                        onNavigateToPlanHistorial = onNavigateToPlanHistorial,
                        onNavigateToLugaresEntreno = onNavigateToLugaresEntreno
                    )
                }

                composable(BottomTab.Perfil.route) {
                    PerfilScreen(
                        authViewModel = authViewModel,
                        onNavigateBack = { navigateToTab(BottomTab.Inicio.route) },
                        onNavigateToEditarPerfil = onNavigateToEditarPerfil,
                        onNavigateToAjustes = onNavigateToAjustes,
                        onNavigateToComunidad = onNavigateToComunidad,
                        onOpenPost = onNavigateToPost
                    )
                }

                composable(BottomTab.Historial.route) {
                    HistorialScreen(
                        authViewModel = authViewModel,
                        onNavigateBack = { navigateToTab(BottomTab.Inicio.route) },
                        onOpenSesion = onNavigateToSesionDetalle,
                        onOpenEjercicio = onNavigateToEjercicioHistorial,
                        onOpenPlan = onNavigateToPlanHistorial
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 28.dp, top = 16.dp, bottom = 4.dp)
    )
}
