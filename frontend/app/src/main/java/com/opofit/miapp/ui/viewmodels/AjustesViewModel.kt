package com.opofit.miapp.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.opofit.miapp.data.api.RetrofitClient
import com.opofit.miapp.data.local.TokenManager
import com.opofit.miapp.data.responsemodels.ActualizarAjustesRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AjustesViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(getApplication())

    data class AjustesUiState(
        val isLoading: Boolean = false,
        val error: String = "",
        val guardadoExitoso: Boolean = false,
        val unidadPeso: String = "kg",
        val unidadDistancia: String = "km"
    )

    private val _uiState = MutableStateFlow(AjustesUiState())
    val uiState: StateFlow<AjustesUiState> = _uiState.asStateFlow()

    fun guardarAjustes(userId: Int, unidadPeso: String, unidadDistancia: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = "", guardadoExitoso = false) }
            try {
                val token = tokenManager.getToken().first() ?: ""
                val body = ActualizarAjustesRequest(userId, unidadPeso, unidadDistancia)
                val response = RetrofitClient.usuarioApi.actualizarAjustes("Bearer $token", body)
                if (response.ok) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            guardadoExitoso = true,
                            unidadPeso = unidadPeso,
                            unidadDistancia = unidadDistancia
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = response.message ?: "Error al guardar ajustes") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error de conexión") }
            }
        }
    }

    fun resetGuardado() {
        _uiState.update { it.copy(guardadoExitoso = false, error = "") }
    }
}
