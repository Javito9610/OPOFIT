package com.opofit.miapp.data.responsemodels

data class TipoLugarEntreno(val id: String, val etiqueta: String)

data class MapasTiposResponse(val ok: Boolean, val data: List<TipoLugarEntreno>?)

data class LugarEntreno(
    val id: String = "",
    val nombre: String = "",
    val direccion: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val rating: Double? = null,
    val abierto: Boolean? = null,
    val tipo: String = "",
    val distanciaM: Double = 0.0,
    val demo: Boolean? = null
)

data class LugaresResponse(val ok: Boolean, val data: List<LugarEntreno>?)

data class RoutePointDto(val lat: Double, val lng: Double)

data class RutaEntreno(
    val id: String = "",
    val nombre: String = "",
    val distanciaKm: Double = 0.0,
    val distanciaObjetivoKm: Double? = null,
    val puntos: List<RoutePointDto> = emptyList(),
    val origen: String = ""
)

data class RutaEntrenoResponse(val ok: Boolean, val data: RutaEntreno?)

data class RutaPersonalizadaBody(
    val waypoints: List<RoutePointDto>,
    val nombre: String = "Ruta personalizada",
    val actividad: String = "CARRERA"
)
