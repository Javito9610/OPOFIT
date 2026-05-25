package com.opofit.miapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.opofit.miapp.data.responsemodels.DiaCalendario
import java.time.YearMonth

@Composable
fun PlanCalendarioMes(
    year: Int,
    month: Int,
    dias: List<DiaCalendario>,
    onDiaClick: (DiaCalendario) -> Unit,
    modifier: Modifier = Modifier
) {
    val ym = YearMonth.of(year, month)
    val primerDiaSemana = ym.atDay(1).dayOfWeek.value
    val offset = if (primerDiaSemana == 7) 0 else primerDiaSemana
    val mapa = dias.associateBy { it.dia }

    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("L", "M", "X", "J", "V", "S", "D").forEach {
                Text(it, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
        }
        val celdas = mutableListOf<Int?>()
        repeat(offset) { celdas.add(null) }
        for (d in 1..ym.lengthOfMonth()) celdas.add(d)
        while (celdas.size % 7 != 0) celdas.add(null)

        celdas.chunked(7).forEach { semana ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                semana.forEach { num ->
                    Box(Modifier.weight(1f).aspectRatio(1f)) {
                        if (num != null) {
                            val info = mapa[num]
                            CalendarioCelda(
                                dia = num,
                                info = info,
                                onClick = { info?.let { if (it.tiene_entreno) onDiaClick(it) } }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarioCelda(
    dia: Int,
    info: DiaCalendario?,
    onClick: () -> Unit
) {
    val tiene = info?.tiene_entreno == true
    val completada = info?.completada == true
    val esHoy = info?.es_hoy == true
    val colorFondo = when {
        completada -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        tiene && esHoy -> MaterialTheme.colorScheme.primaryContainer
        tiene -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
        else -> MaterialTheme.colorScheme.surface
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(colorFondo)
            .then(if (esHoy) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small) else Modifier)
            .clickable(enabled = tiene, onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "$dia",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (esHoy) FontWeight.Bold else FontWeight.Normal
        )
        if (tiene) {
            val emoji = when (info?.enfoque) {
                "FUERZA" -> "💪"
                "RESISTENCIA" -> "🏃"
                "VELOCIDAD" -> "⚡"
                else -> "•"
            }
            Text(emoji, style = MaterialTheme.typography.labelSmall)
            if (completada) {
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}
