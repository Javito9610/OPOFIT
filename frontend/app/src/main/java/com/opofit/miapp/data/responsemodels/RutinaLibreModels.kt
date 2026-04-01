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
    val msg: String? = null,
    val message: String? = null
)

data class RutinaLibreEjercicio(
    val id_ejercicio: Int,
    val nombre_ejercicio: String? = null,
    val series: Int?,
    val repeticiones: Int?,
    val descanso: Int? = null
)

data class RutinaLibre(
    val id_rutina_pers: Int,
    val nombre_personalizado: String,
    val ejercicios: List<RutinaLibreEjercicio> = emptyList()
)

data class RutinasLibresListResponse(
    val ok: Boolean,
    val data: List<RutinaLibre>?
)
