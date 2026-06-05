package com.opofit.miapp.data.responsemodels

data class UsuarioData(
    val id_usuario: Int,
    val nombre: String,
    val email: String,
    val genero: String,
    val peso: Double,
    val altura: Double,
    val imc: Double,
    val oposiciones_id_oposicion: Int? = null,
    val modo_uso: String? = null,
    val avatar_url: String? = null
)