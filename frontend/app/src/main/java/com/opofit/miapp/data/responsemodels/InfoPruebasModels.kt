package com.opofit.miapp.data.responsemodels

data class InfoPrueba(
    val id_pruebas_oficiales: Int,
    val nombre_prueba: String,
    val descripcion: String?,
    val trucos: String?,
    val genero: String?,
    val marca_valor: Double?,
    val nota: Double?
)

data class InfoPruebasResponse(
    val ok: Boolean,
    val data: List<InfoPrueba>?
)
