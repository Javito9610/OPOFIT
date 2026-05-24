package com.opofit.miapp.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.opofit.miapp.data.api.RetrofitClient
import com.opofit.miapp.data.local.TokenManager
import com.opofit.miapp.data.responsemodels.DashboardResumen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(getApplication())

    data class HomeUiState(
        val loading: Boolean = true,
        val error: String = "",
        val resumen: DashboardResumen? = null
    )

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun cargarResumen(oposicionId: Int, force: Boolean = false) {
        if (!force && _uiState.value.resumen != null && !_uiState.value.loading) return
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = "") }
            try {
                val token = tokenManager.getToken().first().orEmpty()
                if (token.isBlank()) {
                    _uiState.update { it.copy(loading = false, error = "Sesión no válida") }
                    return@launch
                }
                val resp = RetrofitClient.dashboardApi.resumen("Bearer $token", oposicionId)
                if (resp.ok && resp.data != null) {
                    _uiState.update { it.copy(loading = false, resumen = resp.data) }
                } else {
                    _uiState.update {
                        it.copy(loading = false, error = resp.msg ?: "No se pudo cargar el resumen")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(loading = false, error = e.message ?: "Error de conexión")
                }
            }
        }
    }

    fun refresh(oposicionId: Int) = cargarResumen(oposicionId, force = true)
}
