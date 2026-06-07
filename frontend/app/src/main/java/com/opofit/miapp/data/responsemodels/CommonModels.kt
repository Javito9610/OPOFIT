package com.opofit.miapp.data.responsemodels

/**
 * Respuesta genérica para endpoints que solo confirman éxito/error sin payload.
 * Útil para DELETE, PUT simples, etc.
 */
data class SimpleOkResponse(
    val ok: Boolean,
    val msg: String? = null
)
