package com.opofit.miapp.data.responsemodels

data class SegmentoItem(
    val id_segmento: Int,
    val slug: String,
    val nombre: String,
    val tipo: String,
    val distancia_m: Double,
    val mejor_si_menor: Int,
    val categoria: String,
    val lat_inicio: Double? = null,
    val lng_inicio: Double? = null,
    val lat_fin: Double? = null,
    val lng_fin: Double? = null
)

data class SegmentoRankingEntry(
    val posicion: Int,
    val usuarioId: Int,
    val usuarioNombre: String? = null,
    val avatarUrl: String? = null,
    val duracionMs: Long,
    val duracionSec: Double,
    val gpsUuid: String? = null,
    val creadoEn: String? = null,
    val esRecord: Boolean = false
)

data class SegmentoRankingData(
    val segmento: SegmentoInfo,
    val ranking: List<SegmentoRankingEntry>
)

data class SegmentoInfo(
    val idSegmento: Int,
    val slug: String,
    val nombre: String,
    val tipo: String,
    val distanciaM: Double,
    val mejorSiMenor: Boolean,
    val categoria: String
)

data class SegmentosListResponse(val ok: Boolean, val data: List<SegmentoItem>? = null)
data class SegmentoRankingResponse(val ok: Boolean, val data: SegmentoRankingData? = null)

data class EsfuerzoSegmentoRequest(val duracionMs: Long, val gpsUuid: String? = null)
data class EsfuerzoDesdeActividadRequest(
    val gpsUuid: String,
    val esfuerzos: List<EsfuerzoSlug>
)

data class EsfuerzoSlug(val slug: String, val duracionMs: Long)

data class CrearSegmentoGeoRequest(
    val nombre: String,
    val latInicio: Double,
    val lngInicio: Double,
    val latFin: Double,
    val lngFin: Double,
    val categoria: String = "CARRERA"
)
