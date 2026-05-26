package com.opofit.miapp.data.api

import com.opofit.miapp.BuildConfig
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

data class ProveedorEstado(
    val provider: String,
    val externalUserId: String? = null,
    val lastSyncAt: String? = null,
    val expiresAt: Long? = null,
    val conectado: Boolean = true
)

data class EstadoIntegraciones(
    val proveedores: List<ProveedorEstado> = emptyList(),
    val stravaConfigured: Boolean = false,
    val polarConfigured: Boolean = false
)

data class EstadoIntegracionesResponse(
    val ok: Boolean,
    val data: EstadoIntegraciones? = null,
    val msg: String? = null
)

data class SyncIntegracionData(
    val importadas: Int = 0,
    val saltadas: Int = 0
)

data class SyncIntegracionResponse(
    val ok: Boolean,
    val data: SyncIntegracionData? = null,
    val msg: String? = null
)

data class HcActivityPayload(
    val externalId: String,
    val tipo: String,
    val startedAtMs: Long,
    val endedAtMs: Long,
    val durationSec: Int,
    val movingSec: Int,
    val distanceM: Double,
    val avgSpeedMps: Double,
    val maxSpeedMps: Double,
    val avgPaceSecPerKm: Double,
    val minPaceSecPerKm: Double,
    val maxPaceSecPerKm: Double,
    val elevationGainM: Double,
    val elevationMinM: Double?,
    val elevationMaxM: Double?,
    val avgCadenceSpm: Double?
)

data class HcImportarRequest(
    val actividades: List<HcActivityPayload>
)

interface IntegracionesApi {
    @GET("/api/integraciones/estado")
    suspend fun estado(@Header("Authorization") token: String): EstadoIntegracionesResponse

    @POST("/api/integraciones/strava/sync")
    suspend fun syncStrava(@Header("Authorization") token: String): SyncIntegracionResponse

    @DELETE("/api/integraciones/strava")
    suspend fun disconnectStrava(@Header("Authorization") token: String): SyncIntegracionResponse

    @POST("/api/integraciones/polar/sync")
    suspend fun syncPolar(@Header("Authorization") token: String): SyncIntegracionResponse

    @DELETE("/api/integraciones/polar")
    suspend fun disconnectPolar(@Header("Authorization") token: String): SyncIntegracionResponse

    @POST("/api/integraciones/health-connect/importar")
    suspend fun importarHealthConnect(
        @Header("Authorization") token: String,
        @Body body: HcImportarRequest
    ): SyncIntegracionResponse

    companion object {
        fun stravaStartUrl(): String =
            "${BuildConfig.BASE_URL.trimEnd('/')}/api/integraciones/strava/start"
        fun polarStartUrl(): String =
            "${BuildConfig.BASE_URL.trimEnd('/')}/api/integraciones/polar/start"
    }
}
