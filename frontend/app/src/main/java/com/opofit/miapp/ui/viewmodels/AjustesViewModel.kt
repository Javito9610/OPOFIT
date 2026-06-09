package com.opofit.miapp.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.opofit.miapp.data.api.RetrofitClient
import com.opofit.miapp.data.local.TokenManager
import com.opofit.miapp.data.responsemodels.ActualizarAjustesRequest
import com.opofit.miapp.data.responsemodels.MaterialDisponibleItem
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
        val darkMode: Boolean = false,
        val horaRecordatorio: Int = 18,
        val recordatorioActivo: Boolean = true,
        // v7-doctorado: material disponible para que la IA y los entrenos
        // libres no propongan ejercicios que el usuario no puede hacer.
        val materialCatalogo: List<MaterialDisponibleItem> = emptyList(),
        val materialSeleccionado: Set<String> = emptySet()
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
        // Carga remota: unidades + hora del recordatorio + activo. Antes faltaba esto y
        // los toggles del recordatorio salían siempre con defaults aunque el usuario los
        // hubiera cambiado. Por eso parecía que "el recordatorio no hacía nada".
        cargarAjustesRemotos()
    }

    private fun cargarAjustesRemotos() {
        viewModelScope.launch {
            try {
                val token = tokenManager.getToken().first().orEmpty()
                if (token.isBlank()) return@launch
                val resp = RetrofitClient.usuarioApi.obtenerAjustes("Bearer $token")
                val d = resp.data ?: return@launch
                _uiState.update {
                    it.copy(
                        unidadPeso = d.unidadPeso,
                        unidadDistancia = d.unidadDistancia,
                        horaRecordatorio = d.horaRecordatorio,
                        recordatorioActivo = d.recordatorioActivo,
                        materialSeleccionado = d.materialDisponible.toSet()
                    )
                }
                // Cargamos el catálogo de material (lista cerrada) para los checkboxes.
                try {
                    val mat = RetrofitClient.ejerciciosApi.listarMaterial("Bearer $token")
                    if (mat.ok && mat.data.isNotEmpty()) {
                        _uiState.update { it.copy(materialCatalogo = mat.data) }
                    }
                } catch (_: Exception) { /* opcional */ }
            } catch (_: Exception) { /* silencioso: defaults siguen siendo válidos */ }
        }
    }

    /** Toggle de un item de material en la selección del usuario. */
    fun toggleMaterial(codigo: String) {
        _uiState.update { st ->
            val nueva = st.materialSeleccionado.toMutableSet()
            if (codigo in nueva) nueva.remove(codigo) else nueva.add(codigo)
            // Si elige GIMNASIO_COMPLETO, lo dejamos solo (implica todo).
            // Si elige NADA, lo dejamos solo (excluye al resto).
            val final = when {
                codigo == "GIMNASIO_COMPLETO" && codigo in nueva -> setOf("GIMNASIO_COMPLETO")
                codigo == "NADA" && codigo in nueva -> setOf("NADA")
                "GIMNASIO_COMPLETO" in nueva && codigo != "GIMNASIO_COMPLETO" -> nueva - "GIMNASIO_COMPLETO"
                "NADA" in nueva && codigo != "NADA" -> nueva - "NADA"
                else -> nueva
            }
            st.copy(materialSeleccionado = final)
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            tokenManager.setDarkMode(enabled)
            _uiState.update { it.copy(darkMode = enabled) }
        }
    }

    fun guardarAjustes(
        userId: Int,
        unidadPeso: String,
        unidadDistancia: String,
        horaRecordatorio: Int = 18,
        recordatorioActivo: Boolean = true
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = "", guardadoExitoso = false) }
            try {
                val token = tokenManager.getToken().first() ?: ""
                // Mandamos la selección de material para que el backend la guarde
                // y la IA filtre planes en consecuencia.
                val materialSel = _uiState.value.materialSeleccionado.toList()
                val body = ActualizarAjustesRequest(
                    userId = userId,
                    unidadPeso = unidadPeso,
                    unidadDistancia = unidadDistancia,
                    horaRecordatorio = horaRecordatorio,
                    recordatorioActivo = recordatorioActivo,
                    materialDisponible = materialSel.ifEmpty { null }
                )
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
