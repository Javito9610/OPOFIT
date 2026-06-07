package com.opofit.miapp.data.responsemodels

data class Ejercicio(
    val id_ejercicio: Int,
    val nombre: String,
    val video_url: String? = null,
    val instrucciones_tecnicas: String? = null,
    val categoria: String? = null,
    val pilar: String? = null,
    val grupo_muscular: String? = null,
    val equipamiento: String? = null,
    val tipo_ilustracion: String? = null
)

fun Ejercicio.toEjercicioPlan(prescripcion: String = ""): com.opofit.miapp.data.responsemodels.EjercicioPlan {
    val parts = prescripcion.split("×", limit = 2)
    val series = parts.getOrNull(0)?.toIntOrNull() ?: 3
    val reps = parts.getOrNull(1)?.toDoubleOrNull() ?: 10.0
    return com.opofit.miapp.data.responsemodels.EjercicioPlan(
        id_ejercicio = id_ejercicio,
        nombre = nombre,
        video_url = video_url,
        instrucciones_tecnicas = instrucciones_tecnicas,
        tipo_ilustracion = tipo_ilustracion,
        grupo_muscular = grupo_muscular,
        equipamiento = equipamiento,
        categoria = categoria,
        pilar = pilar,
        series = series,
        repeticiones = reps,
        descanso = 90
    )
}

data class EjerciciosListResponse(
    val ok: Boolean,
    val data: List<Ejercicio>?
)
