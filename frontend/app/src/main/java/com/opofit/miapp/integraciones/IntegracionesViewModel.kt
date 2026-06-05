package com.opofit.miapp.integraciones

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.opofit.miapp.data.api.EstadoIntegraciones
import com.opofit.miapp.data.api.HcActivityPayload
import com.opofit.miapp.data.api.HcImportarRequest
import com.opofit.miapp.data.api.RetrofitClient
import com.opofit.miapp.data.local.TokenManager
import com.opofit.miapp.utils.ApiErrorParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class IntegracionesViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(getApplication())
    private val api = RetrofitClient.integracionesApi
    private val hc = HealthConnectManager.get(application)
    private val gf = GoogleFitManager.get(application)

    data class UiState(
        val loading: Boolean = false,
        val message: String? = null,
        val estado: EstadoIntegraciones = EstadoIntegraciones(),
        val hcAvailability: HealthConnectManager.Availability = HealthConnectManager.Availability.NOT_SUPPORTED,
        val hcConnected: Boolean = false,
        val gfConnected: Boolean = false,
        val lastSyncImported: Int = 0
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, message = null) }
            try {
                val token = tokenManager.getToken().first().orEmpty()
                val resp = api.estado("Bearer $token")
                val hcAvail = hc.availability()
                val hcConnected = if (hcAvail == HealthConnectManager.Availability.AVAILABLE) {
                    hc.hasAllPermissions()
                } else false
                val gfConnected = gf.hasPermissions()
                _uiState.update {
                    it.copy(
                        loading = false,
                        estado = resp.data ?: EstadoIntegraciones(),
                        hcAvailability = hcAvail,
                        hcConnected = hcConnected,
                        gfConnected = gfConnected
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, message = ApiErrorParser.message(e)) }
            }
        }
    }

    fun syncStrava() = withSpinner {
        val token = tokenManager.getToken().first().orEmpty()
        val resp = RetrofitClient.integracionesApi.syncStrava("Bearer $token")
        if (resp.ok) "Strava: ${resp.data?.importadas ?: 0} importadas, ${resp.data?.saltadas ?: 0} ya estaban"
        else resp.msg ?: "Error sincronizando Strava"
    }

    fun disconnectStrava() = withSpinner {
        val token = tokenManager.getToken().first().orEmpty()
        RetrofitClient.integracionesApi.disconnectStrava("Bearer $token")
        "Strava desconectado"
    }

    fun syncPolar() = withSpinner {
        val token = tokenManager.getToken().first().orEmpty()
        val resp = RetrofitClient.integracionesApi.syncPolar("Bearer $token")
        if (resp.ok) "Polar: ${resp.data?.importadas ?: 0} importadas"
        else resp.msg ?: "Error sincronizando Polar"
    }

    fun disconnectPolar() = withSpinner {
        val token = tokenManager.getToken().first().orEmpty()
        RetrofitClient.integracionesApi.disconnectPolar("Bearer $token")
        "Polar desconectado"
    }

    fun syncHealthConnect() = withSpinner {
        val res = hc.syncLastDays(30)
        if (res.error != null) {
            res.error
        } else {
            // Sube a backend además del repo local.
            val token = tokenManager.getToken().first().orEmpty()
            val repo = com.opofit.miapp.gps.data.GpsRepository.get(getApplication())
            val recientes = repo.listAll()
                .filter { it.id.startsWith("hc_") }
                .take(60)
                .map {
                    HcActivityPayload(
                        externalId = it.id,
                        tipo = it.type.name,
                        startedAtMs = it.startedAtMs,
                        endedAtMs = it.endedAtMs,
                        durationSec = it.durationSec,
                        movingSec = it.movingSec,
                        distanceM = it.distanceM,
                        avgSpeedMps = it.avgSpeedMps,
                        maxSpeedMps = it.maxSpeedMps,
                        avgPaceSecPerKm = it.avgPaceSecPerKm,
                        minPaceSecPerKm = it.minPaceSecPerKm,
                        maxPaceSecPerKm = it.maxPaceSecPerKm,
                        elevationGainM = it.elevationGainM,
                        elevationMinM = it.elevationMinM,
                        elevationMaxM = it.elevationMaxM,
                        avgCadenceSpm = it.avgCadenceSpm
                    )
                }
            if (recientes.isNotEmpty()) {
                runCatching {
                    RetrofitClient.integracionesApi.importarHealthConnect(
                        "Bearer $token",
                        HcImportarRequest(recientes)
                    )
                }
            }
            "Health Connect: ${res.importadas} importadas, ${res.saltadas} ya estaban"
        }
    }

    fun syncGoogleFit() = withSpinner {
        val res = gf.syncLastDays(30)
        if (res.error != null) {
            res.error
        } else {
            val token = tokenManager.getToken().first().orEmpty()
            val repo = com.opofit.miapp.gps.data.GpsRepository.get(getApplication())
            val recientes = repo.listAll()
                .filter { it.id.startsWith("gf_") }
                .take(60)
                .map {
                    HcActivityPayload(
                        externalId = it.id,
                        tipo = it.type.name,
                        startedAtMs = it.startedAtMs,
                        endedAtMs = it.endedAtMs,
                        durationSec = it.durationSec,
                        movingSec = it.movingSec,
                        distanceM = it.distanceM,
                        avgSpeedMps = it.avgSpeedMps,
                        maxSpeedMps = it.maxSpeedMps,
                        avgPaceSecPerKm = it.avgPaceSecPerKm,
                        minPaceSecPerKm = it.minPaceSecPerKm,
                        maxPaceSecPerKm = it.maxPaceSecPerKm,
                        elevationGainM = it.elevationGainM,
                        elevationMinM = it.elevationMinM,
                        elevationMaxM = it.elevationMaxM,
                        avgCadenceSpm = it.avgCadenceSpm
                    )
                }
            if (recientes.isNotEmpty()) {
                runCatching {
                    RetrofitClient.integracionesApi.importarHealthConnect(
                        "Bearer $token",
                        HcImportarRequest(recientes)
                    )
                }
            }
            "Google Fit: ${res.importadas} importadas, ${res.saltadas} ya estaban"
        }
    }

    fun consumeMessage() = _uiState.update { it.copy(message = null) }

    private fun withSpinner(block: suspend () -> String) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, message = null) }
            val msg = try {
                block()
            } catch (e: Exception) {
                ApiErrorParser.message(e)
            }
            _uiState.update { it.copy(loading = false, message = msg) }
            refresh()
        }
    }
}
