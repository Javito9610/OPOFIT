package com.opofit.miapp.integraciones

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.opofit.miapp.gps.data.GpsRepository
import com.opofit.miapp.gps.model.ActivitySummary
import com.opofit.miapp.gps.model.ActivityType
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Adaptador de Health Connect: pide permisos, lee sesiones de ejercicio
 * con todas las métricas asociadas y las añade al historial GPS local
 * como [ActivitySummary] (origen Health Connect).
 */
class HealthConnectManager(private val context: Context) {

    enum class Availability { NOT_SUPPORTED, NOT_INSTALLED, AVAILABLE }

    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(SpeedRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ElevationGainedRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class)
    )

    fun availability(): Availability {
        return when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_AVAILABLE -> Availability.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> Availability.NOT_INSTALLED
            else -> Availability.NOT_SUPPORTED
        }
    }

    private fun clientOrNull(): HealthConnectClient? {
        return if (availability() == Availability.AVAILABLE) {
            HealthConnectClient.getOrCreate(context)
        } else null
    }

    suspend fun hasAllPermissions(): Boolean {
        val client = clientOrNull() ?: return false
        val granted = client.permissionController.getGrantedPermissions()
        return granted.containsAll(permissions)
    }

    /**
     * Sincroniza las sesiones de ejercicio recientes y las añade como
     * [ActivitySummary] en el repositorio. Devuelve cuántas se importaron.
     */
    suspend fun syncLastDays(days: Long = 30): SyncResult {
        val client = clientOrNull() ?: return SyncResult(0, 0, "Health Connect no disponible")
        if (!hasAllPermissions()) return SyncResult(0, 0, "Faltan permisos de Health Connect")

        val repo = GpsRepository.get(context)
        val end = Instant.now()
        val start = end.minus(days, ChronoUnit.DAYS)
        val range = TimeRangeFilter.between(start, end)

        val sessions = client.readRecords(
            ReadRecordsRequest(ExerciseSessionRecord::class, range)
        ).records

        val existentes = repo.listAll().mapNotNull { it.id.takeIf { id -> id.startsWith("hc_") } }.toSet()

        var importadas = 0
        var saltadas = 0
        for (s in sessions) {
            val hcId = "hc_${s.startTime.toEpochMilli()}_${s.exerciseType}"
            if (hcId in existentes) { saltadas += 1; continue }

            val sessionRange = TimeRangeFilter.between(s.startTime, s.endTime)
            val hr = runCatching {
                client.readRecords(ReadRecordsRequest(HeartRateRecord::class, sessionRange)).records
            }.getOrNull().orEmpty()
            val dist = runCatching {
                client.readRecords(ReadRecordsRequest(DistanceRecord::class, sessionRange)).records
            }.getOrNull().orEmpty()
            val speed = runCatching {
                client.readRecords(ReadRecordsRequest(SpeedRecord::class, sessionRange)).records
            }.getOrNull().orEmpty()
            val kcal = runCatching {
                client.readRecords(ReadRecordsRequest(ActiveCaloriesBurnedRecord::class, sessionRange)).records
            }.getOrNull().orEmpty()
            val elev = runCatching {
                client.readRecords(ReadRecordsRequest(ElevationGainedRecord::class, sessionRange)).records
            }.getOrNull().orEmpty()

            val totalDistance = dist.sumOf { it.distance.inMeters }
            val durSec = ChronoUnit.SECONDS.between(s.startTime, s.endTime).toInt().coerceAtLeast(1)
            val totalKcal = kcal.sumOf { it.energy.inKilocalories }.toInt()
            val totalElev = elev.sumOf { it.elevation.inMeters }
            val hrSamples = hr.flatMap { it.samples }.map { it.beatsPerMinute }
            val speedSamples = speed.flatMap { it.samples }.map { it.speed.inMetersPerSecond }
            val avgSpeedMps = if (totalDistance > 0) totalDistance / durSec else speedSamples.average().takeIf { it.isFinite() } ?: 0.0
            val maxSpeed = speedSamples.maxOrNull() ?: avgSpeedMps
            val avgPace = if (totalDistance >= 10.0) durSec / (totalDistance / 1000.0) else 0.0
            val tipo = mapExerciseType(s.exerciseType)

            val summary = ActivitySummary(
                id = hcId,
                type = tipo,
                startedAtMs = s.startTime.toEpochMilli(),
                endedAtMs = s.endTime.toEpochMilli(),
                durationSec = durSec,
                movingSec = durSec,
                distanceM = totalDistance,
                avgSpeedMps = avgSpeedMps,
                maxSpeedMps = maxSpeed,
                avgPaceSecPerKm = avgPace,
                maxPaceSecPerKm = avgPace,
                minPaceSecPerKm = avgPace,
                elevationGainM = totalElev,
                elevationMinM = null,
                elevationMaxM = null,
                avgHrBpm = if (hrSamples.isNotEmpty()) hrSamples.average().toInt() else null,
                maxHrBpm = hrSamples.maxOrNull()?.toInt(),
                minHrBpm = hrSamples.minOrNull()?.toInt(),
                kcal = totalKcal.takeIf { it > 0 }
            )
            repo.save(summary)
            importadas += 1
        }
        return SyncResult(importadas, saltadas, null)
    }

    private fun mapExerciseType(code: Int): ActivityType {
        return when (code) {
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL -> ActivityType.RUN
            ExerciseSessionRecord.EXERCISE_TYPE_WALKING,
            ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> ActivityType.WALK
            ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
            ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY -> ActivityType.BIKE
            else -> ActivityType.RUN
        }
    }

    data class SyncResult(val importadas: Int, val saltadas: Int, val error: String?)

    companion object {
        @Volatile
        private var INSTANCE: HealthConnectManager? = null
        fun get(context: Context): HealthConnectManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: HealthConnectManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
