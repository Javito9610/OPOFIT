package com.opofit.miapp.gps.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Puente entre pantallas: plan → entrenamiento → mapa → GPS → vuelta al registro.
 */
data class EntrenoFlowState(
    val origen: String = "",
    val returnRoute: String? = null,
    val tituloSesion: String? = null,
    val distKmObjetivo: Double? = null,
    val enfoque: String? = null
)

object EntrenoFlowContext {
    private val _state = MutableStateFlow<EntrenoFlowState?>(null)
    val state: StateFlow<EntrenoFlowState?> = _state.asStateFlow()

    fun vincularEntrenamiento(
        returnRoute: String,
        titulo: String? = null,
        distKm: Double? = null,
        enfoque: String? = null
    ) {
        _state.value = EntrenoFlowState(
            origen = "entrenamiento",
            returnRoute = returnRoute,
            tituloSesion = titulo,
            distKmObjetivo = distKm,
            enfoque = enfoque
        )
    }

    fun vincularDesdePlan(titulo: String?, distKm: Double?, enfoque: String?) {
        _state.value = EntrenoFlowState(
            origen = "plan",
            returnRoute = null,
            tituloSesion = titulo,
            distKmObjetivo = distKm,
            enfoque = enfoque
        )
    }

    fun peekReturnRoute(): String? = _state.value?.returnRoute

    fun consumeReturnRoute(): String? {
        val r = _state.value?.returnRoute
        if (r != null) _state.value = _state.value?.copy(returnRoute = null)
        return r
    }

    fun clear() {
        _state.value = null
    }
}
