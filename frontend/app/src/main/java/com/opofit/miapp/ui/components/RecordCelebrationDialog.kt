package com.opofit.miapp.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opofit.miapp.data.responsemodels.RecordRotoItem
import com.opofit.miapp.utils.EntrenoValidation

/**
 * Diálogo de "nuevo récord personal" estilo Strong / Hevy.
 *
 * Mejoras respecto a la versión anterior:
 *   - SIEMPRE muestra el nombre real del ejercicio (no "Ejercicio 140").
 *   - Trofeo animado con pulso suave.
 *   - Cada récord se renderiza como tarjeta: nombre + comparación anterior→nuevo
 *     con unidad legible (seg/reps/kg/km) y delta de mejora en %.
 *   - Botón secundario "Compartir" además del "Cerrar" para empujar al usuario
 *     a publicar (Strava/Hevy hacen esto y aumenta engagement).
 *   - Scroll si hay muchos PRs (un día bueno pueden caer 5-6).
 */
@Composable
fun RecordCelebrationDialog(
    records: List<RecordRotoItem>,
    onDismiss: () -> Unit,
    onShare: (() -> Unit)? = null
) {
    if (records.isEmpty()) return

    // Animación de pulso del trofeo (suave, no agresivo).
    val infinite = rememberInfiniteTransition(label = "trofeo")
    val scale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "trofeoScale"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .scale(scale)
                        .background(
                            color = Color(0xFFFFD54F).copy(alpha = 0.25f),
                            shape = RoundedCornerShape(28.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.EmojiEvents,
                        contentDescription = null,
                        tint = Color(0xFFFFA000),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val titulo = if (records.size == 1) "¡Nuevo récord personal!"
                else "¡${records.size} nuevos récords!"
                Text(
                    titulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    "Has superado tu mejor marca anterior",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 340.dp)
                ) {
                    items(records, key = { it.idEjercicio }) { r ->
                        PrCardRow(r)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("¡Genial!")
            }
        },
        dismissButton = if (onShare != null) {
            {
                OutlinedButton(
                    onClick = onShare,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Compartir")
                }
            }
        } else null
    )
}

@Composable
private fun PrCardRow(r: RecordRotoItem) {
    val unidadLegible = EntrenoValidation.unidadLegible(
        // El scoreTipo "tiempo" o "tiempo_max" → "seg"; "peso" → "kg"; otros → "reps".
        when (r.scoreTipo) {
            "tiempo", "tiempo_max" -> "s"
            "peso" -> "kg"
            "distancia" -> "m"
            "calorias" -> "kcal"
            "rondas", "rondas_completadas" -> "rondas"
            "rondas_reps" -> "reps"
            else -> "reps"
        }
    )
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                r.nombreEjercicio,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Anterior",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${formatPr(r.valorAnterior)} $unidadLegible",
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.Filled.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        "Ahora",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "${formatPr(r.valorNuevo)} $unidadLegible",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            // Delta de mejora (+8.5%, etc.) si está disponible.
            r.mejoraPorcentaje?.let { pct ->
                val sign = if (pct >= 0) "+" else ""
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (pct >= 0) Color(0xFF388E3C).copy(alpha = 0.18f)
                            else Color(0xFFD32F2F).copy(alpha = 0.18f)
                ) {
                    Text(
                        "$sign$pct% vs marca anterior",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (pct >= 0) Color(0xFF1B5E20) else Color(0xFFB71C1C),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

private fun formatPr(v: Double?): String {
    if (v == null) return "—"
    return if (v == v.toLong().toDouble()) v.toLong().toString()
    else "%.1f".format(v)
}
