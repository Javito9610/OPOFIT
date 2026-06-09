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
    val tipo_ilustracion: String? = null,
    // v7-doctorado: tipo de tracking del ejercicio para que la UI muestre el
    // input correcto y el historial grafique según el tipo:
    //   modalidad = convencional | calistenia | crossfit_lift | wod | emom |
    //               amrap | for_time | tabata | death_by | chipper | ladder |
    //               test | movilidad | cardio
    //   score_tipo = reps | tiempo | tiempo_max | rondas | rondas_reps |
    //                rondas_completadas | reps_min_ronda | ultima_ronda |
    //                peso | distancia | rpe | calorias
    val modalidad: String? = null,
    val score_tipo: String? = null
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
