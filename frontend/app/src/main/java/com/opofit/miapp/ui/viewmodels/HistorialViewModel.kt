package com.opofit.miapp.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.opofit.miapp.data.api.RetrofitClient
import com.opofit.miapp.data.local.TokenManager
import com.opofit.miapp.data.responsemodels.EjercicioRealizado
import com.opofit.miapp.data.responsemodels.PuntoEvolucion
import com.opofit.miapp.data.responsemodels.RecordRotoItem
import com.opofit.miapp.data.responsemodels.RegistrarHistorialRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException

class HistorialViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(getApplication())

    data class UltimoEntrenoRegistrado(
        val idHistorial: Int,
        val titulo: String,
        val duracionMin: Int,
        val ejerciciosCount: Int
    )

    data class HistorialUiState(
        val isLoading: Boolean = false,
        val error: String = "",
        val evolucion: List<PuntoEvolucion> = emptyList(),
        val registradoExitoso: Boolean = false,
        val recordsRotos: List<RecordRotoItem> = emptyList(),
        val ultimoEntreno: UltimoEntrenoRegistrado? = null
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
                val msg = when (e) {
                    is HttpException -> parseHttpError(e)
                    else -> e.message
                } ?: "Error de conexión"
                _uiState.update { it.copy(isLoading = false, error = msg) }
            }
        }
    }

    fun registrarEntrenamiento(
        userId: Int,
        tipoRutina: String,
        idRutina: Int,
        duracion: Int,
        ejercicios: List<EjercicioRealizado>,
        gpsActividadUuid: String? = null,
        tituloRutina: String? = null
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = "",
                    registradoExitoso = false,
                    recordsRotos = emptyList(),
                    ultimoEntreno = null
                )
            }
            try {
                val token = tokenManager.getToken().first() ?: ""
                val body = RegistrarHistorialRequest(
                    userId,
                    tipoRutina,
                    idRutina,
                    duracion,
                    ejercicios,
                    gpsActividadUuid = gpsActividadUuid
                )
                val response = RetrofitClient.progresoApi.registrarEntrenamiento("Bearer $token", body)
                if (response.ok) {
                    val idHistorial = response.id
                    val ultimo = if (idHistorial != null && idHistorial > 0) {
                        UltimoEntrenoRegistrado(
                            idHistorial = idHistorial,
                            titulo = tituloRutina?.trim().orEmpty().ifBlank { "Entrenamiento" },
                            duracionMin = duracion,
                            ejerciciosCount = ejercicios.size
                        )
                    } else null
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            registradoExitoso = true,
                            recordsRotos = response.recordsRotos.orEmpty(),
                            ultimoEntreno = ultimo
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = response.msg ?: response.message ?: "Error al registrar"
                        )
                    }
                }
            } catch (e: Exception) {
                val msg = when (e) {
                    is HttpException -> parseHttpError(e)
                    else -> e.message
                } ?: "Error de conexión"
                _uiState.update { it.copy(isLoading = false, error = msg) }
            }
        }
    }

    private fun parseHttpError(e: HttpException): String {
        val serverMsg = try {
            val rawBody = e.response()?.errorBody()?.string()
            if (!rawBody.isNullOrEmpty()) JSONObject(rawBody).optString("msg", "") else ""
        } catch (_: Exception) { "" }

        return when {
            serverMsg.isNotEmpty() -> serverMsg
            e.code() == 409 -> "Ya has registrado este entrenamiento hoy. ¡Mañana más!"
            e.code() == 401 -> "Sesión inválida. Inicia sesión de nuevo."
            e.code() == 400 -> "Datos inválidos. Revisa los campos."
            e.code() >= 500 -> "Error del servidor. Inténtalo más tarde."
            else -> "Error (${e.code()})"
        }
    }

    fun resetRegistrado() {
        _uiState.update {
            it.copy(registradoExitoso = false, error = "", recordsRotos = emptyList(), ultimoEntreno = null)
        }
    }

    fun clearRecordsCelebration() {
        _uiState.update { it.copy(recordsRotos = emptyList()) }
    }
}
