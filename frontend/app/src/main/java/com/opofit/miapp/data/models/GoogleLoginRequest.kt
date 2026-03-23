package com.opofit.miapp.data.models

data class GoogleLoginRequest(
    val googleToken: String,
    val email: String,
    val nombre: String
)