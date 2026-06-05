package com.opofit.miapp.data.responsemodels

data class PostStats(
    val tipo: String? = null,
    val distanciaM: Double? = null,
    val duracionSec: Int? = null,
    val ritmoMedioSpkm: Double? = null,
    val desnivelM: Double? = null,
    val avgHrBpm: Int? = null,
    val kcal: Int? = null,
    val ejercicios: Int? = null,
    val nota: Double? = null
)

data class PostComentario(
    val idComentario: Int,
    val usuarioId: Int,
    val usuarioNombre: String? = null,
    val avatarUrl: String? = null,
    val texto: String,
    val creadoEn: String? = null
)

data class ActividadPost(
    val idPost: Int,
    val usuarioId: Int,
    val usuarioNombre: String? = null,
    val avatarUrl: String? = null,
    val titulo: String,
    val texto: String? = null,
    val fotoUrl: String? = null,
    val visibilidad: String = "AMIGOS",
    val fuente: String = "MANUAL",
    val gpsUuid: String? = null,
    val idHistorialSesion: Int? = null,
    val idSimulacro: Int? = null,
    val stats: PostStats? = null,
    val creadoEn: String? = null,
    val likes: Int = 0,
    val comentarios: Int = 0,
    val yoDiLike: Boolean = false,
    val comentariosLista: List<PostComentario>? = null
)

data class CrearPostRequest(
    val titulo: String,
    val texto: String? = null,
    val visibilidad: String = "AMIGOS",
    val fuente: String = "MANUAL",
    val gpsUuid: String? = null,
    val idHistorialSesion: Int? = null,
    val idSimulacro: Int? = null,
    val stats: PostStats? = null,
    val imagenBase64: String? = null
)

data class ComentarPostRequest(val texto: String)

data class PostsListResponse(val ok: Boolean, val data: List<ActividadPost>? = null, val msg: String? = null)
data class PostDetailResponse(val ok: Boolean, val data: ActividadPost? = null, val msg: String? = null)
data class LikePostResponse(val ok: Boolean, val data: LikeData? = null, val msg: String? = null)
data class LikeData(val liked: Boolean)
data class ComentarPostResponse(val ok: Boolean, val data: ComentarData? = null, val msg: String? = null)
data class ComentarData(val idComentario: Int, val texto: String)
