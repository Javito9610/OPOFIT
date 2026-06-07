package com.opofit.miapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.opofit.miapp.gps.ui.GpsActivityDetailScreen
import com.opofit.miapp.gps.ui.GpsHubScreen
import com.opofit.miapp.gps.ui.MapaEntrenoScreen
import com.opofit.miapp.gps.model.ActivityType
import com.opofit.miapp.gps.service.GpsRecordingContext
import com.opofit.miapp.gps.ui.GpsRecordingScreen
import com.opofit.miapp.ui.screens.ajustes.AjustesScreen
import com.opofit.miapp.ui.screens.historial.EjercicioHistorialScreen
import com.opofit.miapp.ui.screens.historial.PlanHistorialScreen
import com.opofit.miapp.ui.screens.historial.SesionDetalleScreen
import com.opofit.miapp.ui.screens.integraciones.MisDispositivosScreen
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
import com.opofit.miapp.ui.screens.social.CompartirActividadScreen
import com.opofit.miapp.ui.screens.social.PostDetalleScreen
import com.opofit.miapp.ui.screens.social.SegmentosScreen
import com.opofit.miapp.ui.screens.ranking.RankingScreen
import com.opofit.miapp.ui.screens.premium.PremiumScreen
import com.opofit.miapp.ui.viewmodels.AuthViewModel
import com.opofit.miapp.gps.service.EntrenoFlowContext
import com.opofit.miapp.utils.MapaEntrenoNav

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
                onNavigateToGpsQuickStart = {
                    GpsRecordingContext.prepare(ActivityType.RUN, conRuta = false)
                    navController.navigate(NavDestinations.GPS_RECORDING)
                },
                onNavigateToMapaRuta = { distKm, titulo, enfoque ->
                    EntrenoFlowContext.vincularDesdePlan(titulo, distKm, enfoque)
                    navController.navigate(MapaEntrenoNav.rutaMapa(distKm = distKm, modo = MapaEntrenoNav.MODO_RUTAS))
                },
                onNavigateToLugaresEntreno = { tipo ->
                    navController.navigate(MapaEntrenoNav.rutaMapa(modo = MapaEntrenoNav.MODO_LUGARES, tipoLugar = tipo))
                },
                onNavigateToSesionDetalle = { id ->
                    navController.navigate("historial_sesion/$id")
                },
                onNavigateToEjercicioHistorial = { id ->
                    navController.navigate("historial_ejercicio/$id")
                },
                onNavigateToPlanHistorial = { id ->
                    navController.navigate("historial_plan/$id")
                },
                onNavigateToMisDispositivos = {
                    navController.navigate(NavDestinations.MIS_DISPOSITIVOS)
                },
                onNavigateToPost = { id ->
                    navController.navigate("post_detalle/$id")
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
                onEntrenamientoFinalizado = { offerShare ->
                    if (offerShare) {
                        navController.navigate(NavDestinations.COMPARTIR_ACTIVIDAD)
                    } else {
                        navController.popBackStack()
                    }
                },
                onNavigateToGps = { distKm ->
                    EntrenoFlowContext.vincularEntrenamiento(
                        returnRoute = MapaEntrenoNav.rutaEntrenamientoPers(rutinaId),
                        titulo = "Rutina personalizada",
                        distKm = distKm,
                        enfoque = null
                    )
                    GpsRecordingContext.prepare(ActivityType.RUN, conRuta = false)
                    navController.navigate(NavDestinations.GPS_RECORDING)
                }
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
                onEntrenamientoFinalizado = { offerShare ->
                    if (offerShare) {
                        navController.navigate(NavDestinations.COMPARTIR_ACTIVIDAD)
                    } else {
                        navController.popBackStack()
                    }
                },
                onNavigateToGps = { distKm ->
                    EntrenoFlowContext.vincularEntrenamiento(
                        returnRoute = MapaEntrenoNav.rutaEntrenamiento(enfoque, idPlanDia, idRutinaOpo),
                        titulo = enfoque.ifBlank { "Entrenamiento" },
                        distKm = distKm,
                        enfoque = enfoque
                    )
                    GpsRecordingContext.prepare(ActivityType.RUN, conRuta = false)
                    navController.navigate(NavDestinations.GPS_RECORDING)
                },
                initialEnfoque = enfoque,
                initialPlanDiaId = idPlanDia.takeIf { it > 0 },
                initialRutinaOpoId = idRutinaOpo.takeIf { it > 0 }
            )
        }

        composable(NavDestinations.REGISTRAR_ENTRENAMIENTO) {
            RegistrarEntrenamientoScreen(
                authViewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() },
                onRegistrado = { offerShare ->
                    if (offerShare) {
                        navController.navigate(NavDestinations.COMPARTIR_ACTIVIDAD)
                    } else {
                        navController.popBackStack()
                    }
                }
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
                onNavigateToMisDispositivos = {
                    navController.navigate(NavDestinations.MIS_DISPOSITIVOS)
                },
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
                onNavigateBack = { navController.popBackStack() },
                onOpenPost = { id -> navController.navigate("post_detalle/$id") },
                onOpenSegmentos = { navController.navigate(NavDestinations.SEGMENTOS) }
            )
        }

        composable(NavDestinations.COMPARTIR_ACTIVIDAD) {
            CompartirActividadScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onPublicado = { postId ->
                    navController.navigate("post_detalle/$postId") {
                        popUpTo(NavDestinations.GPS_HUB) { inclusive = false }
                    }
                }
            )
        }

        composable(
            route = NavDestinations.POST_DETALLE,
            arguments = listOf(navArgument("postId") { type = NavType.IntType })
        ) { entry ->
            val postId = entry.arguments?.getInt("postId") ?: 0
            PostDetalleScreen(
                postId = postId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NavDestinations.SEGMENTOS) {
            SegmentosScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(NavDestinations.PREMIUM) {
            PremiumScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(NavDestinations.GPS_HUB) {
            GpsHubScreen(
                onNavigateBack = { navController.popBackStack() },
                onStartRecording = { navController.navigate(NavDestinations.GPS_RECORDING) },
                onOpenMapa = { tipo ->
                    val km = EntrenoFlowContext.state.value?.distKmObjetivo
                    navController.navigate(
                        MapaEntrenoNav.rutaMapa(
                            distKm = km,
                            modo = MapaEntrenoNav.MODO_RUTAS,
                            actividad = MapaEntrenoNav.actividadDesdeGps(tipo)
                        )
                    )
                },
                onOpenActivity = { id -> navController.navigate("gps_activity/$id") }
            )
        }

        composable(
            route = NavDestinations.MAPA_ENTRENO,
            arguments = listOf(
                navArgument("distKm") { type = NavType.FloatType; defaultValue = 0f },
                navArgument("modo") { type = NavType.StringType; defaultValue = MapaEntrenoNav.MODO_RUTAS },
                navArgument("tipo") { type = NavType.StringType; defaultValue = "GYM" },
                navArgument("actividad") { type = NavType.StringType; defaultValue = "CARRERA" },
                navArgument("terreno") { type = NavType.StringType; defaultValue = "CIUDAD" }
            )
        ) { backStackEntry ->
            val distKm = backStackEntry.arguments?.getFloat("distKm")?.toDouble()?.takeIf { it > 0 }
            val modo = backStackEntry.arguments?.getString("modo") ?: MapaEntrenoNav.MODO_RUTAS
            val tipo = backStackEntry.arguments?.getString("tipo") ?: "GYM"
            val actividad = backStackEntry.arguments?.getString("actividad") ?: "CARRERA"
            val terreno = backStackEntry.arguments?.getString("terreno") ?: "CIUDAD"
            MapaEntrenoScreen(
                onNavigateBack = { navController.popBackStack() },
                onUsarRutaEnGps = { navController.navigate(NavDestinations.GPS_RECORDING) },
                onActividadLibre = { navController.navigate(NavDestinations.GPS_RECORDING) },
                distanciaObjetivoKm = distKm,
                modoInicial = modo,
                tipoLugarInicial = tipo,
                actividadInicial = actividad,
                terrenoInicial = terreno
            )
        }

        composable(NavDestinations.GPS_RECORDING) {
            GpsRecordingScreen(
                onFinishSaved = { id ->
                    val volverEntreno = EntrenoFlowContext.consumeReturnRoute()
                    if (volverEntreno != null) {
                        navController.navigate(volverEntreno) {
                            popUpTo(NavDestinations.MAIN) { saveState = true }
                            launchSingleTop = true
                        }
                    } else {
                        navController.navigate(NavDestinations.COMPARTIR_ACTIVIDAD) {
                            popUpTo(NavDestinations.GPS_RECORDING) { inclusive = true }
                        }
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
                onNavigateBack = { navController.popBackStack() },
                onShareToProfile = { navController.navigate(NavDestinations.COMPARTIR_ACTIVIDAD) }
            )
        }

        composable(
            route = NavDestinations.HISTORIAL_SESION_DETALLE,
            arguments = listOf(navArgument("sesion_id") { type = NavType.IntType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("sesion_id") ?: 0
            SesionDetalleScreen(
                sesionId = id,
                onNavigateBack = { navController.popBackStack() },
                onOpenEjercicio = { idEj -> navController.navigate("historial_ejercicio/$idEj") },
                onOpenPlan = { idPlan -> navController.navigate("historial_plan/$idPlan") },
                onOpenGpsActividad = { uuid -> navController.navigate("gps_activity/$uuid") }
            )
        }

        composable(
            route = NavDestinations.HISTORIAL_EJERCICIO,
            arguments = listOf(navArgument("ejercicio_id") { type = NavType.IntType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("ejercicio_id") ?: 0
            EjercicioHistorialScreen(
                idEjercicio = id,
                onNavigateBack = { navController.popBackStack() },
                onOpenGpsActividad = { uuid -> navController.navigate("gps_activity/$uuid") }
            )
        }

        composable(
            route = NavDestinations.HISTORIAL_PLAN,
            arguments = listOf(navArgument("plan_id") { type = NavType.IntType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("plan_id") ?: 0
            PlanHistorialScreen(
                idPlan = id,
                onNavigateBack = { navController.popBackStack() },
                onOpenSesion = { idSes -> navController.navigate("historial_sesion/$idSes") }
            )
        }

        composable(NavDestinations.MIS_DISPOSITIVOS) {
            MisDispositivosScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToGpsHub = { navController.navigate(NavDestinations.GPS_HUB) }
            )
        }
    }
}
