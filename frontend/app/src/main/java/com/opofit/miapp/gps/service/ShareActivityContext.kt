package com.opofit.miapp.gps.service

import com.opofit.miapp.data.responsemodels.PostStats

data class PendingShare(
    val fuente: String,
    val gpsUuid: String? = null,
    val idHistorialSesion: Int? = null,
    val tituloSugerido: String = "",
    val stats: PostStats? = null,
    val segmentSlugs: List<Pair<String, Long>> = emptyList()
)

object ShareActivityContext {
    @Volatile
    var pending: PendingShare? = null

    fun set(value: PendingShare) {
        pending = value
    }

    fun consume(): PendingShare? {
        val v = pending
        pending = null
        return v
    }
}
