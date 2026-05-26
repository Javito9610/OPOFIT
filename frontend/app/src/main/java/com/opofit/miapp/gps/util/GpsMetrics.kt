package com.opofit.miapp.gps.util

import com.opofit.miapp.gps.model.GpsPoint
import com.opofit.miapp.gps.model.SplitKm
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object GpsMetrics {
    private const val EARTH_RADIUS_M = 6371000.0

    fun haversineMeters(a: GpsPoint, b: GpsPoint): Double {
        val lat1 = Math.toRadians(a.lat)
        val lat2 = Math.toRadians(b.lat)
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLng = Math.toRadians(b.lng - a.lng)
        val h = sin(dLat / 2).let { it * it } +
            cos(lat1) * cos(lat2) * sin(dLng / 2).let { it * it }
        val c = 2 * atan2(sqrt(h), sqrt(1 - h))
        return EARTH_RADIUS_M * c
    }

    /** Pace in seconds per kilometer; null when distance is too small. */
    fun paceSecPerKm(distanceM: Double, durationSec: Int): Double? {
        if (distanceM < 10.0 || durationSec <= 0) return null
        return durationSec / (distanceM / 1000.0)
    }

    fun formatPace(secPerKm: Double?): String {
        if (secPerKm == null || !secPerKm.isFinite() || secPerKm <= 0) return "-"
        val mins = (secPerKm / 60).toInt()
        val secs = (secPerKm % 60).toInt()
        return "%d:%02d".format(mins, secs)
    }

    fun formatDuration(totalSec: Int): String {
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }

    fun formatDistance(meters: Double): String {
        return if (meters < 1000) "%.0f m".format(meters)
        else "%.2f km".format(meters / 1000.0)
    }

    fun formatSpeedKmh(mps: Double): String {
        return "%.1f km/h".format(mps * 3.6)
    }

    /** Computes splits per kilometre from raw points. */
    fun computeSplits(points: List<GpsPoint>): List<SplitKm> {
        if (points.size < 2) return emptyList()
        val splits = mutableListOf<SplitKm>()
        var accDist = 0.0
        var splitStartIdx = 0
        var elevationGainSplit = 0.0
        var nextKm = 1
        for (i in 1 until points.size) {
            val seg = haversineMeters(points[i - 1], points[i])
            accDist += seg
            val prevAlt = points[i - 1].altitude
            val curAlt = points[i].altitude
            if (prevAlt != null && curAlt != null) {
                val diff = curAlt - prevAlt
                if (diff > 0) elevationGainSplit += diff
            }
            while (accDist >= nextKm * 1000.0) {
                val splitDur = ((points[i].timestampMs - points[splitStartIdx].timestampMs) / 1000).toInt()
                splits += SplitKm(
                    km = nextKm,
                    durationSec = splitDur,
                    paceSecPerKm = paceSecPerKm(1000.0, splitDur) ?: 0.0,
                    elevationGainM = elevationGainSplit
                )
                splitStartIdx = i
                elevationGainSplit = 0.0
                nextKm += 1
            }
        }
        return splits
    }

    /**
     * Returns the smoothed cumulative elevation gain (sum of positive altitude deltas)
     * after applying a 3-point smoothing window.
     */
    fun elevationGain(points: List<GpsPoint>): Double {
        if (points.size < 3) return 0.0
        val smoothed = DoubleArray(points.size) {
            val alts = listOfNotNull(
                points.getOrNull(it - 1)?.altitude,
                points[it].altitude,
                points.getOrNull(it + 1)?.altitude
            )
            if (alts.isEmpty()) 0.0 else alts.average()
        }
        var gain = 0.0
        for (i in 1 until smoothed.size) {
            val d = smoothed[i] - smoothed[i - 1]
            if (d > 0) gain += d
        }
        return gain
    }
}
