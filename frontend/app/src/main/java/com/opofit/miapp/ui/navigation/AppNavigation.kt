package com.opofit.miapp.ui.navigation

import androidx.compose.runtime.Composable
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
import com.opofit.miapp.ui.screens.main.MainScreen
import com.opofit.miapp.ui.screens.oposicion.OposicionInfoScreen
import com.opofit.miapp.ui.screens.perfil.EditarPerfilScreen
import com.opofit.miapp.ui.screens.rutinas.CrearRutinaScreen
import com.opofit.miapp.ui.screens.rutinas.DetallesEjercicioScreen
import com.opofit.miapp.ui.screens.rutinas.DetallesRutinaScreen
import com.opofit.miapp.ui.screens.rutinas.RutinasLibresScreen
import com.opofit.miapp.ui.viewmodels.AuthViewModel

@Composable
fun AppNavigation(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    isLoggedIn: Boolean = false
) {
    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) NavDestinations.MAIN else NavDestinations.LOGIN
    ) {

        composable(NavDestinations.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(NavDestinations.MAIN) {
                        popUpTo(NavDestinations.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(NavDestinations.REGISTRO)
                },
                viewModel = authViewModel
            )
        }

        composable(NavDestinations.REGISTRO) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(NavDestinations.MAIN) {
                        popUpTo(NavDestinations.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                viewModel = authViewModel
            )
        }

        // Main app screen with BottomNavigation (Home, Rutinas, Perfil, Historial)
        composable(NavDestinations.MAIN) {
            MainScreen(
                authViewModel = authViewModel,
                onNavigateToEntrenamientos = {
                    navController.navigate(NavDestinations.ENTRENAMIENTOS)
                },
                onNavigateToAjustes = {
                    navController.navigate(NavDestinations.AJUSTES)
                },
                onNavigateToInfoOposicion = {
                    navController.navigate(NavDestinations.INFO_OPOSICION)
                },
                onNavigateToRutinasLibres = {
                    navController.navigate(NavDestinations.RUTINAS_LIBRES)
                },
                onNavigateToCrearRutina = {
                    navController.navigate(NavDestinations.CREAR_RUTINA)
                },
                onNavigateToDetallesRutina = { id ->
                    navController.navigate("detalles_rutina/$id")
                },
                onNavigateToEditarPerfil = {
                    navController.navigate(NavDestinations.EDITAR_PERFIL)
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(NavDestinations.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

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

        composable(NavDestinations.ENTRENAMIENTOS) {
            EntrenamientosScreen(
                authViewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() },
                onEntrenamientoFinalizado = {
                    navController.popBackStack()
                }
            )
        }

        composable(NavDestinations.REGISTRAR_ENTRENAMIENTO) {
            RegistrarEntrenamientoScreen(
                authViewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() },
                onRegistrado = { navController.popBackStack() }
            )
        }

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

        composable(NavDestinations.EDITAR_PERFIL) {
            EditarPerfilScreen(
                authViewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

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

        composable(NavDestinations.INFO_OPOSICION) {
            OposicionInfoScreen(
                authViewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
