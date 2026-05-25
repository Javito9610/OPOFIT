package com.opofit.miapp.data.responsemodels

data class Ejercicio(
    val id_ejercicio: Int,
    val nombre: String,
    val video_url: String? = null,
    val instrucciones_tecnicas: String? = null,
    val categoria: String? = null,
    val pilar: String? = null,
    val grupo_muscular: String? = null,
    val equipamiento: String? = null
)

data class EjerciciosListResponse(
    val ok: Boolean,
    val data: List<Ejercicio>?
)
