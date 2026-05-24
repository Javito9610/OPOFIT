package com.opofit.miapp.data.responsemodels

data class AmigoItem(
    val id_amistad: Int,
    val amigo_id: Int,
    val amigo_nombre: String,
    val oposicion_id: Int
)

data class SolicitudAmistadItem(
    val id_amistad: Int,
    val solicitante_id: Int,
    val solicitante_nombre: String
)

data class AmigosData(
    val amigos: List<AmigoItem>?,
    val pendientes: List<SolicitudAmistadItem>?
)

data class AmigosListResponse(val ok: Boolean, val data: AmigosData?)

data class UsuarioBusqueda(
    val id_usuario: Int,
    val nombre: String,
    val perfil_publico: Int
)

data class BuscarUsuariosResponse(val ok: Boolean, val data: List<UsuarioBusqueda>?)

data class SolicitarAmistadRequest(val idUsuario: Int)
data class ResponderAmistadRequest(val idAmistad: Int, val aceptar: Boolean)
data class EnviarMensajeRequest(val idDestinatario: Int, val texto: String)

data class MensajeChat(
    val id_mensaje: Int,
    val id_remitente: Int,
    val id_destinatario: Int,
    val texto: String,
    val enviado_en: String
)

data class ChatResponse(val ok: Boolean, val data: List<MensajeChat>?)

data class FeedActividadItem(
    val tipo: String,
    val fecha: String?,
    val usuarioNombre: String?,
    val usuarioId: Int? = null,
    val detalle: String?
)

data class FeedActividadResponse(val ok: Boolean, val data: List<FeedActividadItem>?)
