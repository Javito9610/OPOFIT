package com.opofit.miapp.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.opofit.miapp.data.api.RetrofitClient
import com.opofit.miapp.data.local.TokenManager
import com.opofit.miapp.data.responsemodels.ActualizarPerfilRequest
import com.opofit.miapp.data.responsemodels.SubirAvatarRequest
import com.opofit.miapp.data.responsemodels.InfoPrueba
import com.opofit.miapp.data.responsemodels.MarcaActualizar
import com.opofit.miapp.data.responsemodels.MarcaUsuario
import com.opofit.miapp.data.responsemodels.RequisitoOposicion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PerfilViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(getApplication())

    data class PerfilUiState(
        val isLoading: Boolean = false,
        val error: String = "",
        val guardadoExitoso: Boolean = false,
        val nuevoNivel: String? = null,
        val nuevaNota: String? = null,
        val requisitos: List<RequisitoOposicion> = emptyList(),
        val infoPruebas: List<InfoPrueba> = emptyList(),
        val marcasUsuario: List<MarcaUsuario> = emptyList(),
        val diasEntrenoSemana: Int? = null
    )

    private val _uiState = MutableStateFlow(PerfilUiState())
    val uiState: StateFlow<PerfilUiState> = _uiState.asStateFlow()

    /** Lee /api/user/perfil para obtener campos no cacheados (como días/semana). */
    fun cargarPreferenciasPerfil() {
        viewModelScope.launch {
            try {
                val token = tokenManager.getToken().first() ?: ""
                val response = RetrofitClient.usuarioApi.obtenerPerfil("Bearer $token")
                if (response.ok && response.data != null) {
                    _uiState.update { it.copy(diasEntrenoSemana = response.data.diasEntrenoSemana) }
                }
            } catch (_: Exception) {
                // No bloqueamos el flujo si el perfil no carga: aplicamos default 5.
            }
        }
    }

    fun cargarRequisitos(oposicionId: Int, genero: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = "") }
            try {
                val token = tokenManager.getToken().first() ?: ""
                val response = RetrofitClient.oposicionesApi.getRequisitos(
                    "Bearer $token", oposicionId, genero
                )
                if (response.ok) {
                    _uiState.update { it.copy(isLoading = false, requisitos = response.data ?: emptyList()) }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "No se pudieron cargar los requisitos") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error de conexión") }
            }
        }
    }

    fun cargarInfoPruebas(oposicionId: Int, genero: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = "") }
            try {
                val token = tokenManager.getToken().first() ?: ""
                val response = RetrofitClient.infoPruebasApi.getInfoPruebas(
                    "Bearer $token", oposicionId, genero
                )
                if (response.ok) {
                    _uiState.update { it.copy(isLoading = false, infoPruebas = response.data ?: emptyList()) }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "No se pudo cargar información") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error de conexión") }
            }
        }
    }

    fun cargarMarcasUsuario(userId: Int, oposicionId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = "") }
            try {
                val token = tokenManager.getToken().first() ?: ""
                val response = RetrofitClient.infoPruebasApi.getMarcasUsuario(
                    "Bearer $token", userId, oposicionId
                )
                if (response.ok) {
                    _uiState.update { it.copy(isLoading = false, marcasUsuario = response.data ?: emptyList()) }
                } else {
                    _uiState.update { it.copy(isLoading = false, marcasUsuario = emptyList()) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, marcasUsuario = emptyList()) }
            }
        }
    }

    fun actualizarPerfil(
        userId: Int,
        peso: Double?,
        altura: Double?,
        oposicionId: Int?,
        nuevasMarcas: List<MarcaActualizar>,
        nombre: String? = null,
        avatarUrl: String? = null,
        diasEntrenoSemana: Int? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = "", guardadoExitoso = false) }
            try {
                val token = tokenManager.getToken().first() ?: ""
                val body = ActualizarPerfilRequest(
                    userId = userId,
                    peso = peso,
                    altura = altura,
                    oposicionId = oposicionId,
                    nuevasMarcas = nuevasMarcas,
                    nombre = nombre,
                    avatarUrl = avatarUrl,
                    diasEntrenoSemana = diasEntrenoSemana
                )
                val response = RetrofitClient.usuarioApi.actualizarPerfil("Bearer $token", body)
                if (response.ok) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            guardadoExitoso = true,
                            nuevoNivel = response.nuevoNivel,
                            nuevaNota = response.nuevaNota
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = response.msg ?: response.message ?: "Error al actualizar perfil"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error de conexión") }
            }
        }
    }

    fun resetGuardado() {
        _uiState.update { it.copy(guardadoExitoso = false, nuevoNivel = null, nuevaNota = null, error = "") }
    }

    fun subirAvatar(imagenBase64: String, onOk: (String) -> Unit, onFinished: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(error = "") }
            try {
                val token = tokenManager.getToken().first() ?: ""
                val response = RetrofitClient.usuarioApi.subirAvatar(
                    "Bearer $token",
                    SubirAvatarRequest(imagenBase64)
                )
                if (response.ok && !response.avatarUrl.isNullOrBlank()) {
                    onOk(response.avatarUrl)
                } else {
                    _uiState.update {
                        it.copy(error = response.msg ?: "No se pudo subir la foto")
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error de conexión") }
            } finally {
                onFinished()
            }
        }
    }
}
