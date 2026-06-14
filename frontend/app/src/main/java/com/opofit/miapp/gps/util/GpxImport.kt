package com.opofit.miapp.gps.util

import android.util.Xml
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
 * Importa actividades desde ficheros GPX 1.1 (Strava, Garmin, Wikiloc, Polar Flow, etc.)
 * sin necesitar API de terceros.
 */
object GpxImport {

    private val ISO_Z = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val ISO_OFFSET = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)

    fun parse(stream: InputStream): Result<ActivitySummary> {
        return runCatching {
            val parser = Xml.newPullParser()
            parser.setInput(stream, "UTF-8")
            var event = parser.eventType
            val points = mutableListOf<GpsPoint>()
            var trackType: String? = null
            var inTrkpt = false
            var lat = 0.0
            var lon = 0.0
            var ele: Double? = null
            var timeMs: Long? = null
            var hr: Int? = null
            var cad: Int? = null
            var inExtensions = false
            var inTpx = false

            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> when (parser.name) {
                        "trkpt" -> {
                            inTrkpt = true
                            lat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull() ?: 0.0
                            lon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull() ?: 0.0
                            ele = null; timeMs = null; hr = null; cad = null
                        }
                        "type" -> if (!inTrkpt) trackType = parser.nextText().trim()
                        "ele" -> if (inTrkpt) ele = parser.nextText().toDoubleOrNull()
                        "time" -> if (inTrkpt) timeMs = parseTime(parser.nextText())
                        "extensions" -> if (inTrkpt) inExtensions = true
                        "TrackPointExtension", "gpxtpx:TrackPointExtension" -> if (inTrkpt) inTpx = true
                        "hr", "gpxtpx:hr" -> if (inTrkpt && inTpx) hr = parser.nextText().toIntOrNull()
                        "cad", "gpxtpx:cad" -> if (inTrkpt && inTpx) cad = parser.nextText().toIntOrNull()
                    }
                    XmlPullParser.END_TAG -> when (parser.name) {
                        "trkpt" -> {
                            val ts = timeMs ?: 0L
                            if (lat != 0.0 || lon != 0.0) {
                                points += GpsPoint(
                                    lat = lat,
                                    lng = lon,
                                    altitude = ele,
                                    hrBpm = hr,
                                    cadenceSpm = cad,
                                    timestampMs = ts
                                )
                            }
                            inTrkpt = false
                            inExtensions = false
                            inTpx = false
                        }
                        "extensions" -> inExtensions = false
                        "TrackPointExtension", "gpxtpx:TrackPointExtension" -> inTpx = false
                    }
                }
                event = parser.next()
            }

            if (points.size < 2) throw IllegalArgumentException("El GPX no tiene suficientes puntos GPS")

            // Si faltan timestamps, interpolamos 1 s entre puntos.
            val normalized = normalizeTimestamps(points)
            val type = inferType(trackType, normalized)
            val startedAt = normalized.first().timestampMs
            val endedAt = normalized.last().timestampMs
            val durationSec = max(1, ((endedAt - startedAt) / 1000).toInt())

            var distanceM = 0.0
            var maxSpeed = 0.0
            for (i in 1 until normalized.size) {
                val seg = GpsMetrics.haversineMeters(normalized[i - 1], normalized[i])
                distanceM += seg
                val dt = (normalized[i].timestampMs - normalized[i - 1].timestampMs) / 1000.0
                if (dt > 0) maxSpeed = max(maxSpeed, seg / dt)
            }
            if (distanceM < 25.0) throw IllegalArgumentException("La ruta es demasiado corta (menos de 25 m)")

            val movingSec = durationSec
            val avgSpeed = if (movingSec > 0) distanceM / movingSec else 0.0
            val avgPace = GpsMetrics.paceSecPerKm(distanceM, movingSec) ?: 0.0
            val (elevGain, elevLoss) = GpsMetrics.elevationStats(normalized)
            val alts = normalized.mapNotNull { it.altitude }
            val splits = GpsMetrics.computeSplits(normalized)
            val paces = splits.map { it.paceSecPerKm }.filter { it > 0 }
            val hrVals = normalized.mapNotNull { it.hrBpm }
            val cadVals = normalized.mapNotNull { it.cadenceSpm }

            val id = "gpx_${startedAt}_${normalized.size}_${distanceM.toInt()}"

            ActivitySummary(
                id = id,
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
                splitsMile = GpsMetrics.computeSplitsMile(normalized),
                splitsTime = GpsMetrics.computeSplitsTime(normalized),
                bestSegments = GpsMetrics.computeBestSegments(normalized, distanceM),
                points = normalized
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
        return runCatching { ISO_Z.parse(t)?.time }.getOrNull()
            ?: runCatching { ISO_OFFSET.parse(t)?.time }.getOrNull()
            ?: 0L
    }

    private fun inferType(trackType: String?, points: List<GpsPoint>): ActivityType {
        val t = trackType?.lowercase(Locale.US).orEmpty()
        when {
            t.contains("cycl") || t.contains("bike") || t.contains("bici") ||
            t.contains("biking") || t.contains("cycling") || t.contains("ciclismo") ||
            t.contains("mtb") || t.contains("ride") -> return ActivityType.BIKE
            t.contains("walk") || t.contains("hike") || t.contains("andar") ||
            t.contains("hiking") || t.contains("caminar") -> return ActivityType.WALK
            t.contains("run") || t.contains("corr") || t.contains("running") -> return ActivityType.RUN
        }
        // Heuristica por velocidad media si no hay tipo en el GPX.
        // Umbral bici: 3.5 m/s (12.6 km/h) para cubrir bici de montaña y ritmos tranquilos.
        if (points.size >= 2) {
            val start = points.first().timestampMs
            val end = points.last().timestampMs
            val dur = (end - start) / 1000.0
            if (dur > 0) {
                var dist = 0.0
                for (i in 1 until points.size) dist += GpsMetrics.haversineMeters(points[i - 1], points[i])
                val mps = dist / dur
                return when {
                    mps >= 3.5 -> ActivityType.BIKE
                    mps >= 2.2 -> ActivityType.RUN
                    else -> ActivityType.WALK
                }
            }
        }
        return ActivityType.RUN
    }
}
