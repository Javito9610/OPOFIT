package com.opofit.miapp.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.opofit.miapp.data.api.RetrofitClient
import com.opofit.miapp.data.local.TokenManager
import com.opofit.miapp.data.responsemodels.DetalleSesion
import com.opofit.miapp.data.responsemodels.HistorialEjercicio
import com.opofit.miapp.data.responsemodels.HistorialPlan
import com.opofit.miapp.data.responsemodels.ResumenHistorial
import com.opofit.miapp.data.responsemodels.SesionItem
import com.opofit.miapp.integraciones.EntrenoSyncService
import com.opofit.miapp.utils.ApiErrorParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HistorialAvanzadoViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(getApplication())
    private val api = RetrofitClient.historialAvanzadoApi

    data class UiState(
        val loading: Boolean = false,
        val error: String = "",
        val resumen: ResumenHistorial? = null,
        val periodo: String = "month",
        val sesiones: List<SesionItem> = emptyList(),
        val filtroTipo: String = "TODOS",
        val refreshing: Boolean = false,
        val syncMessage: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _detalle = MutableStateFlow<DetalleSesion?>(null)
    val detalle: StateFlow<DetalleSesion?> = _detalle.asStateFlow()

    private val _historialEjercicio = MutableStateFlow<HistorialEjercicio?>(null)
    val historialEjercicio: StateFlow<HistorialEjercicio?> = _historialEjercicio.asStateFlow()

    private val _historialPlan = MutableStateFlow<HistorialPlan?>(null)
    val historialPlan: StateFlow<HistorialPlan?> = _historialPlan.asStateFlow()

    fun setPeriodo(p: String) {
        if (_uiState.value.periodo == p) return
        _uiState.update { it.copy(periodo = p) }
        cargarResumen()
    }

    fun setFiltroTipo(t: String) {
        _uiState.update { it.copy(filtroTipo = t) }
        cargarSesiones()
    }

    fun cargarResumen() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = "") }
            try {
                val token = tokenManager.getToken().first().orEmpty()
                val resp = api.resumen("Bearer $token", _uiState.value.periodo)
                _uiState.update {
                    it.copy(
                        loading = false,
                        resumen = resp.data,
                        error = if (resp.ok) "" else (resp.msg ?: "")
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = ApiErrorParser.message(e)) }
            }
        }
    }

    fun cargarSesiones() {
        viewModelScope.launch {
            try {
                val token = tokenManager.getToken().first().orEmpty()
                val tipo = when (_uiState.value.filtroTipo) {
                    "OPO", "PERS" -> _uiState.value.filtroTipo
                    else -> null
                }
                val resp = api.sesiones("Bearer $token", tipo = tipo, limit = 200)
                _uiState.update { it.copy(sesiones = resp.data.orEmpty()) }
            } catch (_: Exception) { /* sin nada */ }
        }
    }

    fun cargarDetalleSesion(id: Int) {
        viewModelScope.launch {
            try {
                val token = tokenManager.getToken().first().orEmpty()
                val resp = api.detalleSesion("Bearer $token", id)
                _detalle.value = resp.data
            } catch (_: Exception) {
                _detalle.value = null
            }
        }
    }

    fun cargarHistorialEjercicio(id: Int) {
        viewModelScope.launch {
            try {
                val token = tokenManager.getToken().first().orEmpty()
                val resp = api.historialEjercicio("Bearer $token", id)
                _historialEjercicio.value = resp.data
            } catch (_: Exception) {
                _historialEjercicio.value = null
            }
        }
    }

    fun consumeSyncMessage() = _uiState.update { it.copy(syncMessage = null) }

    /** Pull-to-refresh: importa del reloj/nube y recarga historial. */
    fun refreshAll(onGpsSynced: (() -> Unit)? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(refreshing = true, syncMessage = null) }
            try {
                val token = tokenManager.getToken().first().orEmpty()
                val sync = EntrenoSyncService.syncDesdeRelojYCloud(getApplication(), token)
                cargarResumen()
                cargarSesiones()
                onGpsSynced?.invoke()
                _uiState.update { it.copy(refreshing = false, syncMessage = sync.mensaje()) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(refreshing = false, syncMessage = ApiErrorParser.message(e))
                }
            }
        }
    }

    fun cargarHistorialPlan(idPlan: Int) {
        viewModelScope.launch {
            try {
                val token = tokenManager.getToken().first().orEmpty()
                val resp = api.historialPlan("Bearer $token", idPlan)
                _historialPlan.value = resp.data
            } catch (_: Exception) {
                _historialPlan.value = null
            }
        }
    }
}
