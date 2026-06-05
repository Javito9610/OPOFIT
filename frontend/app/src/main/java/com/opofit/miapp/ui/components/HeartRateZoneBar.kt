package com.opofit.miapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opofit.miapp.gps.util.HeartRateZones

@Composable
fun HeartRateZoneLive(
    bpm: Int?,
    edad: Int? = null,
    modifier: Modifier = Modifier
) {
    if (bpm == null) return
    val status = HeartRateZones.status(bpm, edad)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                HeartRateZones.formatZoneLabel(status),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = status.zone.color
            )
            Text(
                "$bpm bpm",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HeartRateZoneStrip(activeZone = status.zone.number, modifier = Modifier.fillMaxWidth())
        Text(
            "FC máx estimada: ${status.maxHr} bpm (${HeartRateZones.formatZoneRange(status.zone)})",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun HeartRateZoneBreakdown(
    samples: List<Int>,
    edad: Int? = null,
    modifier: Modifier = Modifier
) {
    if (samples.isEmpty()) return
    val distribution = HeartRateZones.timeInZones(samples, edad)
    val total = distribution.sumOf { it.second }.coerceAtLeast(1)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Tiempo por zona cardíaca",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        HeartRateZoneStrip(
            activeZone = HeartRateZones.status(samples.last(), edad).zone.number,
            modifier = Modifier.fillMaxWidth()
        )
        distribution.forEach { (zone, count) ->
            val pct = (count * 100) / total
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .weight(0.15f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(zone.color)
                )
                Text(
                    "Z${zone.number} ${zone.name}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(0.45f)
                )
                Text(
                    "${zone.minPct}–${zone.maxPct}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(0.2f)
                )
                Text(
                    "$pct%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(0.2f)
                )
            }
        }
    }
}

@Composable
fun HeartRateZoneStrip(
    activeZone: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp)),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        HeartRateZones.zones.forEach { zone ->
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        if (zone.number == activeZone) zone.color
                        else zone.color.copy(alpha = 0.28f)
                    )
            )
        }
    }
}
