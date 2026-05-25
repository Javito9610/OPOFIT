package com.opofit.miapp.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.opofit.miapp.data.api.RetrofitClient
import com.opofit.miapp.data.local.TokenManager
import com.opofit.miapp.data.responsemodels.BloqueRutina
import com.opofit.miapp.data.responsemodels.Oposicion
import com.opofit.miapp.data.responsemodels.PlanCalendario
import com.opofit.miapp.data.responsemodels.PlanSemanal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import com.opofit.miapp.utils.ApiErrorParser
import kotlinx.coroutines.launch

class RutinasViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(getApplication())

    data class RutinasUiState(
        val isLoading: Boolean = false,
        val error: String = "",
        val notaActual: String = "",
        val nivelAsignado: String = "",
        val rutinaCompleta: List<BloqueRutina> = emptyList(),
        val planSemanal: PlanSemanal? = null,
        val calendario: PlanCalendario? = null,
        val oposiciones: List<Oposicion> = emptyList(),
        val oposicionesLoading: Boolean = false,
        val pruebasFaltantes: Int? = null,
        val totalPruebas: Int? = null,
        val pruebasCompletadas: Int? = null,
        val msgPremium: String? = null,
        val nivelPremiumBloqueado: Boolean = false
    )

    private val _uiState = MutableStateFlow(RutinasUiState())
    val uiState: StateFlow<RutinasUiState> = _uiState.asStateFlow()

    fun cargarRutina(userId: Int, oposicionId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = "") }
            try {
                val token = tokenManager.getToken().first() ?: ""
                val response = RetrofitClient.rutinasApi.getMiEntrenamiento(
                    "Bearer $token", userId, oposicionId
                )
                if (response.ok && response.data != null) {
                    val data = response.data
                    val bloqueadoPorPruebas = (data.pruebasFaltantes ?: 0) > 0
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            notaActual = data.notaActual,
                            nivelAsignado = data.nivelAsignado,
                            rutinaCompleta = data.rutinaCompleta,
                            planSemanal = data.planSemanal,
                            pruebasFaltantes = data.pruebasFaltantes,
                            totalPruebas = data.totalPruebas,
                            pruebasCompletadas = data.pruebasCompletadas,
                            msgPremium = data.msgPremium,
                            nivelPremiumBloqueado = data.nivelPremiumBloqueado == true,
                            error = if (bloqueadoPorPruebas) "" else (response.msg ?: "")
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = response.msg ?: "No se pudo cargar la rutina") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = ApiErrorParser.message(e)) }
            }
        }
    }

    fun cargarCalendario(userId: Int, oposicionId: Int, year: Int, month: Int) {
        viewModelScope.launch {
            try {
                val token = tokenManager.getToken().first() ?: ""
                val response = RetrofitClient.planesApi.getCalendario(
                    "Bearer $token", oposicionId, year, month
                )
                if (response.ok) {
                    _uiState.update { it.copy(calendario = response.data) }
                }
            } catch (_: Exception) { }
        }
    }

    fun cargarOposiciones() {
        viewModelScope.launch {
            _uiState.update { it.copy(oposicionesLoading = true) }
            try {
                val token = tokenManager.getToken().first() ?: ""
                val response = RetrofitClient.oposicionesApi.getOposiciones("Bearer $token")
                if (response.ok && response.data != null) {
                    _uiState.update { it.copy(oposicionesLoading = false, oposiciones = response.data) }
                } else {
                    _uiState.update { it.copy(oposicionesLoading = false, error = "No se pudieron cargar las oposiciones") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(oposicionesLoading = false, error = e.message ?: "Error al cargar oposiciones") }
            }
        }
    }
}
