package com.opofit.miapp.gps.model

import java.util.UUID

enum class ActivityType(val display: String, val emoji: String) {
    RUN("Carrera", "🏃"),
    WALK("Caminar", "🚶"),
    BIKE("Bicicleta", "🚴");

    companion object {
        fun fromName(name: String?): ActivityType =
            entries.firstOrNull { it.name == name } ?: RUN
    }
}

data class GpsPoint(
    val lat: Double,
    val lng: Double,
    val altitude: Double? = null,
    val speedMps: Float? = null,
    val accuracyM: Float? = null,
    val timestampMs: Long
)

data class SplitKm(
    val km: Int,
    val durationSec: Int,
    val paceSecPerKm: Double,
    val elevationGainM: Double = 0.0
)

data class ActivitySummary(
    val id: String = UUID.randomUUID().toString(),
    val type: ActivityType,
    val startedAtMs: Long,
    val endedAtMs: Long,
    val durationSec: Int,
    val movingSec: Int,
    val distanceM: Double,
    val avgSpeedMps: Double,
    val maxSpeedMps: Double,
    val avgPaceSecPerKm: Double,
    val maxPaceSecPerKm: Double,
    val minPaceSecPerKm: Double,
    val elevationGainM: Double,
    val elevationMinM: Double?,
    val elevationMaxM: Double?,
    val avgCadenceSpm: Double? = null,
    val splits: List<SplitKm> = emptyList(),
    val points: List<GpsPoint> = emptyList(),
    val syncedRemoteId: Int? = null
)

data class GpsTrackingState(
    val active: Boolean = false,
    val paused: Boolean = false,
    val type: ActivityType = ActivityType.RUN,
    val startedAtMs: Long = 0L,
    val durationSec: Int = 0,
    val movingSec: Int = 0,
    val distanceM: Double = 0.0,
    val currentSpeedMps: Float = 0f,
    val maxSpeedMps: Float = 0f,
    val avgSpeedMps: Double = 0.0,
    val avgPaceSecPerKm: Double = 0.0,
    val currentAltitudeM: Double? = null,
    val elevationMinM: Double? = null,
    val elevationMaxM: Double? = null,
    val elevationGainM: Double = 0.0,
    val splits: List<SplitKm> = emptyList(),
    val points: List<GpsPoint> = emptyList(),
    val lastErrorMsg: String? = null
)
