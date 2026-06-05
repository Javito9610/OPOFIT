package com.opofit.miapp.gps.data

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.opofit.miapp.gps.model.ActivitySummary
import com.opofit.miapp.gps.model.ActivityType
import com.opofit.miapp.gps.model.BestSegment
import com.opofit.miapp.gps.model.GpsPoint
import com.opofit.miapp.gps.model.SplitKm
import com.opofit.miapp.gps.model.SplitMile
import com.opofit.miapp.gps.model.SplitTime

/** Convierte respuestas JSON del servidor a [ActivitySummary] local. */
object GpsRemoteSync {
    private val gson = Gson()

    fun fromDetalle(json: JsonObject): ActivitySummary? {
        return try {
            val id = json.get("id")?.asString ?: return null
            val type = ActivityType.fromName(json.get("type")?.asString)
            ActivitySummary(
                id = id,
                type = type,
                startedAtMs = json.get("startedAtMs")?.asLong ?: 0L,
                endedAtMs = json.get("endedAtMs")?.asLong ?: 0L,
                durationSec = json.get("durationSec")?.asInt ?: 0,
                movingSec = json.get("movingSec")?.asInt ?: 0,
                distanceM = json.get("distanceM")?.asDouble ?: 0.0,
                avgSpeedMps = json.get("avgSpeedMps")?.asDouble ?: 0.0,
                maxSpeedMps = json.get("maxSpeedMps")?.asDouble ?: 0.0,
                avgPaceSecPerKm = json.get("avgPaceSecPerKm")?.asDouble ?: 0.0,
                minPaceSecPerKm = json.get("minPaceSecPerKm")?.asDouble ?: 0.0,
                maxPaceSecPerKm = json.get("maxPaceSecPerKm")?.asDouble ?: 0.0,
                elevationGainM = json.get("elevationGainM")?.asDouble ?: 0.0,
                elevationLossM = json.get("elevationLossM")?.asDouble ?: 0.0,
                elevationMinM = json.get("elevationMinM")?.takeIf { !it.isJsonNull }?.asDouble,
                elevationMaxM = json.get("elevationMaxM")?.takeIf { !it.isJsonNull }?.asDouble,
                avgCadenceSpm = json.get("avgCadenceSpm")?.takeIf { !it.isJsonNull }?.asDouble,
                maxCadenceSpm = json.get("maxCadenceSpm")?.takeIf { !it.isJsonNull }?.asInt,
                avgHrBpm = json.get("avgHrBpm")?.takeIf { !it.isJsonNull }?.asInt,
                maxHrBpm = json.get("maxHrBpm")?.takeIf { !it.isJsonNull }?.asInt,
                minHrBpm = json.get("minHrBpm")?.takeIf { !it.isJsonNull }?.asInt,
                kcal = json.get("kcal")?.takeIf { !it.isJsonNull }?.asInt,
                avgStrideM = json.get("avgStrideM")?.takeIf { !it.isJsonNull }?.asDouble,
                avgInclinePct = json.get("avgInclinePct")?.takeIf { !it.isJsonNull }?.asDouble,
                points = parsePoints(json.getAsJsonArray("points")),
                splits = parseSplits(json.getAsJsonArray("splits")),
                splitsMile = parseSplitsMile(json.getAsJsonArray("splitsMile")),
                splitsTime = parseSplitsTime(json.getAsJsonArray("splitsTime")),
                bestSegments = parseBestSegments(json.getAsJsonArray("bestSegments")),
                syncedRemoteId = json.get("idRemoto")?.takeIf { !it.isJsonNull }?.asInt
            )
        } catch (_: Exception) {
            null
        }
    }

    fun fromDetalleJson(raw: String): ActivitySummary? =
        runCatching { fromDetalle(gson.fromJson(raw, JsonObject::class.java)) }.getOrNull()

    private fun parsePoints(arr: com.google.gson.JsonArray?): List<GpsPoint> {
        if (arr == null) return emptyList()
        return arr.mapNotNull { el ->
            val o = el.asJsonObject
            GpsPoint(
                lat = o.get("lat")?.asDouble ?: return@mapNotNull null,
                lng = o.get("lng")?.asDouble ?: return@mapNotNull null,
                altitude = o.get("altitude")?.takeIf { !it.isJsonNull }?.asDouble,
                speedMps = o.get("speedMps")?.takeIf { !it.isJsonNull }?.asFloat,
                accuracyM = o.get("accuracyM")?.takeIf { !it.isJsonNull }?.asFloat,
                cadenceSpm = o.get("cadenceSpm")?.takeIf { !it.isJsonNull }?.asInt,
                hrBpm = o.get("hrBpm")?.takeIf { !it.isJsonNull }?.asInt,
                timestampMs = o.get("timestampMs")?.asLong ?: 0L
            )
        }
    }

    private fun parseSplits(arr: com.google.gson.JsonArray?): List<SplitKm> {
        if (arr == null) return emptyList()
        return arr.mapNotNull { el ->
            val o = el.asJsonObject
            SplitKm(
                km = o.get("km")?.asInt ?: return@mapNotNull null,
                durationSec = o.get("durationSec")?.asInt ?: 0,
                paceSecPerKm = o.get("paceSecPerKm")?.asDouble ?: 0.0,
                elevationGainM = o.get("elevationGainM")?.asDouble ?: 0.0,
                avgHrBpm = o.get("avgHrBpm")?.takeIf { !it.isJsonNull }?.asInt,
                avgCadenceSpm = o.get("avgCadenceSpm")?.takeIf { !it.isJsonNull }?.asInt
            )
        }
    }

    private fun parseSplitsMile(arr: com.google.gson.JsonArray?): List<SplitMile> {
        if (arr == null) return emptyList()
        return arr.mapNotNull { el ->
            val o = el.asJsonObject
            SplitMile(
                mile = o.get("mile")?.asInt ?: return@mapNotNull null,
                durationSec = o.get("durationSec")?.asInt ?: 0,
                paceSecPerMi = o.get("paceSecPerMi")?.asDouble ?: 0.0,
                elevationGainM = o.get("elevationGainM")?.asDouble ?: 0.0
            )
        }
    }

    private fun parseSplitsTime(arr: com.google.gson.JsonArray?): List<SplitTime> {
        if (arr == null) return emptyList()
        return arr.mapNotNull { el ->
            val o = el.asJsonObject
            SplitTime(
                index = o.get("index")?.asInt ?: return@mapNotNull null,
                durationSec = o.get("durationSec")?.asInt ?: 0,
                distanceM = o.get("distanceM")?.asDouble ?: 0.0,
                avgPaceSecPerKm = o.get("avgPaceSecPerKm")?.asDouble ?: 0.0
            )
        }
    }

    private fun parseBestSegments(arr: com.google.gson.JsonArray?): List<BestSegment> {
        if (arr == null) return emptyList()
        return arr.mapNotNull { el ->
            val o = el.asJsonObject
            BestSegment(
                label = o.get("label")?.asString ?: return@mapNotNull null,
                distanceM = o.get("distanceM")?.asDouble ?: 0.0,
                durationSec = o.get("durationSec")?.asInt ?: 0,
                paceSecPerKm = o.get("paceSecPerKm")?.asDouble ?: 0.0
            )
        }
    }
}
