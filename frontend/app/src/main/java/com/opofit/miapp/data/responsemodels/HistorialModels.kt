package com.opofit.miapp.data.responsemodels

data class ResumenHistorial(
    val periodo: String,
    val sesiones: Int,
    val minutos: Int,
    val gps: ResumenGps?,
    val heatmap: List<HeatmapDia>,
    val porTipo: List<DistribucionTipo>,
    val topPrs: List<TopPr>
)

data class ResumenGps(
    val actividades: Int,
    val distanciaM: Double,
    val duracionSeg: Int,
    val desnivelPosM: Double
)

data class HeatmapDia(
    val dia: String,
    val sesiones: Int,
    val minutos: Int
)

data class DistribucionTipo(
    val tipo: String,
    val sesiones: Int
)

data class TopPr(
    val ejercicio: String,
    val valor: Double
)

data class ResumenHistorialResponse(
    val ok: Boolean,
    val data: ResumenHistorial? = null,
    val msg: String? = null
)

data class SesionItem(
    val id: Int,
    val fechaEntreno: String? = null,
    val tipoRutina: String? = null,
    val duracionSeg: Int? = null,
    val gpsActividadUuid: String? = null,
    val idRutinaOpo: Int? = null,
    val idRutinaPers: Int? = null,
    val enfoque: String? = null,
    val nivel: String? = null,
    val idPlan: Int? = null,
    val tituloSesion: String? = null,
    val planNombre: String? = null,
    val planNivel: String? = null,
    val nombrePersonalizado: String? = null,
    val nEjercicios: Int = 0
)

data class SesionesResponse(
    val ok: Boolean,
    val data: List<SesionItem>? = null,
    val msg: String? = null
)

data class EjercicioSesion(
    val idEjercicio: Int,
    val nombre: String,
    val categoria: String? = null,
    val pilar: String? = null,
    val valor: Double,
    val valorAnterior: Double? = null,
    val delta: Double? = null,
    val esPr: Boolean = false
)

data class GpsActividadResumen(
    val id: String,
    val type: String,
    val distanceM: Double,
    val durationSec: Int,
    val avgPaceSecPerKm: Double,
    val avgSpeedMps: Double,
    val elevationGainM: Double,
    val avgHrBpm: Int? = null
)

data class DetalleSesion(
    val id: Int,
    val fechaEntreno: String? = null,
    val tipoRutina: String? = null,
    val duracionSeg: Int? = null,
    val enfoque: String? = null,
    val nivel: String? = null,
    val idPlan: Int? = null,
    val tituloSesion: String? = null,
    val planNombre: String? = null,
    val nombrePersonalizado: String? = null,
    val idRutinaOpo: Int? = null,
    val idRutinaPers: Int? = null,
    val gpsActividadUuid: String? = null,
    val gpsActividad: GpsActividadResumen? = null,
    val ejercicios: List<EjercicioSesion> = emptyList()
)

data class DetalleSesionResponse(
    val ok: Boolean,
    val data: DetalleSesion? = null,
    val msg: String? = null
)

data class PuntoEjercicio(
    val idSesion: Int,
    val fechaEntreno: String? = null,
    val duracionSeg: Int? = null,
    val valor: Double
)

data class HistorialEjercicio(
    val ejercicio: String,
    val sesiones: Int,
    val mejor: Double,
    val peor: Double,
    val media: Double,
    val puntos: List<PuntoEjercicio>
)

data class HistorialEjercicioResponse(
    val ok: Boolean,
    val data: HistorialEjercicio? = null,
    val msg: String? = null
)

data class PlanMeta(
    val id_plan: Int? = null,
    val nombre: String? = null,
    val nivel: String? = null,
    val genero: String? = null,
    val dias_por_semana: Int? = null
)

data class SesionPlanItem(
    val id: Int,
    val fechaEntreno: String? = null,
    val duracionSeg: Int? = null,
    val enfoque: String? = null,
    val nivel: String? = null,
    val idPlanDia: Int? = null,
    val diaSemana: Int? = null,
    val tituloSesion: String? = null,
    val gpsActividadUuid: String? = null
)

data class HistorialPlan(
    val plan: PlanMeta? = null,
    val totalSesiones: Int = 0,
    val sesiones: List<SesionPlanItem> = emptyList()
)

data class HistorialPlanResponse(
    val ok: Boolean,
    val data: HistorialPlan? = null,
    val msg: String? = null
)
