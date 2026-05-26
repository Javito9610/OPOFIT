package com.opofit.miapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.opofit.miapp.gps.ui.GpsActivityDetailScreen
import com.opofit.miapp.gps.ui.GpsHubScreen
import com.opofit.miapp.gps.ui.GpsRecordingScreen
import com.opofit.miapp.ui.screens.ajustes.AjustesScreen
import com.opofit.miapp.ui.screens.auth.LoginScreen
import com.opofit.miapp.ui.screens.auth.RegisterScreen
import com.opofit.miapp.ui.screens.entrenamientos.EntrenamientosScreen
import com.opofit.miapp.ui.screens.entrenamientos.EntrenamientoPersonalizadoScreen
import com.opofit.miapp.ui.screens.entrenamientos.RegistrarEntrenamientoScreen
import com.opofit.miapp.ui.screens.main.MainScreen
import com.opofit.miapp.ui.screens.oposicion.OposicionInfoScreen
import com.opofit.miapp.ui.screens.perfil.EditarPerfilScreen
import com.opofit.miapp.ui.screens.rutinas.CrearRutinaScreen
import com.opofit.miapp.ui.screens.rutinas.DetallesEjercicioScreen
import com.opofit.miapp.ui.screens.rutinas.DetallesRutinaScreen
import com.opofit.miapp.ui.screens.rutinas.RutinasLibresScreen
import com.opofit.miapp.ui.screens.simulacro.SimulacroScreen
import com.opofit.miapp.ui.screens.comunidad.ComunidadScreen
import com.opofit.miapp.ui.screens.ranking.RankingScreen
import com.opofit.miapp.ui.screens.premium.PremiumScreen
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

        
        composable(NavDestinations.MAIN) {
            MainScreen(
                authViewModel = authViewModel,
                onNavigateToEntrenamientos = { enfoque, idPlanDia, idRutinaOpo ->
                    val e = enfoque?.takeIf { it.isNotBlank() } ?: ""
                    navController.navigate(
                        "entrenamientos?enfoque=$e&idPlanDia=${idPlanDia ?: 0}&idRutinaOpo=${idRutinaOpo ?: 0}"
                    )
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
                onNavigateToSimulacro = {
                    navController.navigate(NavDestinations.SIMULACRO)
                },
                onNavigateToRanking = {
                    navController.navigate(NavDestinations.RANKING)
                },
                onNavigateToComunidad = {
                    navController.navigate(NavDestinations.COMUNIDAD)
                },
                onNavigateToPremium = {
                    navController.navigate(NavDestinations.PREMIUM)
                },
                onNavigateToGps = {
                    navController.navigate(NavDestinations.GPS_HUB)
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
                onIniciarEntrenamiento = { navController.navigate("entrenamiento_pers/$rutinaId") }
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

        composable(
            route = NavDestinations.ENTRENAMIENTO_PERS,
            arguments = listOf(navArgument("rutina_id") { type = NavType.StringType })
        ) { backStackEntry ->
            val rutinaIdStr = backStackEntry.arguments?.getString("rutina_id") ?: "0"
            val rutinaId = rutinaIdStr.toIntOrNull() ?: 0
            EntrenamientoPersonalizadoScreen(
                rutinaId = rutinaId,
                authViewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() },
                onEntrenamientoFinalizado = { navController.popBackStack() }
            )
        }

        composable(
            route = NavDestinations.ENTRENAMIENTOS,
            arguments = listOf(
                navArgument("enfoque") { type = NavType.StringType; defaultValue = "" },
                navArgument("idPlanDia") { type = NavType.IntType; defaultValue = 0 },
                navArgument("idRutinaOpo") { type = NavType.IntType; defaultValue = 0 }
            )
        ) { backStackEntry ->
            val enfoque = backStackEntry.arguments?.getString("enfoque").orEmpty()
            val idPlanDia = backStackEntry.arguments?.getInt("idPlanDia") ?: 0
            val idRutinaOpo = backStackEntry.arguments?.getInt("idRutinaOpo") ?: 0
            EntrenamientosScreen(
                authViewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() },
                onEntrenamientoFinalizado = {
                    navController.popBackStack()
                },
                onNavigateToGps = { navController.navigate(NavDestinations.GPS_HUB) },
                initialEnfoque = enfoque,
                initialPlanDiaId = idPlanDia.takeIf { it > 0 },
                initialRutinaOpoId = idRutinaOpo.takeIf { it > 0 }
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
                onNavigateBack = { navController.popBackStack() },
                onNavigatePremium = { navController.navigate(NavDestinations.PREMIUM) }
            )
        }

        composable(NavDestinations.SIMULACRO) {
            SimulacroScreen(
                authViewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigatePremium = { navController.navigate(NavDestinations.PREMIUM) }
            )
        }

        composable(NavDestinations.RANKING) {
            RankingScreen(
                authViewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateComunidad = { navController.navigate(NavDestinations.COMUNIDAD) }
            )
        }

        composable(NavDestinations.COMUNIDAD) {
            ComunidadScreen(
                authViewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NavDestinations.PREMIUM) {
            PremiumScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(NavDestinations.GPS_HUB) {
            GpsHubScreen(
                onNavigateBack = { navController.popBackStack() },
                onStartRecording = { navController.navigate(NavDestinations.GPS_RECORDING) },
                onOpenActivity = { id -> navController.navigate("gps_activity/$id") }
            )
        }

        composable(NavDestinations.GPS_RECORDING) {
            GpsRecordingScreen(
                onFinishSaved = { id ->
                    navController.navigate("gps_activity/$id") {
                        popUpTo(NavDestinations.GPS_HUB) { inclusive = false }
                    }
                },
                onDiscarded = { navController.popBackStack() }
            )
        }

        composable(
            route = NavDestinations.GPS_ACTIVITY_DETAIL,
            arguments = listOf(navArgument("activity_id") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("activity_id") ?: ""
            GpsActivityDetailScreen(
                activityId = id,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
