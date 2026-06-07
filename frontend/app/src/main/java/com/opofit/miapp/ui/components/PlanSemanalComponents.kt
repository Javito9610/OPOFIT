package com.opofit.miapp.ui.components

import com.opofit.miapp.ui.components.ElevatedCard
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.opofit.miapp.ui.theme.AccentOrange
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opofit.miapp.data.responsemodels.DiaPlan
import com.opofit.miapp.data.responsemodels.EjercicioPlan
import com.opofit.miapp.data.responsemodels.PersonalizacionPlan

fun enfoqueEmoji(enfoque: String): String = when (enfoque.uppercase()) {
    "FUERZA" -> "💪"
    "RESISTENCIA" -> "🏃"
    "VELOCIDAD" -> "⚡"
    else -> "📋"
}

fun enfoqueLabel(enfoque: String): String = when (enfoque.uppercase()) {
    "FUERZA" -> "Fuerza"
    "RESISTENCIA" -> "Resistencia"
    "VELOCIDAD" -> "Velocidad"
    else -> enfoque
}

@Composable
fun PlanMetricMini(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun PlanEjercicioRow(
    prescripcion: String,
    nombre: String,
    destacado: Boolean,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ExerciseInfoButton(onClick = onInfoClick, size = 36.dp)
        Surface(
            shape = MaterialTheme.shapes.small,
            color = if (destacado) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        ) {
            Text(
                prescripcion,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (destacado) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        Text(
            nombre,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (destacado) FontWeight.Medium else FontWeight.Normal,
            color = if (destacado) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
            maxLines = 2
        )
    }
}

@Composable
fun PlanSemanaResumenRow(
    dias: List<DiaPlan>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        dias.forEach { dia ->
            val abbr = dia.nombre_dia.take(3)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    enfoqueEmoji(dia.enfoque),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    abbr,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (dia.es_hoy) FontWeight.Bold else FontWeight.Normal,
                    color = if (dia.es_hoy) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    if (dia.completada) "✓" else "·",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (dia.completada) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun PlanDiaCard(
    dia: DiaPlan,
    modifier: Modifier = Modifier,
    onEntrenar: (enfoque: String, idPlanDia: Int, idRutinaOpo: Int) -> Unit,
    expanded: Boolean = false,
    onOtraOpcion: (() -> Unit)? = null,
    regenerando: Boolean = false
) {
    var dragTotal by remember(dia.id_plan_dia) { mutableFloatStateOf(0f) }
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(dia.id_plan_dia, onOtraOpcion, dia.completada) {
                if (onOtraOpcion != null && !dia.completada) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (dragTotal < -100f) onOtraOpcion()
                            dragTotal = 0f
                        },
                        onHorizontalDrag = { _, amount -> dragTotal += amount }
                    )
                }
            }
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        dia.nombre_dia,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    AssistChip(
                        onClick = {},
                        label = { Text("${enfoqueEmoji(dia.enfoque)} ${enfoqueLabel(dia.enfoque)}") }
                    )
                    Text(
                        dia.titulo,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
                if (dia.completada) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Completada",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                } else if (dia.es_hoy) {
                    Text(
                        "HOY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlanMetricMini("Ejercicios", "${dia.ejercicios.size}")
                if (dia.completada) {
                    PlanMetricMini("Estado", "Hecho ✓")
                } else if (dia.es_hoy) {
                    PlanMetricMini("Estado", "Hoy")
                }
            }
            if (!dia.completada && onOtraOpcion != null) {
                FilledTonalButton(
                    onClick = onOtraOpcion,
                    enabled = !regenerando,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (regenerando) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Filled.SwapHoriz, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (regenerando) "Generando…" else "Otra variante ←")
                }
            }
            if (dia.es_hoy && !dia.completada) {
                Button(
                    onClick = { onEntrenar(dia.enfoque, dia.id_plan_dia, dia.id_rutina_opo) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Empezar entreno de hoy")
                }
            } else if (!dia.completada && expanded) {
                OutlinedButton(
                    onClick = { onEntrenar(dia.enfoque, dia.id_plan_dia, dia.id_rutina_opo) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Entrenar ${enfoqueLabel(dia.enfoque).lowercase()}")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlanPersonalizacionCard(
    personalizacion: PersonalizacionPlan,
    modifier: Modifier = Modifier
) {
    var expandido by remember { mutableStateOf(false) }
    val textoIa = personalizacion.explicacion_ia ?: personalizacion.resumen
    ElevatedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "  Tu plan inteligente",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                coachingFuenteLabel(personalizacion.coaching_fuente)?.let { label ->
                    AssistChip(
                        onClick = {},
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        enabled = false,
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        )
                    )
                }
            }
            if (!personalizacion.entorno_etiqueta.isNullOrBlank()) {
                AssistChip(
                    onClick = {},
                    label = { Text("${personalizacion.entorno_emoji ?: ""} ${personalizacion.entorno_etiqueta}") }
                )
            }
            Text(
                textoIa,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (expandido) Int.MAX_VALUE else 2
            )
            if (textoIa.length > 80) {
                TextButton(onClick = { expandido = !expandido }) {
                    Text(if (expandido) "Ver menos" else "Ver explicación completa")
                }
            }
            if (personalizacion.pilares_debiles.isNotEmpty() || personalizacion.pilares_fuertes.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    personalizacion.pilares_debiles.forEach { p ->
                        AssistChip(
                            onClick = {},
                            label = { Text("↑ ${p.etiqueta ?: p.pilar} ${String.format("%.1f", p.notaMedia)}") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                            )
                        )
                    }
                    personalizacion.pilares_fuertes.forEach { p ->
                        AssistChip(
                            onClick = {},
                            label = { Text("✓ ${p.etiqueta ?: p.pilar}") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                            )
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PlanMetricMini("Adaptados", "${personalizacion.ajustes_aplicados}")
                personalizacion.sustituciones?.takeIf { it > 0 }?.let {
                    PlanMetricMini("Sustituidos", "$it", Modifier.weight(1f))
                }
                PlanMetricMini("Racha", "${personalizacion.racha_dias}d", Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun PlanSesionActivaCard(
    titulo: String,
    nombreDia: String?,
    enfoque: String,
    completados: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(enfoqueEmoji(enfoque), style = MaterialTheme.typography.headlineMedium)
                Column(Modifier.weight(1f)) {
                    AssistChip(
                        onClick = {},
                        label = { Text(enfoqueLabel(enfoque)) }
                    )
                    Text(
                        titulo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )
                    nombreDia?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (total > 0) {
                LinearProgressIndicator(
                    progress = { completados.toFloat() / total },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "$completados de $total ejercicios",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
fun EntrenoHoyHeroCard(
    titulo: String,
    subtitulo: String,
    enfoque: String,
    onEmpezar: () -> Unit,
    onPrepararRuta: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primary
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(AccentOrange)
            )
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                titulo,
                style = MaterialTheme.typography.labelLarge,
                color = AccentOrange.copy(alpha = 0.95f)
            )
            Text(
                subtitulo,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                "${enfoqueEmoji(enfoque)} ${enfoqueLabel(enfoque)}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
            )
            Button(
                onClick = onEmpezar,
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = AccentOrange,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Empezar ahora", fontWeight = FontWeight.Bold)
            }
            if (onPrepararRuta != null && enfoque.equals("RESISTENCIA", ignoreCase = true)) {
                androidx.compose.material3.OutlinedButton(
                    onClick = onPrepararRuta,
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Preparar ruta de carrera")
                }
            }
            }
        }
    }
}

private fun coachingFuenteLabel(fuente: String?): String? = when (fuente?.lowercase()) {
    "openai", "gemini" -> "Coaching IA"
    "reglas" -> "Coaching automático"
    "cache" -> null
    else -> null
}
