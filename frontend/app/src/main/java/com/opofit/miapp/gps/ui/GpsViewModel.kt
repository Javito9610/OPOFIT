package com.opofit.miapp.gps.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.opofit.miapp.data.api.GpsActivityPayload
import com.opofit.miapp.data.api.RetrofitClient
import com.opofit.miapp.data.local.TokenManager
import com.opofit.miapp.gps.data.GpsRepository
import com.opofit.miapp.gps.model.ActivitySummary
import com.opofit.miapp.gps.model.ActivityType
import com.opofit.miapp.gps.model.GpsTrackingState
import com.opofit.miapp.gps.service.GpsLastResult
import com.opofit.miapp.gps.service.GpsTracker
import com.opofit.miapp.gps.service.GpsTrackingService
import com.opofit.miapp.gps.service.HrBleManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GpsViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = GpsRepository.get(application)
    private val tokenManager = TokenManager(application)
    private val hrBle = HrBleManager.get(application)

    val hrState: StateFlow<HrBleManager.State> = hrBle.state
    val hrFound: StateFlow<List<HrBleManager.FoundDevice>> = hrBle.found

    data class HistoryState(
        val loading: Boolean = false,
        val items: List<ActivitySummary> = emptyList()
    )

    private val _history = MutableStateFlow(HistoryState())
    val history: StateFlow<HistoryState> = _history.asStateFlow()

    private val _selectedType = MutableStateFlow(ActivityType.RUN)
    val selectedType: StateFlow<ActivityType> = _selectedType.asStateFlow()

    val tracking: StateFlow<GpsTrackingState> = GpsTracker.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, GpsTracker.state.value)

    private val _lastSaved = MutableStateFlow<ActivitySummary?>(null)
    val lastSaved: StateFlow<ActivitySummary?> = _lastSaved.asStateFlow()

    init {
        loadHistory()
    }

    fun selectType(t: ActivityType) {
        _selectedType.value = t
    }

    fun startTracking() {
        GpsTrackingService.start(getApplication(), _selectedType.value)
    }

    fun pause() {
        GpsTrackingService.pause(getApplication())
    }

    fun resume() {
        GpsTrackingService.resume(getApplication())
    }

    /** Stops the service and persists the summary. Returns the saved activity id. */
    fun stopAndSave(): String? {
        val summary = GpsTracker.finish() ?: run {
            GpsTrackingService.stop(getApplication())
            return null
        }
        GpsTrackingService.stop(getApplication())
        if (summary.points.size >= 2 && summary.distanceM >= 25.0) {
            repo.save(summary)
            _lastSaved.value = summary
            GpsLastResult.set(summary)
            loadHistory()
            syncToBackend(summary)
            return summary.id
        }
        return null
    }

    private fun syncToBackend(summary: ActivitySummary) {
        viewModelScope.launch {
            try {
                val token = tokenManager.getToken().first().orEmpty()
                if (token.isBlank()) return@launch
                val payload = GpsActivityPayload(
                    id = summary.id,
                    type = summary.type.name,
                    startedAtMs = summary.startedAtMs,
                    endedAtMs = summary.endedAtMs,
                    durationSec = summary.durationSec,
                    movingSec = summary.movingSec,
                    distanceM = summary.distanceM,
                    avgSpeedMps = summary.avgSpeedMps,
                    maxSpeedMps = summary.maxSpeedMps,
                    avgPaceSecPerKm = summary.avgPaceSecPerKm,
                    minPaceSecPerKm = summary.minPaceSecPerKm,
                    maxPaceSecPerKm = summary.maxPaceSecPerKm,
                    elevationGainM = summary.elevationGainM,
                    elevationLossM = summary.elevationLossM,
                    elevationMinM = summary.elevationMinM,
                    elevationMaxM = summary.elevationMaxM,
                    avgCadenceSpm = summary.avgCadenceSpm,
                    maxCadenceSpm = summary.maxCadenceSpm,
                    avgHrBpm = summary.avgHrBpm,
                    maxHrBpm = summary.maxHrBpm,
                    minHrBpm = summary.minHrBpm,
                    kcal = summary.kcal,
                    points = summary.points.map { p ->
                        mapOf(
                            "lat" to p.lat,
                            "lng" to p.lng,
                            "altitude" to p.altitude,
                            "speedMps" to p.speedMps,
                            "accuracyM" to p.accuracyM,
                            "cadenceSpm" to p.cadenceSpm,
                            "hrBpm" to p.hrBpm,
                            "timestampMs" to p.timestampMs
                        )
                    },
                    splits = summary.splits.map { s ->
                        mapOf(
                            "km" to s.km,
                            "durationSec" to s.durationSec,
                            "paceSecPerKm" to s.paceSecPerKm,
                            "elevationGainM" to s.elevationGainM,
                            "avgHrBpm" to s.avgHrBpm,
                            "avgCadenceSpm" to s.avgCadenceSpm
                        )
                    },
                    splitsMile = summary.splitsMile.map { s ->
                        mapOf(
                            "mile" to s.mile,
                            "durationSec" to s.durationSec,
                            "paceSecPerMi" to s.paceSecPerMi,
                            "elevationGainM" to s.elevationGainM
                        )
                    },
                    splitsTime = summary.splitsTime.map { s ->
                        mapOf(
                            "index" to s.index,
                            "durationSec" to s.durationSec,
                            "distanceM" to s.distanceM,
                            "avgPaceSecPerKm" to s.avgPaceSecPerKm
                        )
                    },
                    bestSegments = summary.bestSegments.map { b ->
                        mapOf(
                            "label" to b.label,
                            "distanceM" to b.distanceM,
                            "durationSec" to b.durationSec,
                            "paceSecPerKm" to b.paceSecPerKm
                        )
                    }
                )
                val resp = RetrofitClient.gpsApi.guardar("Bearer $token", payload)
                if (resp.ok && resp.data?.idActividad != null) {
                    repo.markSynced(summary.id, resp.data.idActividad)
                }
            } catch (_: Exception) {
                /* mantenemos la actividad solo en local; reintentos quedan para una futura cola. */
            }
        }
    }

    fun discard() {
        GpsTracker.finish()
        GpsTrackingService.stop(getApplication())
    }

    fun startHrScan() = hrBle.startScan()
    fun stopHrScan() = hrBle.stopScan()
    fun connectHr(device: HrBleManager.FoundDevice) = hrBle.connect(device)
    fun disconnectHr() = hrBle.disconnect()
    fun hrManager(): HrBleManager = hrBle

    fun loadHistory() {
        viewModelScope.launch {
            _history.update { it.copy(loading = true) }
            val items = repo.listAll()
            _history.update { it.copy(loading = false, items = items) }
        }
    }

    fun get(id: String): ActivitySummary? = repo.get(id)

    fun delete(id: String) {
        repo.delete(id)
        loadHistory()
        viewModelScope.launch {
            try {
                val token = tokenManager.getToken().first().orEmpty()
                if (token.isNotBlank()) {
                    RetrofitClient.gpsApi.borrar("Bearer $token", id)
                }
            } catch (_: Exception) { }
        }
    }

    fun consumeLastSaved() {
        _lastSaved.value = null
    }
}
