package com.opofit.miapp.data.responsemodels

data class Oposicion(
    val id_oposicion: Int,
    val nombre: String
)

data class OposicionesListResponse(
    val ok: Boolean,
    val data: List<Oposicion>?
)

data class PruebaOposicion(
    val id_prueba: Int,
    val nombre: String,
    val descripcion: String?
)

data class NoticiaOposicion(
    val titulo: String,
    val contenido: String?
)

data class OposicionDetalleResponse(
    val ok: Boolean,
    val pruebas: List<PruebaOposicion>?,
    val noticias: List<NoticiaOposicion>?
)

data class RequisitoOposicion(
    val id_prueba: Int,
    val nombre_prueba: String,
    val valor_minimo: Double?,
    val unidad: String?
)

data class RequisitosResponse(
    val ok: Boolean,
    val data: List<RequisitoOposicion>?
)
