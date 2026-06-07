package com.opofit.miapp.data.responsemodels

data class LogrosResponse(
    val ok: Boolean,
    val data: LogrosData? = null,
    val msg: String? = null
)

data class LogrosData(
    val rachas: RachasLogro? = null,
    val medallas: List<MedallaLogro> = emptyList(),
    val medallasDesbloqueadas: Int = 0,
    val medallasTotales: Int = 0,
    val stats: LogrosStats? = null
)

data class RachasLogro(
    val actual: Int = 0,
    val maxima: Int = 0,
    val diasActivos: Int = 0
)

data class MedallaLogro(
    val id: String,
    val nombre: String,
    val desc: String,
    val icono: String? = null,
    val desbloqueada: Boolean = false,
    val progreso: Double = 0.0
)

data class LogrosStats(
    val sesiones: Int = 0,
    val distanciaKm: Double = 0.0,
    val desnivelM: Double = 0.0
)
