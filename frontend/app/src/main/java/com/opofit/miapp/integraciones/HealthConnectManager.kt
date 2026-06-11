package com.opofit.miapp.integraciones

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import com.opofit.miapp.gps.data.GpsRepository
import com.opofit.miapp.gps.model.ActivitySummary
import com.opofit.miapp.gps.model.ActivityType
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/**
 * Adaptador de Health Connect: pide permisos, lee sesiones de ejercicio
 * con todas las métricas asociadas y las añade al historial GPS local
 * como [ActivitySummary] (origen Health Connect).
 */
class HealthConnectManager(private val context: Context) {

    enum class Availability { NOT_SUPPORTED, NOT_INSTALLED, AVAILABLE }

    /** Lectura mínima para importar entrenos del reloj (sin esto no hay sync). */
    val readPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ElevationGainedRecord::class)
    )

    /** Opcionales: si faltan, el sync sigue (velocidad/pasos se calculan por distancia). */
    val optionalReadPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(SpeedRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class)
    )

    /** Escritura: solo para empujar actividades OpoFit al reloj. */
    val writePermissions: Set<String> = setOf(
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(DistanceRecord::class),
        HealthPermission.getWritePermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getWritePermission(ElevationGainedRecord::class)
    )

    /** Todo lo que pedimos al usuario en el diálogo de HC. */
    val permissions: Set<String> = readPermissions + optionalReadPermissions + writePermissions

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

    suspend fun hasReadPermissions(): Boolean {
        val client = clientOrNull() ?: return false
        val granted = client.permissionController.getGrantedPermissions()
        return granted.containsAll(readPermissions)
    }

    suspend fun hasWritePermissions(): Boolean {
        val client = clientOrNull() ?: return false
        val granted = client.permissionController.getGrantedPermissions()
        return granted.containsAll(writePermissions)
    }

    /** Lectura + escritura (empujar al reloj). */
    suspend fun hasAllPermissions(): Boolean {
        return hasReadPermissions() && hasWritePermissions()
    }

    /**
     * Sincroniza las sesiones de ejercicio recientes y las añade como
     * [ActivitySummary] en el repositorio. Devuelve cuántas se importaron.
     */
    suspend fun syncLastDays(days: Long = 30): SyncResult {
        val client = clientOrNull() ?: return SyncResult(0, 0, "Health Connect no disponible")
        if (!hasReadPermissions()) return SyncResult(0, 0, "Faltan permisos de lectura en Health Connect")

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

    data class PushResult(val enviadas: Int, val saltadas: Int, val error: String?)

    /**
     * Empuja una actividad GPS al reloj/wearable vía Health Connect.
     * El reloj la verá en Samsung Health / Mi Fitness / Zepp / Google Fit cuando
     * abra la app de salud correspondiente.
     */
    suspend fun pushActivity(summary: ActivitySummary): PushResult {
        val client = clientOrNull() ?: return PushResult(0, 0, "Health Connect no disponible")
        if (!hasWritePermissions()) return PushResult(0, 0, "Faltan permisos de escritura en Health Connect")
        return runCatching {
            val start = Instant.ofEpochMilli(summary.startedAtMs)
            val end = Instant.ofEpochMilli(summary.endedAtMs)
            val zone = ZoneId.systemDefault().rules.getOffset(start)
            val records = buildList<androidx.health.connect.client.records.Record> {
                add(
                    ExerciseSessionRecord(
                        startTime = start,
                        startZoneOffset = zone,
                        endTime = end,
                        endZoneOffset = zone,
                        exerciseType = mapActivityType(summary.type),
                        title = "OpoFit · ${summary.type.display}",
                        notes = null
                    )
                )
                if (summary.distanceM > 0) {
                    add(
                        DistanceRecord(
                            startTime = start,
                            startZoneOffset = zone,
                            endTime = end,
                            endZoneOffset = zone,
                            distance = Length.meters(summary.distanceM)
                        )
                    )
                }
                summary.kcal?.takeIf { it > 0 }?.let { kcal ->
                    add(
                        ActiveCaloriesBurnedRecord(
                            startTime = start,
                            startZoneOffset = zone,
                            endTime = end,
                            endZoneOffset = zone,
                            energy = Energy.kilocalories(kcal.toDouble())
                        )
                    )
                }
                if (summary.elevationGainM > 0) {
                    add(
                        ElevationGainedRecord(
                            startTime = start,
                            startZoneOffset = zone,
                            endTime = end,
                            endZoneOffset = zone,
                            elevation = Length.meters(summary.elevationGainM)
                        )
                    )
                }
            }
            client.insertRecords(records)
            PushResult(records.size, 0, null)
        }.getOrElse { e -> PushResult(0, 0, e.message ?: "Error al sincronizar al reloj") }
    }

    /** Sincroniza al reloj las últimas N actividades locales que no fueron previamente importadas desde HC. */
    suspend fun pushUltimasActividades(limite: Int = 10): PushResult {
        val client = clientOrNull() ?: return PushResult(0, 0, "Health Connect no disponible")
        if (!hasWritePermissions()) return PushResult(0, 0, "Faltan permisos de escritura en Health Connect")
        val repo = GpsRepository.get(context)
        val locales = repo.listAll()
            .filter { !it.id.startsWith("hc_") } // no re-empujar las que vinieron del reloj
            .sortedByDescending { it.startedAtMs }
            .take(limite)
        var enviadas = 0
        var fallos = 0
        for (a in locales) {
            val r = pushActivity(a)
            if (r.error == null) enviadas += r.enviadas else fallos += 1
        }
        return PushResult(enviadas, fallos, if (fallos > 0) "$fallos fallos al empujar" else null)
    }

    private fun mapActivityType(type: ActivityType): Int = when (type) {
        ActivityType.RUN -> ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
        ActivityType.WALK -> ExerciseSessionRecord.EXERCISE_TYPE_WALKING
        ActivityType.BIKE -> ExerciseSessionRecord.EXERCISE_TYPE_BIKING
    }

    /** Abre la pantalla de permisos de OpoFit dentro de Health Connect. */
    fun openManagePermissions(context: Context): Boolean {
        val pkg = context.packageName
        val candidates = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                add(
                    Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS).apply {
                        putExtra(Intent.EXTRA_PACKAGE_NAME, pkg)
                    }
                )
            }
            add(
                Intent("android.intent.action.VIEW_PERMISSION_USAGE").apply {
                    putExtra(Intent.EXTRA_PACKAGE_NAME, pkg)
                    addCategory("android.intent.category.HEALTH_PERMISSIONS")
                }
            )
            add(Intent("androidx.health.ACTION_HEALTH_CONNECT_SETTINGS"))
            context.packageManager.getLaunchIntentForPackage(HEALTH_CONNECT_PACKAGE)?.let { add(it) }
        }
        for (intent in candidates) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return true
            } catch (_: Exception) {
                /* siguiente intent */
            }
        }
        return false
    }

    fun openHealthConnect(context: Context): Boolean = openManagePermissions(context)

    companion object {
        const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"
        @Volatile
        private var INSTANCE: HealthConnectManager? = null
        fun get(context: Context): HealthConnectManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: HealthConnectManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
