package com.opofit.miapp.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.opofit.miapp.data.api.RetrofitClient
import com.opofit.miapp.data.local.TokenManager
import com.opofit.miapp.data.responsemodels.CrearRutinaLibreRequest
import com.opofit.miapp.data.responsemodels.EjercicioLibreItem
import com.opofit.miapp.data.responsemodels.RutinaLibre
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.opofit.miapp.utils.ApiErrorParser
import org.json.JSONObject
import retrofit2.HttpException

class RutinasLibresViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(getApplication())

    data class RutinasLibresUiState(
        val isLoading: Boolean = false,
        val error: String = "",
        val rutinas: List<RutinaLibre> = emptyList(),
        val guardadoExitoso: Boolean = false
    )

    private val _uiState = MutableStateFlow(RutinasLibresUiState())
    val uiState: StateFlow<RutinasLibresUiState> = _uiState.asStateFlow()

    fun cargarRutinas(userId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = "") }
            try {
                val token = tokenManager.getToken().first() ?: ""
                val response = RetrofitClient.rutinasLibresApi.getRutinasUsuario("Bearer $token", userId)
                if (response.ok) {
                    _uiState.update { it.copy(isLoading = false, rutinas = response.data ?: emptyList()) }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "No se pudieron cargar las rutinas") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error de conexión") }
            }
        }
    }

    fun crearRutina(userId: Int, nombre: String, ejercicios: List<EjercicioLibreItem>) {
        if (nombre.isBlank()) {
            _uiState.update { it.copy(error = "El nombre de la rutina no puede estar vacío") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = "", guardadoExitoso = false) }
            try {
                val token = tokenManager.getToken().first() ?: ""
                val body = CrearRutinaLibreRequest(userId, nombre, ejercicios)
                val response = RetrofitClient.rutinasLibresApi.crearRutina("Bearer $token", body)
                if (response.ok) {
                    _uiState.update { it.copy(isLoading = false, guardadoExitoso = true) }
                    cargarRutinas(userId)
                } else {
                    _uiState.update { it.copy(isLoading = false, error = response.msg ?: response.message ?: "Error al crear rutina") }
                }
            } catch (e: HttpException) {
                val serverMsg = try {
                    val raw = e.response()?.errorBody()?.string().orEmpty()
                    if (raw.isNotEmpty()) JSONObject(raw).optString("msg", "") else ""
                } catch (_: Exception) {
                    ""
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = serverMsg.ifEmpty { e.message ?: "Error al crear rutina" }
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error de conexión") }
            }
        }
    }

    fun resetGuardado() {
        _uiState.update { it.copy(guardadoExitoso = false, error = "") }
    }

    fun eliminarRutina(userId: Int, idRutina: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = "") }
            try {
                val token = tokenManager.getToken().first() ?: ""
                val response = RetrofitClient.rutinasLibresApi.eliminarRutina("Bearer $token", userId, idRutina)
                if (response.ok) {
                    cargarRutinas(userId)
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = response.msg ?: response.message ?: "Error al eliminar rutina"
                        )
                    }
                }
            } catch (e: HttpException) {
                _uiState.update { it.copy(isLoading = false, error = ApiErrorParser.message(e)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error de conexión") }
            }
        }
    }

    fun limpiarError() {
        _uiState.update { it.copy(error = "") }
    }
}
