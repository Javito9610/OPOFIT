package com.opofit.miapp.gps.util

import android.util.Xml
import com.opofit.miapp.gps.model.ActivityLap
import com.opofit.miapp.gps.model.ActivitySummary
import com.opofit.miapp.gps.model.ActivityType
import com.opofit.miapp.gps.model.GpsPoint
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max

/**
 * Importa actividades completadas desde TCX (Garmin Connect, Polar Flow, Suunto, Coros…).
 * Soporta el esquema TrainingCenterDatabase con Track/Trackpoint.
 */
object TcxImport {

    private val ISO_Z = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val ISO_MS = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val ISO_OFFSET = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)

    fun parse(stream: InputStream): Result<ActivitySummary> {
        return runCatching {
            val parser = Xml.newPullParser()
            parser.setInput(stream, "UTF-8")
            var event = parser.eventType
            var sport: String? = null
            var lapDistanceM: Double? = null
            var lapTimeSec: Int? = null
            val laps = mutableListOf<ActivityLap>()
            var inLap = false
            var currentLapTime: Int? = null
            var currentLapDist: Double? = null
            var currentLapNotes: String? = null
            val points = mutableListOf<GpsPoint>()
            var inTrackpoint = false
            var lat = 0.0
            var lon = 0.0
            var ele: Double? = null
            var timeMs: Long? = null
            var hr: Int? = null
            var cad: Int? = null
            var inHr = false

            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> when (parser.name) {
                        "Activity" -> sport = parser.getAttributeValue(null, "Sport")
                        "Lap" -> {
                            inLap = true
                            currentLapTime = null
                            currentLapDist = null
                            currentLapNotes = null
                        }
                        "TotalTimeSeconds" -> {
                            val v = parser.nextText().toDoubleOrNull()?.toInt()
                            if (inLap) currentLapTime = v else lapTimeSec = v
                        }
                        "DistanceMeters" -> {
                            val v = parser.nextText().toDoubleOrNull()
                            if (inLap) currentLapDist = v else lapDistanceM = v
                        }
                        // Algunos exportadores (Garmin Connect, Suunto) escriben
                        // el nombre del intervalo en Notes dentro del Lap.
                        // Lo guardamos como label para el matcher por nombre.
                        "Notes" -> if (inLap) currentLapNotes = parser.nextText()?.trim()
                        "Trackpoint" -> {
                            inTrackpoint = true
                            lat = 0.0; lon = 0.0; ele = null; timeMs = null; hr = null; cad = null
                        }
                        "Time" -> if (inTrackpoint) timeMs = parseTime(parser.nextText())
                        "LatitudeDegrees" -> if (inTrackpoint) lat = parser.nextText().toDoubleOrNull() ?: 0.0
                        "LongitudeDegrees" -> if (inTrackpoint) lon = parser.nextText().toDoubleOrNull() ?: 0.0
                        "AltitudeMeters" -> if (inTrackpoint) ele = parser.nextText().toDoubleOrNull()
                        "HeartRateBpm" -> if (inTrackpoint) inHr = true
                        "Value" -> if (inTrackpoint && inHr) hr = parser.nextText().toIntOrNull()
                        "Cadence" -> if (inTrackpoint) cad = parser.nextText().toIntOrNull()
                    }
                    XmlPullParser.END_TAG -> when (parser.name) {
                        "Lap" -> {
                            val t = currentLapTime ?: 0
                            val d = currentLapDist ?: 0.0
                            if (t > 0 || d > 0) {
                                laps += ActivityLap(
                                    laps.size + 1,
                                    t.coerceAtLeast(1),
                                    d,
                                    label = currentLapNotes?.takeIf { it.isNotBlank() }
                                )
                            }
                            inLap = false
                            currentLapNotes = null
                        }
                        "Trackpoint" -> {
                            val ts = timeMs ?: 0L
                            if (lat != 0.0 || lon != 0.0 || ts > 0L) {
                                points += GpsPoint(
                                    lat = lat,
                                    lng = lon,
                                    altitude = ele,
                                    hrBpm = hr,
                                    cadenceSpm = cad,
                                    timestampMs = ts
                                )
                            }
                            inTrackpoint = false
                            inHr = false
                        }
                        "HeartRateBpm" -> inHr = false
                    }
                }
                event = parser.next()
            }

            val lapList = laps.toMutableList()
            if (lapList.isEmpty() && (lapTimeSec != null || lapDistanceM != null)) {
                lapList += ActivityLap(
                    1,
                    (lapTimeSec ?: 1).coerceAtLeast(1),
                    lapDistanceM ?: 0.0
                )
            }
            val totalLapDist = lapList.sumOf { it.distanceM }
            val totalLapTime = lapList.sumOf { it.durationSec }
            if (points.size < 2 && totalLapDist < 25.0 && totalLapTime < 30 && lapList.isEmpty()) {
                throw IllegalArgumentException("El TCX no contiene ruta, vueltas ni duración suficiente")
            }

            val normalized = if (points.size >= 2) normalizeTimestamps(points) else emptyList()
            val type = inferType(sport, normalized)
            val startedAt = normalized.firstOrNull()?.timestampMs
                ?: (System.currentTimeMillis() - (lapTimeSec ?: 60) * 1000L)
            val endedAt = normalized.lastOrNull()?.timestampMs
                ?: (startedAt + (lapTimeSec ?: 60) * 1000L)
            val durationSec = max(
                1,
                if (lapList.isNotEmpty()) totalLapTime else lapTimeSec ?: ((endedAt - startedAt) / 1000).toInt()
            )

            var distanceM = if (totalLapDist > 0) totalLapDist else lapDistanceM ?: 0.0
            var maxSpeed = 0.0
            if (normalized.size >= 2) {
                distanceM = 0.0
                for (i in 1 until normalized.size) {
                    val seg = GpsMetrics.haversineMeters(normalized[i - 1], normalized[i])
                    distanceM += seg
                    val dt = (normalized[i].timestampMs - normalized[i - 1].timestampMs) / 1000.0
                    if (dt > 0) maxSpeed = max(maxSpeed, seg / dt)
                }
            }
            if (distanceM < 25.0 && durationSec < 30 && lapList.isEmpty()) {
                throw IllegalArgumentException("La actividad es demasiado corta")
            }

            val movingSec = durationSec
            val avgSpeed = if (movingSec > 0) distanceM / movingSec else 0.0
            val avgPace = GpsMetrics.paceSecPerKm(distanceM, movingSec) ?: 0.0
            val (elevGain, elevLoss) = if (normalized.isNotEmpty()) {
                GpsMetrics.elevationStats(normalized)
            } else 0.0 to 0.0
            val alts = normalized.mapNotNull { it.altitude }
            val splits = if (normalized.isNotEmpty()) GpsMetrics.computeSplits(normalized) else emptyList()
            val paces = splits.map { it.paceSecPerKm }.filter { it > 0 }
            val hrVals = normalized.mapNotNull { it.hrBpm }
            val cadVals = normalized.mapNotNull { it.cadenceSpm }

            ActivitySummary(
                id = "tcx_${startedAt}_${distanceM.toInt()}",
                type = type,
                startedAtMs = startedAt,
                endedAtMs = endedAt,
                durationSec = durationSec,
                movingSec = movingSec,
                distanceM = distanceM,
                avgSpeedMps = avgSpeed,
                maxSpeedMps = maxSpeed,
                avgPaceSecPerKm = avgPace,
                minPaceSecPerKm = paces.minOrNull() ?: avgPace,
                maxPaceSecPerKm = paces.maxOrNull() ?: avgPace,
                elevationGainM = elevGain,
                elevationLossM = elevLoss,
                elevationMinM = alts.minOrNull(),
                elevationMaxM = alts.maxOrNull(),
                avgCadenceSpm = cadVals.takeIf { it.isNotEmpty() }?.average(),
                maxCadenceSpm = cadVals.maxOrNull(),
                avgHrBpm = hrVals.takeIf { it.isNotEmpty() }?.average()?.toInt(),
                maxHrBpm = hrVals.maxOrNull(),
                minHrBpm = hrVals.minOrNull(),
                kcal = GpsMetrics.estimateKcal(type, movingSec, distanceM, null),
                splits = splits,
                splitsMile = if (normalized.isNotEmpty()) GpsMetrics.computeSplitsMile(normalized) else emptyList(),
                splitsTime = if (normalized.isNotEmpty()) GpsMetrics.computeSplitsTime(normalized) else emptyList(),
                bestSegments = if (normalized.isNotEmpty()) GpsMetrics.computeBestSegments(normalized, distanceM) else emptyList(),
                points = normalized,
                laps = lapList
            )
        }
    }

    private fun normalizeTimestamps(points: List<GpsPoint>): List<GpsPoint> {
        val hasTimes = points.any { it.timestampMs > 0 }
        if (hasTimes) return points.sortedBy { it.timestampMs }
        val base = System.currentTimeMillis() - points.size * 1000L
        return points.mapIndexed { i, p -> p.copy(timestampMs = base + i * 1000L) }
    }

    private fun parseTime(raw: String): Long {
        val t = raw.trim()
        return runCatching { ISO_MS.parse(t)?.time }.getOrNull()
            ?: runCatching { ISO_Z.parse(t)?.time }.getOrNull()
            ?: runCatching { ISO_OFFSET.parse(t)?.time }.getOrNull()
            ?: 0L
    }

    private fun inferType(sport: String?, points: List<GpsPoint>): ActivityType {
        val s = sport?.lowercase(Locale.US).orEmpty()
        when {
            s.contains("bik") || s.contains("cycl") -> return ActivityType.BIKE
            s.contains("walk") || s.contains("hike") -> return ActivityType.WALK
            s.contains("run") -> return ActivityType.RUN
        }
        if (points.size >= 2) {
            val dur = (points.last().timestampMs - points.first().timestampMs) / 1000.0
            if (dur > 0) {
                var dist = 0.0
                for (i in 1 until points.size) dist += GpsMetrics.haversineMeters(points[i - 1], points[i])
                val mps = dist / dur
                return when {
                    mps >= 4.5 -> ActivityType.BIKE
                    mps >= 2.2 -> ActivityType.RUN
                    else -> ActivityType.WALK
                }
            }
        }
        return ActivityType.RUN
    }
}
