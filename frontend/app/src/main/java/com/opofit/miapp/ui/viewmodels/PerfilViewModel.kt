package com.opofit.miapp.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.opofit.miapp.data.api.RetrofitClient
import com.opofit.miapp.data.local.TokenManager
import com.opofit.miapp.data.responsemodels.ActualizarPerfilRequest
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
        val marcasUsuario: List<MarcaUsuario> = emptyList()
    )

    private val _uiState = MutableStateFlow(PerfilUiState())
    val uiState: StateFlow<PerfilUiState> = _uiState.asStateFlow()

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
        peso: Double,
        altura: Double,
        oposicionId: Int,
        nuevasMarcas: List<MarcaActualizar>
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = "", guardadoExitoso = false) }
            try {
                val token = tokenManager.getToken().first() ?: ""
                val body = ActualizarPerfilRequest(userId, peso, altura, oposicionId, nuevasMarcas)
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
                    _uiState.update { it.copy(isLoading = false, error = response.message ?: "Error al actualizar perfil") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error de conexión") }
            }
        }
    }

    fun resetGuardado() {
        _uiState.update { it.copy(guardadoExitoso = false, nuevoNivel = null, nuevaNota = null, error = "") }
    }
}
