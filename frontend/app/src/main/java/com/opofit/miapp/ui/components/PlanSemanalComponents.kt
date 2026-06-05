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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import com.opofit.miapp.ui.theme.AccentOrange
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opofit.miapp.data.responsemodels.DiaPlan
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
                Column(Modifier.weight(1f)) {
                    Text(
                        "${dia.nombre_dia} · ${enfoqueEmoji(dia.enfoque)} ${enfoqueLabel(dia.enfoque)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        dia.titulo,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
            val descripcionDistinta = dia.descripcion
                ?.trim()
                ?.takeIf { it.isNotBlank() && !it.equals(dia.titulo.trim(), ignoreCase = true) }
            if (descripcionDistinta != null) {
                Text(
                    descripcionDistinta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "${dia.ejercicios.size} ejercicios en sesión",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )
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
                    Text(if (regenerando) "Generando…" else "Otra opción (desliza ← también)")
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
    ElevatedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
            if (!personalizacion.entorno_etiqueta.isNullOrBlank()) {
                Text(
                    "${personalizacion.entorno_emoji ?: ""} Entorno: ${personalizacion.entorno_etiqueta}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                personalizacion.explicacion_ia ?: personalizacion.resumen,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
            val extras = buildList {
                add("${personalizacion.ajustes_aplicados} ejercicios adaptados")
                personalizacion.sustituciones?.takeIf { it > 0 }?.let { add("$it sustituidos a tu entorno") }
                add("racha ${personalizacion.racha_dias} días")
            }
            Text(
                extras.joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
