package com.opofit.miapp.data.responsemodels

data class EjercicioRutina(
    val id_ejercicio: Int? = null,
    val nombre: String,
    val video_url: String?,
    val unidad: String? = null,
    val series: Int,
    val repeticiones: Double,
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
    val animacion_url: String? = null,
    val instrucciones_tecnicas: String? = null,
    val tipo_ilustracion: String? = null,
    val grupo_muscular: String? = null,
    val equipamiento: String? = null,
    val categoria: String? = null,
    val pilar: String? = null,
    val series: Int,
    val repeticiones: Double,
    val descanso: Int,
    val unidad: String? = null,
    val series_base: Int? = null,
    val repeticiones_base: Double? = null,
    val personalizado: Boolean = false,
    val motivo_ajuste: String? = null,
    val sustituido: Boolean = false,
    val nombre_original: String? = null,
    val motivo_sustitucion: String? = null,
    // v7-doctorado: si el ejercicio es un WOD/AMRAP/EMOM/etc., el frontend usa
    // estos campos para mostrar el input correcto (timer/contador rondas).
    val modalidad: String? = null,
    val score_tipo: String? = null,
    val time_cap_seg: Int? = null
)

data class PilarResumen(
    val pilar: String,
    val etiqueta: String? = null,
    val notaMedia: Double = 0.0,
    val pruebas: List<String> = emptyList()
)

data class PersonalizacionPlan(
    val resumen: String = "",
    val pilares_debiles: List<PilarResumen> = emptyList(),
    val pilares_fuertes: List<PilarResumen> = emptyList(),
    val ajustes_aplicados: Int = 0,
    val nivel_usado: String? = null,
    val racha_dias: Int = 0,
    val sesiones_semana: Int = 0,
    val explicacion_ia: String? = null,
    val entorno_entreno: String? = null,
    val entorno_etiqueta: String? = null,
    val entorno_emoji: String? = null,
    val variacion_seed: Int? = null,
    val sustituciones: Int? = null,
    val coaching_fuente: String? = null
)

data class EntornoEntrenoOpcion(
    val id: String,
    val etiqueta: String,
    val emoji: String? = null,
    val descripcion: String? = null
)

data class EntornoUsuarioData(
    val entorno: String? = null,
    val seed: Int = 0
)

data class EntornosListResponse(
    val ok: Boolean,
    val data: List<EntornoEntrenoOpcion>? = null,
    val msg: String? = null
)

data class EntornoUsuarioResponse(
    val ok: Boolean,
    val data: EntornoUsuarioData? = null,
    val msg: String? = null
)

data class PlanRegenerarResponse(
    val ok: Boolean,
    val data: PlanSemanal? = null,
    val msg: String? = null
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

data class DiaCalendario(
    val fecha: String,
    val dia: Int,
    val tiene_entreno: Boolean = false,
    val id_plan_dia: Int? = null,
    val enfoque: String? = null,
    val titulo: String? = null,
    val completada: Boolean = false,
    val es_hoy: Boolean = false
)

data class PlanCalendario(
    val year: Int,
    val month: Int,
    val dias: List<DiaCalendario> = emptyList(),
    val semana: List<DiaPlan>? = null
)

data class PlanCalendarioResponse(
    val ok: Boolean,
    val data: PlanCalendario? = null,
    val msg: String? = null
)

data class PlanSemanal(
    val id_plan: Int,
    val dias_por_semana: Int,
    val dia_hoy: Int,
    val nombre_dia_hoy: String,
    val semana: List<DiaPlan> = emptyList(),
    val sesion_hoy: DiaPlan? = null,
    val proxima_sesion: DiaPlan? = null,
    val personalizacion: PersonalizacionPlan? = null
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
