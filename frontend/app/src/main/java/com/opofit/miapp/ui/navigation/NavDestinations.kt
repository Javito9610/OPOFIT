package com.opofit.miapp.ui.navigation

object NavDestinations {
    const val LOGIN = "login"
    const val REGISTRO = "registro"

    const val MAIN = "main"
    const val HOME = "home"

    const val RUTINAS = "rutinas"
    const val RUTINAS_POR_NIVEL = "rutinas_nivel/{nivel}"
    const val CREAR_EDITAR_RUTINA = "crear_editar_rutina"
    const val CREAR_RUTINA = "crear_rutina"
    const val RUTINAS_LIBRES = "rutinas_libres"
    const val DETALLES_RUTINA = "detalles_rutina/{rutina_id}"

    const val ENTRENAMIENTOS =
        "entrenamientos?enfoque={enfoque}&idPlanDia={idPlanDia}&idRutinaOpo={idRutinaOpo}"
    const val ENTRENAMIENTO_PERS = "entrenamiento_pers/{rutina_id}"
    const val REGISTRAR_ENTRENAMIENTO = "registrar_entrenamiento"
    const val DETALLES_EJERCICIO = "detalles_ejercicio/{ejercicio_id}"

    const val PERFIL = "perfil"
    const val EDITAR_PERFIL = "editar_perfil"

    const val HISTORIAL = "historial"

    const val AJUSTES = "ajustes"

    const val INFO_OPOSICION = "info_oposicion"
    const val SIMULACRO = "simulacro"
    const val RANKING = "ranking"
    const val PREMIUM = "premium"
    const val COMUNIDAD = "comunidad"

    const val GPS_HUB = "gps_hub"
    const val MAPA_ENTRENO =
        "mapa_entreno?distKm={distKm}&modo={modo}&tipo={tipo}&actividad={actividad}&terreno={terreno}"
    const val GPS_RECORDING = "gps_recording"
    const val GPS_ACTIVITY_DETAIL = "gps_activity/{activity_id}"

    const val HISTORIAL_SESION_DETALLE = "historial_sesion/{sesion_id}"
    const val HISTORIAL_EJERCICIO = "historial_ejercicio/{ejercicio_id}"
    const val HISTORIAL_PLAN = "historial_plan/{plan_id}"

    const val MIS_DISPOSITIVOS = "mis_dispositivos"
}
