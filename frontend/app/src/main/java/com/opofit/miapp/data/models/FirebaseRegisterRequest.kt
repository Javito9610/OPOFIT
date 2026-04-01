package com.opofit.miapp.data.models

data class FirebaseRegisterRequest(
    val idToken: String,
    val nombre: String,
    val genero: String,
    val peso: Double,
    val altura: Double,
    val oposiciones_id_oposicion: Int
)
