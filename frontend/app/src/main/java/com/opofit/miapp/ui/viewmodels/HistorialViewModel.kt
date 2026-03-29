package com.opofit.miapp.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.opofit.miapp.data.api.RetrofitClient
import com.opofit.miapp.data.local.TokenManager
import com.opofit.miapp.data.responsemodels.EjercicioRealizado
import com.opofit.miapp.data.responsemodels.PuntoEvolucion
import com.opofit.miapp.data.responsemodels.RegistrarHistorialRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HistorialViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(getApplication())

    data class HistorialUiState(
        val isLoading: Boolean = false,
        val error: String = "",
        val evolucion: List<PuntoEvolucion> = emptyList(),
        val registradoExitoso: Boolean = false
    )

    private val _uiState = MutableStateFlow(HistorialUiState())
    val uiState: StateFlow<HistorialUiState> = _uiState.asStateFlow()

    fun cargarEvolucion(userId: Int, idEjercicio: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = "") }
            try {
                val token = tokenManager.getToken().first() ?: ""
                val response = RetrofitClient.progresoApi.getEvolucion("Bearer $token", userId, idEjercicio)
                if (response.ok) {
                    _uiState.update { it.copy(isLoading = false, evolucion = response.data ?: emptyList()) }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "No se pudo cargar el historial") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error de conexión") }
            }
        }
    }

    fun registrarEntrenamiento(
        userId: Int,
        tipoRutina: String,
        idRutina: Int,
        duracion: Int,
        ejercicios: List<EjercicioRealizado>
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = "", registradoExitoso = false) }
            try {
                val token = tokenManager.getToken().first() ?: ""
                val body = RegistrarHistorialRequest(userId, tipoRutina, idRutina, duracion, ejercicios)
                val response = RetrofitClient.progresoApi.registrarEntrenamiento("Bearer $token", body)
                if (response.ok) {
                    _uiState.update { it.copy(isLoading = false, registradoExitoso = true) }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = response.message ?: "Error al registrar") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error de conexión") }
            }
        }
    }

    fun resetRegistrado() {
        _uiState.update { it.copy(registradoExitoso = false, error = "") }
    }
}
