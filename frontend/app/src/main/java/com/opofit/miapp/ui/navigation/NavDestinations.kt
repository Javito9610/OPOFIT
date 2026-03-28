package com.opofit.miapp.ui.navigation

object NavDestinations {
    // ============ 1. AUTENTICACIÓN ============
    const val LOGIN = "login"
    const val REGISTRO = "registro"

    // ============ 2. PANTALLA PRINCIPAL ============
    const val HOME = "home"

    // ============ 3-4. RUTINAS ============
    const val RUTINAS = "rutinas"
    const val RUTINAS_POR_NIVEL = "rutinas_nivel/{nivel}"
    const val CREAR_EDITAR_RUTINA = "crear_editar_rutina"
    const val RUTINAS_LIBRES = "rutinas_libres"
    const val DETALLES_RUTINA = "detalles_rutina/{rutina_id}"

    // ============ 5-6. ENTRENAMIENTOS ============
    const val ENTRENAMIENTOS = "entrenamientos"
    const val REGISTRAR_ENTRENAMIENTO = "registrar_entrenamiento"
    const val DETALLES_EJERCICIO = "detalles_ejercicio/{ejercicio_id}"

    // ============ 7-8. PERFIL ============
    const val PERFIL = "perfil"
    const val EDITAR_PERFIL = "editar_perfil"

    // ============ 9. HISTORIAL ============
    const val HISTORIAL = "historial"

    // ============ 10. AJUSTES ============
    const val AJUSTES = "ajustes"
}