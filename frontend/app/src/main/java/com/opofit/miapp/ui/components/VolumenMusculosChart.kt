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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opofit.miapp.data.responsemodels.VolumenGrupoMuscular

/**
 * Bar chart horizontal estilo Hevy "Muscle Distribution".
 *
 * Cada fila es un grupo muscular con una barra proporcional a las series
 * registradas en el periodo. El grupo con más series ocupa el ancho completo.
 */
@Composable
fun VolumenMusculosChart(
    datos: List<VolumenGrupoMuscular>,
    modifier: Modifier = Modifier
) {
    if (datos.isEmpty()) return
    val maxSeries = datos.maxOf { it.series }.coerceAtLeast(1)
    val totalSeries = datos.sumOf { it.series }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "Volumen por grupo muscular",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "$totalSeries series totales en el periodo — útil para detectar desbalances push/pull o pierna débil.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        datos.forEach { v ->
            val pct = v.series.toFloat() / maxSeries.toFloat()
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Etiqueta del grupo (ancho fijo para alinear barras).
                Text(
                    v.grupo,
                    modifier = Modifier.width(96.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                // Barra con relleno proporcional. El track gris claro permite
                // ver siempre el ancho máximo.
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(18.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(9.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(pct)
                            .background(
                                colorGrupoMuscular(v.grupo),
                                RoundedCornerShape(9.dp)
                            )
                    )
                }
                // Cifra con padding fijo a la derecha.
                Text(
                    "${v.series}",
                    modifier = Modifier.width(38.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Paleta consistente con los grupos musculares más comunes. Reusamos colores
 * del sistema cuando podemos para no romper coherencia visual.
 */
@Composable
private fun colorGrupoMuscular(grupo: String): Color = when (grupo.lowercase()) {
    "pecho" -> Color(0xFFE57373)
    "espalda" -> Color(0xFF64B5F6)
    "pierna", "pierna", "cuádriceps", "cuadriceps", "isquios" -> Color(0xFF81C784)
    "hombros", "hombro" -> Color(0xFFFFB74D)
    "brazos", "bíceps", "biceps", "tríceps", "triceps" -> Color(0xFFBA68C8)
    "core", "abdominales" -> Color(0xFF4DB6AC)
    "glúteo", "gluteo", "glúteos" -> Color(0xFFF06292)
    "cardio" -> Color(0xFF26C6DA)
    else -> MaterialTheme.colorScheme.primary
}
