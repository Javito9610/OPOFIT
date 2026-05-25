package com.opofit.miapp.data.responsemodels

data class DashboardResumenResponse(
    val ok: Boolean,
    val data: DashboardResumen? = null,
    val msg: String? = null
)

data class DashboardResumen(
    val oposicionNombre: String? = null,
    val sesionesSemana: Int = 0,
    val minutosSemana: Int = 0,
    val sesionesTotales: Int = 0,
    val rachaDias: Int = 0,
    val ultimaSesion: UltimaSesionResumen? = null,
    val ultimoSimulacro: UltimoSimulacroResumen? = null,
    val notaMedia: String? = null,
    val nivel: String? = null,
    val pruebasCompletadas: Int? = null,
    val totalPruebas: Int? = null,
    val rankingPosicion: Int? = null,
    val rankingTotal: Int? = null,
    val rankingNotaMedia: Double? = null,
    val graficaSemanal: List<DiaActividad>? = null,
    val entrenoHoy: EntrenoHoyResumen? = null
)

data class EntrenoHoyResumen(
    val nombreDia: String? = null,
    val enfoque: String? = null,
    val titulo: String? = null,
    val descripcion: String? = null,
    val esHoy: Boolean = false,
    val completada: Boolean = false,
    val id_plan_dia: Int? = null,
    val id_rutina_opo: Int? = null
)

data class DiaActividad(
    val dia: String? = null,
    val etiqueta: String? = null,
    val sesiones: Int = 0,
    val minutos: Int = 0
)

data class UltimaSesionResumen(
    val fecha: String? = null,
    val duracionMin: Int = 0,
    val tipo: String? = null
)

data class UltimoSimulacroResumen(
    val notaMedia: String? = null,
    val fecha: String? = null
)
