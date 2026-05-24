package com.opofit.miapp.ui.screens.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.opofit.miapp.ui.screens.historial.HistorialScreen
import com.opofit.miapp.ui.screens.home.HomeScreen
import com.opofit.miapp.ui.screens.perfil.PerfilScreen
import com.opofit.miapp.ui.screens.rutinas.RutinasScreen
import com.opofit.miapp.ui.viewmodels.AuthViewModel

private sealed class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    object Inicio : BottomTab("tab_home", "Inicio", Icons.Filled.Home)
    object Rutinas : BottomTab("tab_rutinas", "Rutinas", Icons.Filled.FitnessCenter)
    object Perfil : BottomTab("tab_perfil", "Perfil", Icons.Filled.Person)
    object Historial : BottomTab("tab_historial", "Historial", Icons.Filled.BarChart)
}

@Composable
fun MainScreen(
    authViewModel: AuthViewModel,
    onNavigateToEntrenamientos: (String?) -> Unit,
    onNavigateToAjustes: () -> Unit,
    onNavigateToInfoOposicion: () -> Unit,
    onNavigateToRutinasLibres: () -> Unit,
    onNavigateToCrearRutina: () -> Unit,
    onNavigateToDetallesRutina: (Int) -> Unit,
    onNavigateToEditarPerfil: () -> Unit,
    onNavigateToSimulacro: () -> Unit,
    onNavigateToRanking: () -> Unit,
    onNavigateToPremium: () -> Unit,
    onLogout: () -> Unit
) {
    val innerNavController = rememberNavController()
    val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val authState by authViewModel.uiState.collectAsState()

    val tabs = listOf(
        BottomTab.Inicio,
        BottomTab.Rutinas,
        BottomTab.Perfil,
        BottomTab.Historial
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true,
                        onClick = {
                            innerNavController.navigate(tab.route) {
                                popUpTo(innerNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(imageVector = tab.icon, contentDescription = tab.label)
                        },
                        label = { Text(tab.label) }
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
                    onNavigateToRutinas = {
                        innerNavController.navigate(BottomTab.Rutinas.route) {
                            popUpTo(innerNavController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }
                    },
                    onNavigateToEntrenamientos = { onNavigateToEntrenamientos(null) },
                    onNavigateToPerfil = {
                        innerNavController.navigate(BottomTab.Perfil.route) {
                            popUpTo(innerNavController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }
                    },
                    onNavigateToHistorial = {
                        innerNavController.navigate(BottomTab.Historial.route) {
                            popUpTo(innerNavController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }
                    },
                    onNavigateToAjustes = onNavigateToAjustes,
                    onNavigateToInfoOposicion = onNavigateToInfoOposicion,
                    onNavigateToRutinasLibres = onNavigateToRutinasLibres,
                    onNavigateToSimulacro = onNavigateToSimulacro,
                    onNavigateToRanking = onNavigateToRanking,
                    onNavigateToPremium = onNavigateToPremium,
                    onLogout = onLogout,
                    userName = authState.userName
                )
            }

            composable(BottomTab.Rutinas.route) {
                RutinasScreen(
                    authViewModel = authViewModel,
                    onNavigateBack = {
                        innerNavController.navigate(BottomTab.Inicio.route) {
                            popUpTo(innerNavController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }
                    },
                    onNavigateToEntrenamientos = onNavigateToEntrenamientos,
                    onNavigateToRutinasLibres = onNavigateToRutinasLibres,
                    onNavigateToEditarPerfil = onNavigateToEditarPerfil
                )
            }

            composable(BottomTab.Perfil.route) {
                PerfilScreen(
                    authViewModel = authViewModel,
                    onNavigateBack = {
                        innerNavController.navigate(BottomTab.Inicio.route) {
                            popUpTo(innerNavController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }
                    },
                    onNavigateToEditarPerfil = onNavigateToEditarPerfil
                )
            }

            composable(BottomTab.Historial.route) {
                HistorialScreen(
                    authViewModel = authViewModel,
                    onNavigateBack = {
                        innerNavController.navigate(BottomTab.Inicio.route) {
                            popUpTo(innerNavController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }
                    }
                )
            }
        }
    }
}
