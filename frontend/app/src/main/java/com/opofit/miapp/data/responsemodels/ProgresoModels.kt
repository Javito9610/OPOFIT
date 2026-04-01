package com.opofit.miapp.data.responsemodels

data class EjercicioRealizado(
    val id_ejercicio: Int,
    val valor: Double
)

data class RegistrarHistorialRequest(
    val userId: Int,
    val tipoRutina: String,
    val idRutina: Int,
    val duracion: Int,
    val ejercicios: List<EjercicioRealizado>
)

data class RegistrarHistorialResponse(
    val ok: Boolean,
    val id: Int? = null,
    val msg: String? = null,
    val message: String? = null
)

data class PuntoEvolucion(
    val fecha_entreno: String,
    val duracion_oficial: Int? = null,
    val valor_conseguido: Double,
    val nombre_ejercicio: String? = null
)

data class EvolucionResponse(
    val ok: Boolean,
    val data: List<PuntoEvolucion>?
)
