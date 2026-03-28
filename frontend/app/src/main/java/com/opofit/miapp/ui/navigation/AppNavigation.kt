package com.opofit.miapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.opofit.miapp.ui.screens.auth.LoginScreen
import com.opofit.miapp.ui.screens.auth.RegisterScreen
import com.opofit.miapp.ui.screens.home.HomeScreen
import com.opofit.miapp.ui.viewmodels.AuthViewModel


/**
 * ============ NAVEGACIÓN PRINCIPAL ============
 * Gestiona todas las rutas y transiciones de la app
 * 13 pantallas en total
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    isLoggedIn: Boolean=false
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

        // ============ 3. HOGAR (PANTALLA PRINCIPAL) ============
        composable(NavDestinations.HOME) {
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
                }
            )
        }

        // ============ 4. RUTINAS ============
        composable(NavDestinations.RUTINAS) {
            // TODO: RutinasScreen
        }

        // ============ 4.1 RUTINAS POR NIVEL ============
        composable(NavDestinations.RUTINAS_POR_NIVEL) { backStackEntry ->
            val nivel = backStackEntry.arguments?.getString("nivel") ?: "1"
            // TODO: RutinasPorNivelScreen(nivel)
        }

        // ============ 5. CREAR/EDITAR RUTINA ============
        composable(NavDestinations.CREAR_EDITAR_RUTINA) {
            // TODO: CrearEditarRutinaScreen
        }

        // ============ 12. DETALLES RUTINA ============
        composable(NavDestinations.DETALLES_RUTINA) { backStackEntry ->
            val rutinaId = backStackEntry.arguments?.getString("rutina_id") ?: ""
            // TODO: DetallesRutinaScreen(rutinaId)
        }

        // ============ 13. RUTINAS LIBRES ============
        composable(NavDestinations.RUTINAS_LIBRES) {
            // TODO: RutinasLibresScreen
        }

        // ============ 6. ENTRENAMIENTOS ============
        composable(NavDestinations.ENTRENAMIENTOS) {
            // TODO: EntrenamientosScreen
        }

        // ============ 7. REGISTRAR ENTRENAMIENTO ============
        composable(NavDestinations.REGISTRAR_ENTRENAMIENTO) {
            // TODO: RegistrarEntrenamientoScreen
        }

        // ============ 12. DETALLES EJERCICIO ============
        composable(NavDestinations.DETALLES_EJERCICIO) { backStackEntry ->
            val ejercicioId = backStackEntry.arguments?.getString("ejercicio_id") ?: ""
            // TODO: DetallesEjercicioScreen(ejercicioId)
        }

        // ============ 8. PERFIL ============
        composable(NavDestinations.PERFIL) {
            // TODO: PerfilScreen
        }

        // ============ 9. EDITAR PERFIL ============
        composable(NavDestinations.EDITAR_PERFIL) {
            // TODO: EditarPerfilScreen
        }

        // ============ 11. HISTORIAL ============
        composable(NavDestinations.HISTORIAL) {
            // TODO: HistorialScreen
        }

        // ============ 10. AJUSTES ============
        composable(NavDestinations.AJUSTES) {
            // TODO: AjustesScreen
        }
    }
}