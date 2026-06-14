package com.opofit.miapp.gps.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.opofit.miapp.data.api.GpsActivityPayload
import com.opofit.miapp.data.api.RetrofitClient
import com.opofit.miapp.data.local.TokenManager
import com.opofit.miapp.gps.data.GpsRemoteSync
import com.opofit.miapp.gps.data.GpsRepository
import com.opofit.miapp.gps.model.ActivitySummary
import com.opofit.miapp.gps.model.ActivityType
import com.opofit.miapp.gps.model.GpsTrackingState
import com.opofit.miapp.data.responsemodels.EsfuerzoDesdeActividadRequest
import com.opofit.miapp.data.responsemodels.EsfuerzoSlug
import com.opofit.miapp.gps.service.GpsLastResult
import com.opofit.miapp.gps.service.GpsRecordingContext
import com.opofit.miapp.gps.service.GpsTracker
import com.opofit.miapp.gps.service.GpsTrackingService
import com.opofit.miapp.gps.service.HrBleManager
import com.opofit.miapp.gps.service.RoutePreferences
import com.opofit.miapp.utils.SegmentSlugUtil
import com.opofit.miapp.gps.util.GpxImport
import com.opofit.miapp.gps.util.TcxImport
import com.opofit.miapp.integraciones.EntrenoSyncService
import android.net.Uri
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
    private val hrBle: HrBleManager by lazy {
        try {
            HrBleManager.get(application)
        } catch (_: Exception) {
            HrBleManager.get(application)
        }
    }

    val hrState: StateFlow<HrBleManager.State> = try {
        HrBleManager.get(application).state
    } catch (_: Exception) {
        MutableStateFlow(HrBleManager.State.Idle)
    }
    val hrFound: StateFlow<List<HrBleManager.FoundDevice>> = try {
        HrBleManager.get(application).found
    } catch (_: Exception) {
        MutableStateFlow(emptyList())
    }

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

    private val _importMessage = MutableStateFlow<String?>(null)
    val importMessage: StateFlow<String?> = _importMessage.asStateFlow()

    init {
        loadHistory()
        // El bridge HR↔GpsTracker lo gestiona OpoFitApp via Flows (ver OpoFitApp.onCreate).
        // No se debe llamar setListeners aquí porque sobrescribiría el listener compartido
        // y los datos solo llegarían al último consumidor que llamase.
    }

    fun selectType(t: ActivityType) {
        _selectedType.value = t
    }

    fun startTracking() {
        val type = GpsRecordingContext.consumeType() ?: _selectedType.value
        _selectedType.value = type
        GpsTrackingService.start(getApplication(), type)
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
            viewModelScope.launch { RoutePreferences.clear(getApplication()) }
            return summary.id
        }
        viewModelScope.launch { RoutePreferences.clear(getApplication()) }
        return null
    }

    fun syncToBackend(summary: ActivitySummary) {
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
                registrarSegmentosVirtuales(token, summary)
            } catch (_: Exception) {
                /* mantenemos la actividad solo en local; reintentos quedan para una futura cola. */
            }
        }
    }

    private fun registrarSegmentosVirtuales(token: String, summary: ActivitySummary) {
        viewModelScope.launch {
            try {
                if (summary.bestSegments.isEmpty()) return@launch
                val esfuerzos = summary.bestSegments.mapNotNull { seg ->
                    val slug = SegmentSlugUtil.slugFromLabel(seg.label) ?: return@mapNotNull null
                    EsfuerzoSlug(slug, seg.durationSec * 1000L)
                }
                if (esfuerzos.isEmpty()) return@launch
                RetrofitClient.segmentosApi.desdeActividad(
                    "Bearer $token",
                    EsfuerzoDesdeActividadRequest(summary.id, esfuerzos)
                )
            } catch (_: Exception) { }
        }
    }

    fun discard() {
        GpsTracker.finish()
        GpsTrackingService.stop(getApplication())
    }

    fun startHrScan() = hrBle.startScan(broad = false)

    fun startHrScanBroad() = hrBle.startScan(broad = true)
    fun stopHrScan() = hrBle.stopScan()
    fun connectHr(device: HrBleManager.FoundDevice) = hrBle.connect(device)
    fun disconnectHr() = hrBle.disconnect()
    fun hrManager(): HrBleManager = hrBle
    fun pairedHrDevices(): List<HrBleManager.FoundDevice> = hrBle.pairedDevices()

    fun loadHistory() {
        viewModelScope.launch {
            _history.update { it.copy(loading = true) }
            val local = repo.listAll()
            val merged = try {
                val token = tokenManager.getToken().first().orEmpty()
                if (token.isBlank()) local else syncRemoteActivities("Bearer $token", local)
            } catch (_: Exception) {
                local
            }
            local.filter { it.syncedRemoteId == null }.forEach { syncToBackend(it) }
            _history.update { it.copy(loading = false, items = merged) }
        }
    }

    private suspend fun syncRemoteActivities(token: String, local: List<ActivitySummary>): List<ActivitySummary> {
        val resp = RetrofitClient.gpsApi.listar(token)
        if (!resp.ok || resp.data.isNullOrEmpty()) return local
        val localIds = local.map { it.id }.toSet()
        val merged = local.toMutableList()
        resp.data.take(30).forEach { item ->
            if (item.id !in localIds && repo.get(item.id) == null) {
                val det = RetrofitClient.gpsApi.detalle(token, item.id)
                val summary = det.data?.let { GpsRemoteSync.fromDetalle(it) }
                if (summary != null) {
                    repo.save(summary)
                    merged.add(summary)
                }
            }
        }
        return merged.sortedByDescending { it.startedAtMs }
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

    /** Importa GPX o TCX exportado desde reloj (Garmin, Polar, Suunto, Coros…). */
    fun importActividad(uri: Uri) {
        viewModelScope.launch {
            _importMessage.value = null
            try {
                val ctx = getApplication<Application>()
                val name = runCatching {
                    ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
                        val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
                    }
                }.getOrNull().orEmpty().lowercase()
                val summary = ctx.contentResolver.openInputStream(uri)?.use { stream ->
                    when {
                        name.endsWith(".tcx") -> TcxImport.parse(stream).getOrThrow()
                        else -> {
                            val bytes = stream.readBytes()
                            val gpx = runCatching {
                                GpxImport.parse(bytes.inputStream()).getOrThrow()
                            }
                            gpx.getOrElse {
                                TcxImport.parse(bytes.inputStream()).getOrThrow()
                            }
                        }
                    }
                } ?: throw IllegalArgumentException("No se pudo leer el fichero")
                val existing = repo.get(summary.id)
                if (existing != null) {
                    _importMessage.value = "Esta actividad ya estaba importada"
                    return@launch
                }
                repo.save(summary)
                _lastSaved.value = summary
                loadHistory()
                syncToBackend(summary)
                val dist = com.opofit.miapp.gps.util.GpsMetrics.formatDistance(summary.distanceM)
                _importMessage.value = "Importado: ${summary.type.display} · $dist"
            } catch (e: Exception) {
                _importMessage.value = e.message ?: "No se pudo importar el fichero"
            }
        }
    }

    /** @deprecated Usa [importActividad] */
    fun importGpx(uri: Uri) = importActividad(uri)

    /** Sincroniza entrenos del reloj (Health Connect) y servicios conectados. */
    fun syncDesdeReloj() {
        viewModelScope.launch {
            _history.update { it.copy(loading = true) }
            _importMessage.value = null
            try {
                val token = tokenManager.getToken().first().orEmpty()
                val res = EntrenoSyncService.syncDesdeRelojYCloud(getApplication(), token)
                loadHistory()
                _importMessage.value = res.mensaje()
            } catch (e: Exception) {
                _importMessage.value = e.message ?: "Error al sincronizar"
            } finally {
                _history.update { it.copy(loading = false) }
            }
        }
    }

    fun consumeImportMessage() {
        _importMessage.value = null
    }
}
