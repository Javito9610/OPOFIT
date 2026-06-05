package com.opofit.miapp.gps.util

import androidx.compose.ui.graphics.Color

/** Zonas de frecuencia cardíaca al estilo Zepp (5 zonas por % FC máx). */
object HeartRateZones {

    data class Zone(
        val number: Int,
        val name: String,
        val minPct: Int,
        val maxPct: Int,
        val color: Color
    )

    data class ZoneStatus(
        val zone: Zone,
        val pctMax: Int,
        val bpm: Int,
        val maxHr: Int
    )

    val zones: List<Zone> = listOf(
        Zone(1, "Calentamiento", 50, 60, Color(0xFF42A5F5)),
        Zone(2, "Quema grasa", 60, 70, Color(0xFF66BB6A)),
        Zone(3, "Aeróbica", 70, 80, Color(0xFFFFCA28)),
        Zone(4, "Anaeróbica", 80, 90, Color(0xFFFF7043)),
        Zone(5, "Máxima", 90, 100, Color(0xFFE53935))
    )

    /** FC máx estimada (fórmula 220 − edad). Edad por defecto 30 si no se conoce. */
    fun maxHrBpm(edad: Int? = null): Int {
        val age = edad?.coerceIn(14, 80) ?: 30
        return (220 - age).coerceAtLeast(150)
    }

    fun pctOfMax(bpm: Int, maxHr: Int): Int {
        if (maxHr <= 0) return 0
        return ((bpm.toDouble() / maxHr) * 100).toInt().coerceIn(0, 100)
    }

    fun zoneFor(bpm: Int, maxHr: Int): Zone {
        val pct = pctOfMax(bpm, maxHr)
        return zones.lastOrNull { pct >= it.minPct } ?: zones.first()
    }

    fun status(bpm: Int, edad: Int? = null): ZoneStatus {
        val max = maxHrBpm(edad)
        val zone = zoneFor(bpm, max)
        return ZoneStatus(zone = zone, pctMax = pctOfMax(bpm, max), bpm = bpm, maxHr = max)
    }

    /** Reparto del tiempo en cada zona (para actividad guardada). */
    fun timeInZones(points: List<Int>, edad: Int? = null): List<Pair<Zone, Int>> {
        if (points.isEmpty()) return emptyList()
        val max = maxHrBpm(edad)
        val counts = IntArray(zones.size)
        points.forEach { bpm ->
            val z = zoneFor(bpm, max)
            counts[z.number - 1]++
        }
        return zones.mapIndexed { i, z -> z to counts[i] }
            .filter { it.second > 0 }
    }

    fun formatZoneLabel(status: ZoneStatus): String =
        "Z${status.zone.number} ${status.zone.name} · ${status.pctMax}%"

    fun formatZoneRange(zone: Zone): String =
        "${zone.minPct}–${zone.maxPct}% FC máx"
}
