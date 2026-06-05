package com.opofit.miapp.gps.util

import com.opofit.miapp.gps.model.ActivityType
import com.opofit.miapp.gps.model.BestSegment
import com.opofit.miapp.gps.model.GpsPoint
import com.opofit.miapp.gps.model.SplitKm
import com.opofit.miapp.gps.model.SplitMile
import com.opofit.miapp.gps.model.SplitTime
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

object GpsMetrics {
    private const val EARTH_RADIUS_M = 6371000.0
    private const val MI_M = 1609.344
    private const val TIME_BUCKET_SEC = 300

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
        var hrSum = 0L
        var hrCount = 0
        var cadSum = 0L
        var cadCount = 0
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
            points[i].hrBpm?.let { hrSum += it; hrCount += 1 }
            points[i].cadenceSpm?.let { cadSum += it; cadCount += 1 }
            while (accDist >= nextKm * 1000.0) {
                val splitDur = ((points[i].timestampMs - points[splitStartIdx].timestampMs) / 1000).toInt()
                splits += SplitKm(
                    km = nextKm,
                    durationSec = splitDur.coerceAtLeast(1),
                    paceSecPerKm = paceSecPerKm(1000.0, splitDur.coerceAtLeast(1)) ?: 0.0,
                    elevationGainM = elevationGainSplit,
                    avgHrBpm = if (hrCount > 0) (hrSum / hrCount).toInt() else null,
                    avgCadenceSpm = if (cadCount > 0) (cadSum / cadCount).toInt() else null
                )
                splitStartIdx = i
                elevationGainSplit = 0.0
                hrSum = 0L; hrCount = 0
                cadSum = 0L; cadCount = 0
                nextKm += 1
            }
        }
        return splits
    }

    fun computeSplitsMile(points: List<GpsPoint>): List<SplitMile> {
        if (points.size < 2) return emptyList()
        val splits = mutableListOf<SplitMile>()
        var accDist = 0.0
        var splitStartIdx = 0
        var elevationGainSplit = 0.0
        var nextMile = 1
        for (i in 1 until points.size) {
            val seg = haversineMeters(points[i - 1], points[i])
            accDist += seg
            val prevAlt = points[i - 1].altitude
            val curAlt = points[i].altitude
            if (prevAlt != null && curAlt != null) {
                val diff = curAlt - prevAlt
                if (diff > 0) elevationGainSplit += diff
            }
            while (accDist >= nextMile * MI_M) {
                val splitDur = ((points[i].timestampMs - points[splitStartIdx].timestampMs) / 1000).toInt()
                splits += SplitMile(
                    mile = nextMile,
                    durationSec = splitDur.coerceAtLeast(1),
                    paceSecPerMi = splitDur.coerceAtLeast(1) / 1.0,
                    elevationGainM = elevationGainSplit
                )
                splitStartIdx = i
                elevationGainSplit = 0.0
                nextMile += 1
            }
        }
        return splits
    }

    /** Splits cada N segundos (por defecto 5 min). */
    fun computeSplitsTime(points: List<GpsPoint>, bucketSec: Int = TIME_BUCKET_SEC): List<SplitTime> {
        if (points.size < 2) return emptyList()
        val out = mutableListOf<SplitTime>()
        var bucketIdx = 1
        var startIdx = 0
        var bucketDist = 0.0
        val start = points.first().timestampMs
        for (i in 1 until points.size) {
            bucketDist += haversineMeters(points[i - 1], points[i])
            val elapsedSec = ((points[i].timestampMs - start) / 1000).toInt()
            if (elapsedSec >= bucketIdx * bucketSec) {
                val dur = ((points[i].timestampMs - points[startIdx].timestampMs) / 1000).toInt().coerceAtLeast(1)
                out += SplitTime(
                    index = bucketIdx,
                    durationSec = dur,
                    distanceM = bucketDist,
                    avgPaceSecPerKm = paceSecPerKm(bucketDist, dur) ?: 0.0
                )
                startIdx = i
                bucketDist = 0.0
                bucketIdx += 1
            }
        }
        return out
    }

    /** Mejor segmento de distancia fija (ventana deslizante en metros). */
    fun bestForDistance(points: List<GpsPoint>, targetM: Double, label: String): BestSegment? {
        if (points.size < 2) return null
        val cumDist = DoubleArray(points.size)
        for (i in 1 until points.size) {
            cumDist[i] = cumDist[i - 1] + haversineMeters(points[i - 1], points[i])
        }
        if (cumDist.last() < targetM) return null
        var bestDur = Int.MAX_VALUE
        var bestStart = 0
        var bestEnd = 0
        var i = 0
        var j = 0
        while (j < points.size) {
            while (i < j && cumDist[j] - cumDist[i] >= targetM) i += 1
            if (i > 0 && cumDist[j] - cumDist[i - 1] >= targetM) {
                val dur = ((points[j].timestampMs - points[i - 1].timestampMs) / 1000).toInt()
                if (dur in 1 until bestDur) {
                    bestDur = dur
                    bestStart = i - 1
                    bestEnd = j
                }
            }
            j += 1
        }
        if (bestDur == Int.MAX_VALUE) return null
        @Suppress("UNUSED_VARIABLE") val s = bestStart; @Suppress("UNUSED_VARIABLE") val e = bestEnd
        val pace = paceSecPerKm(targetM, bestDur) ?: return null
        return BestSegment(label = label, distanceM = targetM, durationSec = bestDur, paceSecPerKm = pace)
    }

    fun computeBestSegments(points: List<GpsPoint>, totalDistanceM: Double): List<BestSegment> {
        val out = mutableListOf<BestSegment>()
        if (totalDistanceM >= 50) bestForDistance(points, 50.0, "Mejor 50 m")?.let { out += it }
        if (totalDistanceM >= 100) bestForDistance(points, 100.0, "Mejor 100 m")?.let { out += it }
        if (totalDistanceM >= 1000) bestForDistance(points, 1000.0, "Mejor 1 km")?.let { out += it }
        if (totalDistanceM >= MI_M) bestForDistance(points, MI_M, "Mejor 1 milla")?.let { out += it }
        if (totalDistanceM >= 5000) bestForDistance(points, 5000.0, "Mejor 5K")?.let { out += it }
        if (totalDistanceM >= 10000) bestForDistance(points, 10000.0, "Mejor 10K")?.let { out += it }
        if (totalDistanceM >= 21097.5) bestForDistance(points, 21097.5, "Mejor media")?.let { out += it }
        return out
    }

    /** Sum of positive altitude deltas after a 3-point smoothing window. */
    fun elevationGain(points: List<GpsPoint>): Double {
        return elevationStats(points).first
    }

    fun elevationLoss(points: List<GpsPoint>): Double {
        return elevationStats(points).second
    }

    fun elevationStats(points: List<GpsPoint>): Pair<Double, Double> {
        if (points.size < 3) return 0.0 to 0.0
        val smoothed = DoubleArray(points.size) {
            val alts = listOfNotNull(
                points.getOrNull(it - 1)?.altitude,
                points[it].altitude,
                points.getOrNull(it + 1)?.altitude
            )
            if (alts.isEmpty()) 0.0 else alts.average()
        }
        var gain = 0.0
        var loss = 0.0
        for (i in 1 until smoothed.size) {
            val d = smoothed[i] - smoothed[i - 1]
            if (d > 0) gain += d else loss += -d
        }
        return gain to loss
    }

    /** Zancada estimada (m) a partir de velocidad y cadencia. */
    fun estimateStrideM(speedMps: Double, cadenceSpm: Int?): Double? {
        if (cadenceSpm == null || cadenceSpm < 30 || speedMps < 0.3) return null
        return speedMps * 60.0 / cadenceSpm
    }

    fun avgStrideFromPoints(points: List<GpsPoint>): Double? {
        val strides = points.mapNotNull { p ->
            val c = p.cadenceSpm ?: return@mapNotNull null
            val s = p.speedMps?.toDouble() ?: return@mapNotNull null
            estimateStrideM(s, c)
        }
        return strides.takeIf { it.isNotEmpty() }?.average()
    }

    /** Pendiente media (%) entre puntos consecutivos. */
    fun avgInclinePct(points: List<GpsPoint>): Double? {
        if (points.size < 2) return null
        var sum = 0.0
        var n = 0
        for (i in 1 until points.size) {
            val d = haversineMeters(points[i - 1], points[i])
            if (d < 1.0) continue
            val a0 = points[i - 1].altitude
            val a1 = points[i].altitude
            if (a0 != null && a1 != null) {
                sum += ((a1 - a0) / d) * 100.0
                n++
            }
        }
        return if (n > 0) sum / n else null
    }

    fun formatStride(m: Double?): String {
        if (m == null || !m.isFinite() || m <= 0) return "—"
        return "%.2f m".format(m)
    }

    fun formatIncline(pct: Double?): String {
        if (pct == null || !pct.isFinite()) return "—"
        return "%+.1f %%".format(pct)
    }

    /**
     * Estimated calories: MET * weight * hours. MET ajustado por velocidad para correr/bici.
     * weightKg puede ser null; en ese caso se asume 70 kg.
     */
    fun estimateKcal(type: ActivityType, movingSec: Int, distanceM: Double, weightKg: Double?): Int {
        if (movingSec <= 0) return 0
        val hours = movingSec / 3600.0
        val weight = (weightKg ?: 70.0).coerceIn(35.0, 200.0)
        val mps = if (movingSec > 0) distanceM / movingSec else 0.0
        val met = when (type) {
            ActivityType.RUN -> {
                // tabla aproximada: 4 min/km≈14 MET, 5≈11.5, 6≈9.8, 7≈8.3, 8≈7
                val kmh = mps * 3.6
                when {
                    kmh >= 16 -> 14.5
                    kmh >= 14 -> 12.5
                    kmh >= 12 -> 11.0
                    kmh >= 10 -> 9.8
                    kmh >= 8 -> 8.3
                    kmh >= 6 -> 6.0
                    else -> 5.0
                }
            }
            ActivityType.WALK -> {
                val kmh = mps * 3.6
                when {
                    kmh >= 7 -> 6.3
                    kmh >= 5.5 -> 4.3
                    kmh >= 4 -> 3.5
                    else -> 2.8
                }
            }
            ActivityType.BIKE -> {
                val kmh = mps * 3.6
                when {
                    kmh >= 32 -> 12.0
                    kmh >= 25 -> 9.5
                    kmh >= 19 -> 7.5
                    kmh >= 13 -> 5.5
                    else -> 4.0
                }
            }
        }
        return max(1, (met * weight * hours).toInt())
    }

    /** Bucketiza la polilínea por velocidad para colorear segmentos. */
    fun colorBucketsBySpeed(points: List<GpsPoint>): List<Int> {
        if (points.isEmpty()) return emptyList()
        val speeds = points.map { it.speedMps?.toDouble() ?: 0.0 }
        val nonZero = speeds.filter { it > 0.1 }
        if (nonZero.size < 2) return List(points.size) { 2 }
        val sorted = nonZero.sorted()
        val q = listOf(0.2, 0.4, 0.6, 0.8).map { sorted[((sorted.size - 1) * it).toInt()] }
        return speeds.map { v ->
            when {
                v <= q[0] -> 0
                v <= q[1] -> 1
                v <= q[2] -> 2
                v <= q[3] -> 3
                else -> 4
            }
        }
    }
}
