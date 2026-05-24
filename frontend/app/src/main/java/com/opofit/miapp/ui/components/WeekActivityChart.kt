package com.opofit.miapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opofit.miapp.data.responsemodels.DiaActividad

@Composable
fun WeekActivityChart(
    dias: List<DiaActividad>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Actividad últimos 7 días",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            if (dias.isEmpty()) {
                Text(
                    "Sin entrenos esta semana — ¡empieza hoy!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val maxVal = (dias.maxOfOrNull { it.sesiones } ?: 1).coerceAtLeast(1)
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .padding(top = 8.dp)
                ) {
                    val barCount = dias.size.coerceAtLeast(1)
                    val gap = size.width * 0.04f
                    val barWidth = (size.width - gap * (barCount + 1)) / barCount
                    val primary = androidx.compose.ui.graphics.Color(0xFF1565C0)

                    dias.forEachIndexed { i, dia ->
                        val h = (dia.sesiones.toFloat() / maxVal) * size.height * 0.85f
                        val left = gap + i * (barWidth + gap)
                        val top = size.height - h
                        drawRoundRect(
                            color = primary,
                            topLeft = Offset(left, top),
                            size = Size(barWidth, h.coerceAtLeast(4f)),
                            cornerRadius = CornerRadius(8f, 8f)
                        )
                    }
                }
                RowLabels(dias)
            }
        }
    }
}

@Composable
private fun RowLabels(dias: List<DiaActividad>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        dias.forEach { d ->
            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                Text(
                    d.etiqueta ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (d.sesiones > 0) {
                    Text(
                        "${d.sesiones}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
