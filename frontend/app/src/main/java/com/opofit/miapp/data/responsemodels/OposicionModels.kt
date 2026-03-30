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
    val id_pruebas_oficiales: Int? = null,
    val nombre_prueba: String = "",
    val descripcion: String? = null,
    val trucos: String? = null
)

data class NoticiaOposicion(
    val titulo: String,
    val contenido: String? = null,
    val fecha_publicacion: String? = null
)

data class OposicionDetalleResponse(
    val ok: Boolean,
    val oposicion: Oposicion? = null,
    val pruebas: List<PruebaOposicion>?,
    val noticias: List<NoticiaOposicion>?
)

data class NoticiaRss(
    val titulo: String = "",
    val enlace: String = "",
    val fecha: String = "",
    val fuente: String = "",
    val descripcion: String = ""
)

data class NoticiasRssResponse(
    val ok: Boolean,
    val data: List<NoticiaRss>?
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
