package com.opofit.miapp.data.responsemodels

data class EjercicioRutina(
    val id_ejercicio: Int? = null,
    val nombre: String,
    val video_url: String?,
    val series: Int,
    val repeticiones: Int,
    val descanso: Int
)

data class BloqueRutina(
    val id_rutina_opo: Int? = null,
    val bloque: String,
    val ejercicios: List<EjercicioRutina>
)

data class RutinasData(
    val notaActual: String,
    val nivelAsignado: String,
    val rutinaCompleta: List<BloqueRutina>
)

data class RutinasResponse(
    val ok: Boolean,
    val data: RutinasData?
)
