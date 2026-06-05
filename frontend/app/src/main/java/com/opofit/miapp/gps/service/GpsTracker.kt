package com.opofit.miapp.gps.service

import android.location.Location
import com.opofit.miapp.gps.model.ActivitySummary
import com.opofit.miapp.gps.model.ActivityType
import com.opofit.miapp.gps.model.GpsPoint
import com.opofit.miapp.gps.model.GpsTrackingState
import com.opofit.miapp.gps.model.SplitKm
import com.opofit.miapp.gps.util.GpsMetrics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

/**
 * Singleton in-memory state for the current GPS tracking session.
 * The Foreground Service feeds raw [Location] updates here, and any UI/ViewModel
 * subscribes to [state]. This keeps tracking alive across screen rotations
 * and navigation while the service is the lifecycle owner.
 */
object GpsTracker {
    private const val MIN_ACCURACY_M = 35f
    private const val MIN_SEG_M = 1.5
    private const val CADENCE_WINDOW_MS = 10_000L

    private val _state = MutableStateFlow(GpsTrackingState())
    val state: StateFlow<GpsTrackingState> = _state.asStateFlow()

    private var sessionId: String = ""
    private var lastLocation: Location? = null
    private var lastMovingTickMs: Long = 0L
    private var nextSplitKm: Int = 1
    private var stepEvents: ArrayDeque<Long> = ArrayDeque()
    private var totalSteps: Long = 0L
    private var hrSum: Long = 0L
    private var hrCount: Long = 0L
    private var userWeightKg: Double? = null

    fun begin(type: ActivityType, weightKg: Double? = null) {
        sessionId = UUID.randomUUID().toString()
        lastLocation = null
        lastMovingTickMs = 0L
        nextSplitKm = 1
        stepEvents = ArrayDeque()
        totalSteps = 0L
        hrSum = 0L
        hrCount = 0L
        userWeightKg = weightKg
        _state.value = GpsTrackingState(
            active = true,
            paused = false,
            type = type,
            startedAtMs = System.currentTimeMillis()
        )
    }

    fun pause() {
        _state.update { if (it.active) it.copy(paused = true) else it }
    }

    fun resume() {
        _state.update { if (it.active) it.copy(paused = false) else it }
    }

    fun setError(msg: String?) {
        _state.update { it.copy(lastErrorMsg = msg) }
    }

    fun isActive(): Boolean = _state.value.active

    fun reset() {
        sessionId = ""
        lastLocation = null
        nextSplitKm = 1
        stepEvents.clear()
        totalSteps = 0L
        hrSum = 0L
        hrCount = 0L
        userWeightKg = null
        _state.value = GpsTrackingState()
    }

    /** Called every second by the service timer to advance duration. */
    fun tickSecond() {
        val current = _state.value
        if (!current.active || current.paused) return
        val now = System.currentTimeMillis()
        val duration = ((now - current.startedAtMs) / 1000).toInt()
        val moving = current.movingSec + if (now - lastMovingTickMs in 1..3000) 1 else 0
        lastMovingTickMs = now
        val avgSpeed = if (moving > 0) current.distanceM / moving else 0.0
        val avgPace = GpsMetrics.paceSecPerKm(current.distanceM, moving) ?: 0.0
        val kcal = GpsMetrics.estimateKcal(current.type, moving, current.distanceM, userWeightKg)

        while (stepEvents.isNotEmpty() && now - stepEvents.first() > CADENCE_WINDOW_MS) {
            stepEvents.removeFirst()
        }
        val currentCadence = if (stepEvents.size >= 4) {
            (stepEvents.size * 60_000.0 / CADENCE_WINDOW_MS).toInt()
        } else null
        val avgCadence = if (moving > 0 && totalSteps > 0) {
            (totalSteps * 60.0 / moving).toInt()
        } else null
        val currentStride = GpsMetrics.estimateStrideM(
            current.currentSpeedMps.toDouble(),
            currentCadence
        )
        val avgStride = GpsMetrics.avgStrideFromPoints(current.points)
        val currentIncline = GpsMetrics.avgInclinePct(
            current.points.takeLast(8).takeIf { it.size >= 2 } ?: emptyList()
        )
        val avgIncline = GpsMetrics.avgInclinePct(current.points)

        _state.update {
            it.copy(
                durationSec = duration,
                movingSec = moving,
                avgSpeedMps = avgSpeed,
                avgPaceSecPerKm = avgPace,
                kcal = kcal,
                currentCadenceSpm = currentCadence,
                avgCadenceSpm = avgCadence,
                currentStrideM = currentStride,
                avgStrideM = avgStride,
                currentInclinePct = currentIncline,
                avgInclinePct = avgIncline
            )
        }
    }

    fun onLocation(loc: Location) {
        val current = _state.value
        if (!current.active || current.paused) return
        val acc = loc.accuracy
        if (acc.isFinite() && acc > MIN_ACCURACY_M) return

        val point = GpsPoint(
            lat = loc.latitude,
            lng = loc.longitude,
            altitude = if (loc.hasAltitude()) loc.altitude else null,
            speedMps = if (loc.hasSpeed()) loc.speed else null,
            accuracyM = acc,
            cadenceSpm = current.currentCadenceSpm,
            hrBpm = current.currentHrBpm,
            timestampMs = if (loc.time > 0) loc.time else System.currentTimeMillis()
        )

        var distance = current.distanceM
        val newPoints = current.points + point
        val last = lastLocation
        var segDist = 0.0
        if (last != null) {
            val seg = floatArrayOf(0f)
            Location.distanceBetween(
                last.latitude, last.longitude,
                loc.latitude, loc.longitude,
                seg
            )
            val d = seg[0].toDouble()
            if (d in MIN_SEG_M..200.0) {
                segDist = d
                distance += d
            }
        }
        lastLocation = loc

        val maxSpeed = maxOf(current.maxSpeedMps, point.speedMps ?: 0f)
        val newMinAlt = listOfNotNull(current.elevationMinM, point.altitude).minOrNull()
        val newMaxAlt = listOfNotNull(current.elevationMaxM, point.altitude).maxOrNull()
        val prevAlt = current.points.lastOrNull()?.altitude
        var elevationGain = current.elevationGainM
        var elevationLoss = current.elevationLossM
        if (prevAlt != null && point.altitude != null) {
            val diff = point.altitude - prevAlt
            if (diff > 0.5) elevationGain += diff
            if (diff < -0.5) elevationLoss += -diff
        }
        val instantPace = if (point.speedMps != null && point.speedMps > 0.1f) {
            1000.0 / point.speedMps
        } else current.avgPaceSecPerKm

        val splits = current.splits.toMutableList()
        while (distance >= nextSplitKm * 1000.0 && newPoints.size > 1) {
            val durSec = ((point.timestampMs - current.startedAtMs) / 1000).toInt() -
                splits.sumOf { it.durationSec }
            splits += SplitKm(
                km = nextSplitKm,
                durationSec = durSec.coerceAtLeast(1),
                paceSecPerKm = GpsMetrics.paceSecPerKm(1000.0, durSec.coerceAtLeast(1)) ?: 0.0
            )
            nextSplitKm += 1
        }

        @Suppress("UNUSED_VARIABLE") val s = segDist
        _state.update {
            it.copy(
                distanceM = distance,
                currentSpeedMps = point.speedMps ?: 0f,
                maxSpeedMps = maxSpeed,
                currentAltitudeM = point.altitude,
                elevationMinM = newMinAlt,
                elevationMaxM = newMaxAlt,
                elevationGainM = elevationGain,
                elevationLossM = elevationLoss,
                instantPaceSecPerKm = instantPace,
                points = newPoints,
                splits = splits
            )
        }
    }

    fun onStepDetected() {
        val now = System.currentTimeMillis()
        val current = _state.value
        if (!current.active || current.paused) return
        stepEvents.addLast(now)
        totalSteps += 1
    }

    fun onHrSample(bpm: Int) {
        val current = _state.value
        if (!current.active || current.paused) return
        hrSum += bpm
        hrCount += 1
        val avg = (hrSum / hrCount).toInt()
        val max = maxOf(current.maxHrBpm ?: 0, bpm)
        _state.update {
            it.copy(
                currentHrBpm = bpm,
                avgHrBpm = avg,
                maxHrBpm = max
            )
        }
    }

    fun onHrDeviceChanged(name: String?, connected: Boolean) {
        _state.update { it.copy(hrDeviceName = name, hrDeviceConnected = connected) }
    }

    /** Finishes the current session and returns its summary (or null if it never started). */
    fun finish(): ActivitySummary? {
        val s = _state.value
        if (!s.active || s.startedAtMs == 0L) {
            reset()
            return null
        }
        val endedAt = System.currentTimeMillis()
        val duration = ((endedAt - s.startedAtMs) / 1000).toInt()
        val moving = s.movingSec.coerceAtLeast(1)
        val avgSpeed = if (moving > 0) s.distanceM / moving else 0.0
        val avgPace = GpsMetrics.paceSecPerKm(s.distanceM, moving) ?: 0.0
        val paces = s.splits.map { it.paceSecPerKm }.filter { it > 0 }
        val maxPace = paces.maxOrNull() ?: avgPace
        val minPace = paces.minOrNull() ?: avgPace
        val (elevationGain, elevationLoss) = GpsMetrics.elevationStats(s.points)
        val kcal = GpsMetrics.estimateKcal(s.type, moving, s.distanceM, userWeightKg)
        val splitsKm = s.splits.ifEmpty { GpsMetrics.computeSplits(s.points) }
        val splitsMile = GpsMetrics.computeSplitsMile(s.points)
        val splitsTime = GpsMetrics.computeSplitsTime(s.points)
        val bestSegments = GpsMetrics.computeBestSegments(s.points, s.distanceM)
        val cadenceSamples = s.points.mapNotNull { it.cadenceSpm }
        val avgCadenceDouble = if (cadenceSamples.isNotEmpty()) cadenceSamples.average() else null
        val maxCadence = cadenceSamples.maxOrNull()
        val hrSamples = s.points.mapNotNull { it.hrBpm }
        val avgHr = if (hrSamples.isNotEmpty()) hrSamples.average().toInt() else null
        val maxHr = hrSamples.maxOrNull()
        val minHr = hrSamples.minOrNull()
        val avgStride = GpsMetrics.avgStrideFromPoints(s.points)
        val avgIncline = GpsMetrics.avgInclinePct(s.points)
        val summary = ActivitySummary(
            id = sessionId.ifBlank { UUID.randomUUID().toString() },
            type = s.type,
            startedAtMs = s.startedAtMs,
            endedAtMs = endedAt,
            durationSec = duration,
            movingSec = moving,
            distanceM = s.distanceM,
            avgSpeedMps = avgSpeed,
            maxSpeedMps = s.maxSpeedMps.toDouble(),
            avgPaceSecPerKm = avgPace,
            maxPaceSecPerKm = maxPace,
            minPaceSecPerKm = minPace,
            elevationGainM = elevationGain,
            elevationLossM = elevationLoss,
            elevationMinM = s.elevationMinM,
            elevationMaxM = s.elevationMaxM,
            avgCadenceSpm = avgCadenceDouble,
            maxCadenceSpm = maxCadence,
            avgStrideM = avgStride,
            avgInclinePct = avgIncline,
            avgHrBpm = avgHr,
            maxHrBpm = maxHr,
            minHrBpm = minHr,
            kcal = kcal,
            splits = splitsKm,
            splitsMile = splitsMile,
            splitsTime = splitsTime,
            bestSegments = bestSegments,
            points = s.points
        )
        reset()
        return summary
    }
}
