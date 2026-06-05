package com.opofit.miapp.data.responsemodels

data class GrupoComunidad(
    val id_grupo: Int,
    val nombre: String,
    val descripcion: String? = null,
    val id_oposicion: Int? = null,
    val miembros: Int = 0,
    val soy_miembro: Boolean = false,
    val creado_en: String? = null
)

data class GruposListResponse(val ok: Boolean, val data: List<GrupoComunidad>? = null, val msg: String? = null)

data class CrearGrupoRequest(
    val nombre: String,
    val descripcion: String? = null,
    val idOposicion: Int? = null
)

data class MensajeGrupo(
    val id_mensaje: Int,
    val id_usuario: Int,
    val nombre_usuario: String? = null,
    val texto: String,
    val enviado_en: String? = null
)

data class MensajesGrupoResponse(val ok: Boolean, val data: List<MensajeGrupo>? = null, val msg: String? = null)

data class EnviarMensajeGrupoRequest(val texto: String)

data class QuedadaGrupo(
    val id_quedada: Int,
    val titulo: String,
    val lugar: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val fecha_hora: String? = null,
    val creador_nombre: String? = null
)

data class CrearQuedadaRequest(
    val titulo: String,
    val lugar: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val fechaHora: String? = null
)

data class QuedadasResponse(val ok: Boolean, val data: List<QuedadaGrupo>? = null, val msg: String? = null)

data class UsuarioCerca(
    val id_usuario: Int,
    val nombre: String,
    val modo_uso: String? = null,
    val oposicion_nombre: String? = null,
    val distancia_m: Double = 0.0,
    val avatar_url: String? = null
)

data class CercaResponse(val ok: Boolean, val data: List<UsuarioCerca>? = null, val msg: String? = null)

data class UbicacionRequest(
    val lat: Double,
    val lng: Double,
    val visible: Boolean = true
)

data class OkMsgResponse(val ok: Boolean, val msg: String? = null)
