package com.opofit.miapp.data.models

data class RegisterRequest(
    val nombre: String,
    val email: String,
    val password: String,
    val genero: String,
    val peso: Double,
    val altura: Double,
    val oposiciones_id_oposicion: Int? = null,
    val modo_uso: String = "OPOSITOR",
    val marcasIniciales: List<MarcaInicial> = emptyList()
)