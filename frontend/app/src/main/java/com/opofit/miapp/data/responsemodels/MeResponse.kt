package com.opofit.miapp.data.responsemodels

data class MeResponse(
    val ok: Boolean,
    val msg: String? = null,
    val user: UsuarioData? = null
)

