package com.opofit.miapp.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.opofit.miapp.data.api.RetrofitClient
import com.opofit.miapp.data.local.TokenManager
import com.opofit.miapp.data.responsemodels.BloqueRutina
import com.opofit.miapp.data.responsemodels.Oposicion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RutinasViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(getApplication())

    data class RutinasUiState(
        val isLoading: Boolean = false,
        val error: String = "",
        val notaActual: String = "",
        val nivelAsignado: String = "",
        val rutinaCompleta: List<BloqueRutina> = emptyList(),
        val oposiciones: List<Oposicion> = emptyList(),
        val oposicionesLoading: Boolean = false
    )

    private val _uiState = MutableStateFlow(RutinasUiState())
    val uiState: StateFlow<RutinasUiState> = _uiState.asStateFlow()

    fun cargarRutina(userId: Int, oposicionId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = "") }
            try {
                val token = tokenManager.getToken().first() ?: ""
                val response = RetrofitClient.rutinasApi.getMiEntrenamiento(
                    "Bearer $token", userId, oposicionId
                )
                if (response.ok && response.data != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            notaActual = response.data.notaActual,
                            nivelAsignado = response.data.nivelAsignado,
                            rutinaCompleta = response.data.rutinaCompleta
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "No se pudo cargar la rutina") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error de conexión") }
            }
        }
    }

    fun cargarOposiciones() {
        viewModelScope.launch {
            _uiState.update { it.copy(oposicionesLoading = true) }
            try {
                val token = tokenManager.getToken().first() ?: ""
                val response = RetrofitClient.oposicionesApi.getOposiciones("Bearer $token")
                if (response.ok && response.data != null) {
                    _uiState.update { it.copy(oposicionesLoading = false, oposiciones = response.data) }
                } else {
                    _uiState.update { it.copy(oposicionesLoading = false, error = "No se pudieron cargar las oposiciones") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(oposicionesLoading = false, error = e.message ?: "Error al cargar oposiciones") }
            }
        }
    }
}
