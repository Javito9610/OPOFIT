package com.opofit.miapp.data.api

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

data class GpsSyncResponse(
    val ok: Boolean,
    val data: GpsSyncData? = null,
    val msg: String? = null
)

data class GpsSyncData(
    val idActividad: Int? = null,
    val uuid: String? = null
)

data class GpsActivityPayload(
    val id: String,
    val type: String,
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
    val elevationLossM: Double,
    val elevationMinM: Double?,
    val elevationMaxM: Double?,
    val avgCadenceSpm: Double?,
    val maxCadenceSpm: Int?,
    val avgHrBpm: Int?,
    val maxHrBpm: Int?,
    val minHrBpm: Int?,
    val kcal: Int?,
    val points: List<Map<String, Any?>>,
    val splits: List<Map<String, Any?>>,
    val splitsMile: List<Map<String, Any?>>,
    val splitsTime: List<Map<String, Any?>>,
    val bestSegments: List<Map<String, Any?>>
)

interface GpsApi {
    @POST("/api/gps/actividades")
    suspend fun guardar(
        @Header("Authorization") token: String,
        @Body body: GpsActivityPayload
    ): GpsSyncResponse

    @DELETE("/api/gps/actividades/{uuid}")
    suspend fun borrar(
        @Header("Authorization") token: String,
        @Path("uuid") uuid: String
    ): GpsSyncResponse

    @GET("/api/gps/actividades")
    suspend fun listar(
        @Header("Authorization") token: String
    ): GpsSyncResponse
}
