package com.opofit.miapp.integraciones

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessActivities
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.request.SessionReadRequest
import com.opofit.miapp.gps.data.GpsRepository
import com.opofit.miapp.gps.model.ActivitySummary
import com.opofit.miapp.gps.model.ActivityType
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/**
 * Adaptador de Google Fit para móviles sin Health Connect o como alternativa.
 */
class GoogleFitManager(private val context: Context) {

    val fitnessOptions: FitnessOptions = FitnessOptions.builder()
        .accessActivitySessions(FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_ACTIVITY_SEGMENT, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_SPEED, FitnessOptions.ACCESS_READ)
        .build()

    fun hasPermissions(): Boolean {
        val account = accountOrNull() ?: return false
        return GoogleSignIn.hasPermissions(account, fitnessOptions)
    }

    fun getSignInIntent(activity: Activity): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .addExtension(fitnessOptions)
            .build()
        return GoogleSignIn.getClient(activity, gso).signInIntent
    }

    suspend fun trySilentSignIn(): GoogleSignInAccount? {
        accountOrNull()?.let { return it }
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .addExtension(fitnessOptions)
            .build()
        val client = GoogleSignIn.getClient(context, gso)
        return try {
            client.silentSignIn().await()
        } catch (_: Exception) {
            null
        }
    }

    suspend fun syncLastDays(days: Long = 30): SyncResult {
        val account = accountOrNull()
            ?: return SyncResult(0, 0, "Inicia sesión con Google para usar Google Fit")
        if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
            return SyncResult(0, 0, "Faltan permisos de Google Fit")
        }

        val repo = GpsRepository.get(context)
        val end = System.currentTimeMillis()
        val start = end - TimeUnit.DAYS.toMillis(days)

        val request = SessionReadRequest.Builder()
            .readSessionsFromAllApps()
            .read(DataType.TYPE_ACTIVITY_SEGMENT)
            .read(DataType.TYPE_DISTANCE_DELTA)
            .read(DataType.TYPE_CALORIES_EXPENDED)
            .read(DataType.TYPE_HEART_RATE_BPM)
            .read(DataType.TYPE_SPEED)
            .setTimeInterval(start, end, TimeUnit.MILLISECONDS)
            .build()

        val response = try {
            Fitness.getSessionsClient(context, account).readSession(request).await()
        } catch (e: Exception) {
            return SyncResult(0, 0, e.message ?: "Error leyendo Google Fit")
        }

        val existentes = repo.listAll()
            .mapNotNull { it.id.takeIf { id -> id.startsWith("gf_") } }
            .toSet()

        var importadas = 0
        var saltadas = 0
        for (session in response.sessions) {
            val gfId = "gf_${session.getIdentifier()}"
            if (gfId in existentes) {
                saltadas += 1
                continue
            }

            val durSec = ((session.getEndTime(TimeUnit.MILLISECONDS) -
                session.getStartTime(TimeUnit.MILLISECONDS)) / 1000L)
                .toInt()
                .coerceAtLeast(1)

            var distanceM = 0.0
            var kcal = 0
            var avgHr: Int? = null
            var maxHr: Int? = null
            var maxSpeed = 0.0

            for (dataSet in response.getDataSet(session)) {
                when (dataSet.dataType) {
                    DataType.TYPE_DISTANCE_DELTA -> {
                        distanceM += dataSet.dataPoints.sumOf { dp ->
                            dp.getValue(dataSet.dataType.fields[0]).asFloat().toDouble()
                        }
                    }
                    DataType.TYPE_CALORIES_EXPENDED -> {
                        kcal += dataSet.dataPoints.sumOf { dp ->
                            dp.getValue(dataSet.dataType.fields[0]).asFloat().toInt()
                        }
                    }
                    DataType.TYPE_HEART_RATE_BPM -> {
                        val samples = dataSet.dataPoints.map {
                            it.getValue(dataSet.dataType.fields[0]).asFloat().toInt()
                        }
                        if (samples.isNotEmpty()) {
                            avgHr = samples.average().toInt()
                            maxHr = samples.maxOrNull()
                        }
                    }
                    DataType.TYPE_SPEED -> {
                        val speeds = dataSet.dataPoints.map {
                            it.getValue(dataSet.dataType.fields[0]).asFloat().toDouble()
                        }
                        maxSpeed = speeds.maxOrNull() ?: 0.0
                    }
                }
            }

            val avgSpeedMps = if (distanceM > 0) distanceM / durSec else 0.0
            val avgPace = if (distanceM >= 10.0) durSec / (distanceM / 1000.0) else 0.0

            val summary = ActivitySummary(
                id = gfId,
                type = mapActivityType(session.getActivity()),
                startedAtMs = session.getStartTime(TimeUnit.MILLISECONDS),
                endedAtMs = session.getEndTime(TimeUnit.MILLISECONDS),
                durationSec = durSec,
                movingSec = durSec,
                distanceM = distanceM,
                avgSpeedMps = avgSpeedMps,
                maxSpeedMps = maxSpeed.coerceAtLeast(avgSpeedMps),
                avgPaceSecPerKm = avgPace,
                maxPaceSecPerKm = avgPace,
                minPaceSecPerKm = avgPace,
                elevationGainM = 0.0,
                elevationMinM = null,
                elevationMaxM = null,
                avgHrBpm = avgHr,
                maxHrBpm = maxHr,
                minHrBpm = null,
                kcal = kcal.takeIf { it > 0 }
            )
            repo.save(summary)
            importadas += 1
        }
        return SyncResult(importadas, saltadas, null)
    }

    fun openGoogleFit(): Boolean {
        val intents = listOf(
            context.packageManager.getLaunchIntentForPackage(GOOGLE_FIT_PACKAGE),
            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$GOOGLE_FIT_PACKAGE"))
        )
        for (intent in intents) {
            if (intent == null) continue
            return try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } catch (_: Exception) {
                continue
            }
        }
        return false
    }

    private fun accountOrNull(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
            ?: runCatching {
                GoogleSignIn.getAccountForExtension(context, fitnessOptions)
            }.getOrNull()
    }

    private fun mapActivityType(activity: String): ActivityType {
        return when (activity) {
            FitnessActivities.RUNNING,
            FitnessActivities.RUNNING_JOGGING,
            FitnessActivities.RUNNING_SAND,
            FitnessActivities.RUNNING_TREADMILL -> ActivityType.RUN
            FitnessActivities.WALKING,
            FitnessActivities.WALKING_FITNESS,
            FitnessActivities.WALKING_NORDIC,
            FitnessActivities.WALKING_TREADMILL,
            FitnessActivities.HIKING -> ActivityType.WALK
            FitnessActivities.BIKING,
            FitnessActivities.BIKING_MOUNTAIN,
            FitnessActivities.BIKING_ROAD,
            FitnessActivities.BIKING_SPINNING,
            FitnessActivities.BIKING_STATIONARY,
            FitnessActivities.BIKING_UTILITY -> ActivityType.BIKE
            else -> ActivityType.RUN
        }
    }

    data class SyncResult(val importadas: Int, val saltadas: Int, val error: String?)

    companion object {
        const val GOOGLE_FIT_PACKAGE = "com.google.android.apps.fitness"

        @Volatile
        private var INSTANCE: GoogleFitManager? = null

        fun get(context: Context): GoogleFitManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GoogleFitManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
