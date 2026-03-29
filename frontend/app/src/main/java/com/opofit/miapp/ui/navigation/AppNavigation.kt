package com.opofit.miapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.opofit.miapp.ui.screens.ajustes.AjustesScreen
import com.opofit.miapp.ui.screens.auth.LoginScreen
import com.opofit.miapp.ui.screens.auth.RegisterScreen
import com.opofit.miapp.ui.screens.entrenamientos.EntrenamientosScreen
import com.opofit.miapp.ui.screens.entrenamientos.RegistrarEntrenamientoScreen
import com.opofit.miapp.ui.screens.historial.HistorialScreen
import com.opofit.miapp.ui.screens.home.HomeScreen
import com.opofit.miapp.ui.screens.perfil.EditarPerfilScreen
import com.opofit.miapp.ui.screens.perfil.PerfilScreen
import com.opofit.miapp.ui.screens.rutinas.CrearRutinaScreen
import com.opofit.miapp.ui.screens.rutinas.DetallesEjercicioScreen
import com.opofit.miapp.ui.screens.rutinas.DetallesRutinaScreen
import com.opofit.miapp.ui.screens.rutinas.RutinasLibresScreen
import com.opofit.miapp.ui.screens.rutinas.RutinasScreen
import com.opofit.miapp.ui.viewmodels.AuthViewModel


/**
 * ============ NAVEGACIÓN PRINCIPAL ============
 * Gestiona todas las rutas y transiciones de la app
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    isLoggedIn: Boolean = false
) {
    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) NavDestinations.HOME else NavDestinations.LOGIN
    ) {

        // ============ 1. LOGIN ============
        composable(NavDestinations.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(NavDestinations.HOME) {
                        popUpTo(NavDestinations.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(NavDestinations.REGISTRO)
                },
                viewModel = authViewModel
            )
        }

        // ============ 2. REGISTRO ============
        composable(NavDestinations.REGISTRO) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(NavDestinations.HOME) {
                        popUpTo(NavDestinations.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                viewModel = authViewModel
            )
        }

        // ============ 3. HOME ============
        composable(NavDestinations.HOME) {
            val authState = authViewModel.uiState.collectAsState()
            HomeScreen(
                onNavigateToRutinas = {
                    navController.navigate(NavDestinations.RUTINAS)
                },
                onNavigateToEntrenamientos = {
                    navController.navigate(NavDestinations.ENTRENAMIENTOS)
                },
                onNavigateToPerfil = {
                    navController.navigate(NavDestinations.PERFIL)
                },
                onNavigateToHistorial = {
                    navController.navigate(NavDestinations.HISTORIAL)
                },
                onNavigateToAjustes = {
                    navController.navigate(NavDestinations.AJUSTES)
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(NavDestinations.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                userName = authState.value.userName
            )
        }

        // ============ 4. RUTINAS ============
        composable(NavDestinations.RUTINAS) {
            RutinasScreen(
                authViewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEntrenamientos = { navController.navigate(NavDestinations.ENTRENAMIENTOS) },
                onNavigateToRutinasLibres = { navController.navigate(NavDestinations.RUTINAS_LIBRES) }
            )
        }

        // ============ 4.1 RUTINAS POR NIVEL ============
        composable(NavDestinations.RUTINAS_POR_NIVEL) { backStackEntry ->
            val nivel = backStackEntry.arguments?.getString("nivel") ?: "1"
            RutinasScreen(
                authViewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEntrenamientos = { navController.navigate(NavDestinations.ENTRENAMIENTOS) },
                onNavigateToRutinasLibres = { navController.navigate(NavDestinations.RUTINAS_LIBRES) }
            )
        }

        // ============ 5. CREAR RUTINA ============
        composable(NavDestinations.CREAR_RUTINA) {
            CrearRutinaScreen(
                authViewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NavDestinations.CREAR_EDITAR_RUTINA) {
            CrearRutinaScreen(
                authViewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ============ 6. DETALLES RUTINA ============
        composable(
            route = NavDestinations.DETALLES_RUTINA,
            arguments = listOf(navArgument("rutina_id") { type = NavType.StringType })
        ) { backStackEntry ->
            val rutinaIdStr = backStackEntry.arguments?.getString("rutina_id") ?: "0"
            val rutinaId = rutinaIdStr.toIntOrNull() ?: 0
            DetallesRutinaScreen(
                rutinaId = rutinaId,
                authViewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() },
                onIniciarEntrenamiento = { navController.navigate(NavDestinations.ENTRENAMIENTOS) }
            )
        }

        // ============ 7. RUTINAS LIBRES ============
        composable(NavDestinations.RUTINAS_LIBRES) {
            RutinasLibresScreen(
                authViewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCrearRutina = { navController.navigate(NavDestinations.CREAR_RUTINA) },
                onNavigateToDetallesRutina = { id ->
                    navController.navigate("detalles_rutina/$id")
                }
            )
        }

        // ============ 8. ENTRENAMIENTOS ============
        composable(NavDestinations.ENTRENAMIENTOS) {
            EntrenamientosScreen(
                authViewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() },
                onEntrenamientoFinalizado = {
                    navController.navigate(NavDestinations.HISTORIAL) {
                        popUpTo(NavDestinations.ENTRENAMIENTOS) { inclusive = true }
                    }
                }
            )
        }

        // ============ 9. REGISTRAR ENTRENAMIENTO ============
        composable(NavDestinations.REGISTRAR_ENTRENAMIENTO) {
            RegistrarEntrenamientoScreen(
                authViewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() },
                onRegistrado = { navController.popBackStack() }
            )
        }

        // ============ 10. DETALLES EJERCICIO ============
        composable(
            route = NavDestinations.DETALLES_EJERCICIO,
            arguments = listOf(navArgument("ejercicio_id") { type = NavType.StringType })
        ) { backStackEntry ->
            val ejercicioId = backStackEntry.arguments?.getString("ejercicio_id") ?: ""
            DetallesEjercicioScreen(
                ejercicioNombre = ejercicioId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ============ 11. PERFIL ============
        composable(NavDestinations.PERFIL) {
            PerfilScreen(
                authViewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditarPerfil = { navController.navigate(NavDestinations.EDITAR_PERFIL) }
            )
        }

        // ============ 12. EDITAR PERFIL ============
        composable(NavDestinations.EDITAR_PERFIL) {
            EditarPerfilScreen(
                authViewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ============ 13. HISTORIAL ============
        composable(NavDestinations.HISTORIAL) {
            HistorialScreen(
                authViewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ============ 14. AJUSTES ============
        composable(NavDestinations.AJUSTES) {
            AjustesScreen(
                authViewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(NavDestinations.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}