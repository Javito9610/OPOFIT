package com.opofit.miapp.data.responsemodels

data class EjercicioLibreItem(
    val id_ejercicio: Int,
    val series: Int,
    val repeticiones: Int
)

data class CrearRutinaLibreRequest(
    val userId: Int,
    val nombre: String,
    val ejercicios: List<EjercicioLibreItem>
)

data class CrearRutinaLibreResponse(
    val ok: Boolean,
    val message: String?
)

data class RutinaLibre(
    val id_rutina_pers: Int,
    val nombre_personalizado: String,
    val ejercicios_id_ejercicio: Int?,
    val series: Int?,
    val repeticiones: Int?
)

data class RutinasLibresListResponse(
    val ok: Boolean,
    val data: List<RutinaLibre>?
)
