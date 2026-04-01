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
import org.json.JSONObject
import retrofit2.HttpException

class AjustesViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(getApplication())

    data class AjustesUiState(
        val isLoading: Boolean = false,
        val error: String = "",
        val guardadoExitoso: Boolean = false,
        val eliminandoCuenta: Boolean = false,
        val cuentaEliminada: Boolean = false,
        val unidadPeso: String = "kg",
        val unidadDistancia: String = "km",
        val darkMode: Boolean = false
    )

    private val _uiState = MutableStateFlow(AjustesUiState())
    val uiState: StateFlow<AjustesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            tokenManager.getDarkMode().collect { enabled ->
                _uiState.update { it.copy(darkMode = enabled) }
            }
        }
        viewModelScope.launch {
            tokenManager.getUnitPeso().collect { u ->
                if (!u.isNullOrBlank()) _uiState.update { it.copy(unidadPeso = u) }
            }
        }
        viewModelScope.launch {
            tokenManager.getUnitDistancia().collect { u ->
                if (!u.isNullOrBlank()) _uiState.update { it.copy(unidadDistancia = u) }
            }
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            tokenManager.setDarkMode(enabled)
            _uiState.update { it.copy(darkMode = enabled) }
        }
    }

    fun guardarAjustes(userId: Int, unidadPeso: String, unidadDistancia: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = "", guardadoExitoso = false) }
            try {
                val token = tokenManager.getToken().first() ?: ""
                val body = ActualizarAjustesRequest(userId, unidadPeso, unidadDistancia)
                val response = RetrofitClient.usuarioApi.actualizarAjustes("Bearer $token", body)
                if (response.ok) {
                    tokenManager.saveUnitPeso(unidadPeso)
                    tokenManager.saveUnitDistancia(unidadDistancia)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            guardadoExitoso = true,
                            unidadPeso = unidadPeso,
                            unidadDistancia = unidadDistancia
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = response.msg ?: response.message ?: "Error al guardar ajustes"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error de conexión") }
            }
        }
    }

    fun resetGuardado() {
        _uiState.update { it.copy(guardadoExitoso = false, error = "") }
    }

    fun eliminarCuenta() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(eliminandoCuenta = true, error = "", cuentaEliminada = false)
            }
            try {
                val token = tokenManager.getToken().first() ?: ""
                val response = RetrofitClient.usuarioApi.eliminarCuenta("Bearer $token")
                if (response.ok) {
                    _uiState.update {
                        it.copy(eliminandoCuenta = false, cuentaEliminada = true)
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            eliminandoCuenta = false,
                            error = response.msg ?: response.message ?: "No se pudo eliminar la cuenta"
                        )
                    }
                }
            } catch (e: HttpException) {
                val serverMsg = try {
                    val raw = e.response()?.errorBody()?.string()
                    if (!raw.isNullOrEmpty()) JSONObject(raw).optString("msg", "") else ""
                } catch (_: Exception) {
                    ""
                }
                _uiState.update {
                    it.copy(
                        eliminandoCuenta = false,
                        error = serverMsg.ifEmpty {
                            when (e.code()) {
                                401 -> "Sesión no válida. Vuelve a iniciar sesión."
                                else -> "No se pudo eliminar la cuenta"
                            }
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        eliminandoCuenta = false,
                        error = e.message ?: "Error de conexión"
                    )
                }
            }
        }
    }

    fun clearCuentaEliminadaFlag() {
        _uiState.update { it.copy(cuentaEliminada = false) }
    }

    fun clearMensajeError() {
        _uiState.update { it.copy(error = "") }
    }
}
