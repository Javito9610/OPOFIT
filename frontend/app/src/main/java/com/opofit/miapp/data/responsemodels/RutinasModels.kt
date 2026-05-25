package com.opofit.miapp.data.responsemodels

data class EjercicioRutina(
    val id_ejercicio: Int? = null,
    val nombre: String,
    val video_url: String?,
    val unidad: String? = null,
    val series: Int,
    val repeticiones: Int,
    val descanso: Int
)

data class BloqueRutina(
    val id_rutina_opo: Int? = null,
    val bloque: String,
    val ejercicios: List<EjercicioRutina>
)

data class EjercicioPlan(
    val id_ejercicio: Int? = null,
    val nombre: String,
    val video_url: String? = null,
    val categoria: String? = null,
    val pilar: String? = null,
    val series: Int,
    val repeticiones: Int,
    val descanso: Int,
    val unidad: String? = null
)

data class DiaPlan(
    val id_plan_dia: Int,
    val dia_semana: Int,
    val nombre_dia: String,
    val orden: Int,
    val enfoque: String,
    val titulo: String,
    val descripcion: String? = null,
    val id_rutina_opo: Int,
    val es_hoy: Boolean = false,
    val completada: Boolean = false,
    val ejercicios: List<EjercicioPlan> = emptyList()
)

data class PlanSemanal(
    val id_plan: Int,
    val dias_por_semana: Int,
    val dia_hoy: Int,
    val nombre_dia_hoy: String,
    val semana: List<DiaPlan> = emptyList(),
    val sesion_hoy: DiaPlan? = null,
    val proxima_sesion: DiaPlan? = null
)

data class RutinasData(
    val notaActual: String,
    val nivelAsignado: String,
    val rutinaCompleta: List<BloqueRutina>,
    val planSemanal: PlanSemanal? = null,
    val totalPruebas: Int? = null,
    val pruebasCompletadas: Int? = null,
    val pruebasFaltantes: Int? = null,
    val esPremium: Boolean? = null,
    val nivelRutinasMostradas: String? = null,
    val nivelPremiumBloqueado: Boolean? = null,
    val msgPremium: String? = null
)

data class RutinasResponse(
    val ok: Boolean,
    val msg: String? = null,
    val data: RutinasData?
)
