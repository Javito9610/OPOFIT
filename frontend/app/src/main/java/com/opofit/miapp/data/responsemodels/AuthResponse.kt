package com.opofit.miapp.data.responsemodels

data class AuthResponse(
    val ok: Boolean,
    val userId: Int? = null,
    val msg: String? = null,
    val message: String? = null,
    val user: UsuarioData? = null,
    val token: String? = null
)