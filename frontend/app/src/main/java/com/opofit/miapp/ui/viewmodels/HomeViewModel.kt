package com.opofit.miapp.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.opofit.miapp.data.api.RetrofitClient
import com.opofit.miapp.data.local.TokenManager
import com.opofit.miapp.data.responsemodels.DashboardResumen
import com.opofit.miapp.data.responsemodels.FeedActividadItem
import com.opofit.miapp.utils.ApiErrorParser
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
        val resumen: DashboardResumen? = null,
        val feedAmigos: List<FeedActividadItem> = emptyList()
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
                    var feed = emptyList<FeedActividadItem>()
                    try {
                        val fr = RetrofitClient.amigosApi.feed("Bearer $token")
                        if (fr.ok) feed = fr.data.orEmpty().take(5)
                    } catch (_: Exception) { }
                    _uiState.update { it.copy(loading = false, resumen = resp.data, feedAmigos = feed) }
                } else {
                    _uiState.update {
                        it.copy(loading = false, error = resp.msg ?: "No se pudo cargar el resumen")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(loading = false, error = ApiErrorParser.message(e))
                }
            }
        }
    }

    fun refresh(oposicionId: Int) = cargarResumen(oposicionId, force = true)
}
