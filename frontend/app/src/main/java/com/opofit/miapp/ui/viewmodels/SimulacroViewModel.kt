package com.opofit.miapp.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.opofit.miapp.data.api.RetrofitClient
import com.opofit.miapp.data.local.TokenManager
import com.opofit.miapp.data.responsemodels.PruebaOficialSimulacro
import com.opofit.miapp.gps.service.ChronoForegroundService
import com.opofit.miapp.gps.service.SessionTimerTracker
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Holds simulacro session state so it survives navigation away and back.
 * El cronómetro corre en [ChronoForegroundService] para mantenerse en segundo plano.
 */
class SimulacroViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(getApplication())
    private var syncJob: Job? = null

    data class State(
        val loading: Boolean = true,
        val error: String = "",
        val pruebas: List<PruebaOficialSimulacro> = emptyList(),
        val paso: Int = 0,
        val valores: Map<Int, String> = emptyMap(),
        val cronometroActivo: Boolean = false,
        val elapsedMs: Long = 0L,
        val oposicionId: Int = 1,
        val marcasPreCargadas: Boolean = false
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        syncJob = viewModelScope.launch {
            SessionTimerTracker.state.collect { timer ->
                if (timer.label == TIMER_LABEL) {
                    _state.update {
                        it.copy(
                            elapsedMs = timer.elapsedMs,
                            cronometroActivo = timer.active && !timer.paused
                        )
                    }
                }
            }
        }
    }

    fun cargarPruebas(oposicionId: Int) {
        if (_state.value.pruebas.isNotEmpty() && _state.value.oposicionId == oposicionId) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = "", oposicionId = oposicionId) }
            try {
                val token = tokenManager.getToken().first() ?: ""
                val resp = RetrofitClient.simulacroApi.listarPruebas("Bearer $token", oposicionId)
                if (resp.ok && resp.data != null) {
                    _state.update { it.copy(loading = false, pruebas = resp.data, paso = 0) }
                } else {
                    _state.update { it.copy(loading = false, error = resp.msg ?: "Error al cargar pruebas") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message ?: "Error de conexión") }
            }
        }
    }

    fun setPaso(nuevo: Int) {
        stopTimer()
        ChronoForegroundService.stop(getApplication())
        _state.update { it.copy(paso = nuevo, elapsedMs = 0L, cronometroActivo = false) }
        SessionTimerTracker.setElapsed(0L)
    }

    fun setValor(idPrueba: Int, valor: String) {
        _state.update { it.copy(valores = it.valores + (idPrueba to valor)) }
    }

    fun removeValor(idPrueba: Int) {
        _state.update { it.copy(valores = it.valores - idPrueba) }
    }

    fun startTimer() {
        if (_state.value.cronometroActivo) return
        val ctx = getApplication<Application>()
        val resume = _state.value.elapsedMs
        SessionTimerTracker.start(TIMER_LABEL, resume)
        ChronoForegroundService.start(ctx)
        _state.update { it.copy(cronometroActivo = true) }
    }

    fun stopTimer() {
        val ctx = getApplication<Application>()
        if (SessionTimerTracker.state.value.active) {
            ChronoForegroundService.pause(ctx)
        }
        _state.update { it.copy(cronometroActivo = false) }
    }

    fun resetTimer() {
        stopTimer()
        SessionTimerTracker.setElapsed(0L)
        _state.update { it.copy(elapsedMs = 0L) }
    }

    fun precargarMarcas(userId: Int, oposicionId: Int) {
        if (_state.value.marcasPreCargadas) return
        viewModelScope.launch {
            try {
                val token = tokenManager.getToken().first() ?: ""
                val resp = RetrofitClient.infoPruebasApi.getMarcasUsuario("Bearer $token", userId, oposicionId)
                if (resp.ok && resp.data != null) {
                    val desdePerfil = resp.data
                        .filter { it.valord_record != null }
                        .associate { it.id_pruebas_oficiales to it.valord_record!!.toString() }
                    _state.update { current ->
                        val merged = desdePerfil + current.valores
                        current.copy(valores = merged, marcasPreCargadas = true)
                    }
                } else {
                    _state.update { it.copy(marcasPreCargadas = true) }
                }
            } catch (_: Exception) {
                _state.update { it.copy(marcasPreCargadas = true) }
            }
        }
    }

    fun setElapsedMs(ms: Long) {
        SessionTimerTracker.setElapsed(ms)
        _state.update { it.copy(elapsedMs = ms) }
    }

    fun reset() {
        stopTimer()
        ChronoForegroundService.stop(getApplication())
        _state.update { State() }
    }

    override fun onCleared() {
        super.onCleared()
        syncJob?.cancel()
    }

    companion object {
        private const val TIMER_LABEL = "Simulacro oficial"
    }
}
