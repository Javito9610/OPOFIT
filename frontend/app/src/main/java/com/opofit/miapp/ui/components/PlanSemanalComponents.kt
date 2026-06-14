package com.opofit.miapp.ui.components

import com.opofit.miapp.ui.components.ElevatedCard
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.Timer
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

/**
 * Mantenido temporalmente como wrapper de compatibilidad — el método nuevo
 * `EnfoqueIcons.forEnfoque` devuelve un Material Icon. Los callsites que
 * todavía piden un String se eliminarán al hacer la pasada de UI premium.
 */
@Deprecated("Usa EnfoqueIcons.forEnfoque para obtener un ImageVector.")
fun enfoqueEmoji(enfoque: String): String = ""

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
    ejercicio: EjercicioPlan,
    prescripcion: String,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PlanEjercicioRow(
        prescripcion = prescripcion,
        nombre = ejercicio.nombre,
        destacado = ejercicio.personalizado || ejercicio.sustituido,
        onInfoClick = onInfoClick,
        pilar = ejercicio.pilar,
        descanso = ejercicio.descanso,
        tempo = ejercicio.tempo,
        rpe = ejercicio.rpe_objetivo,
        faseLabel = ejercicio.fase_label,
        modifier = modifier
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlanEjercicioRow(
    prescripcion: String,
    nombre: String,
    destacado: Boolean,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier,
    pilar: String? = null,
    descanso: Int? = null,
    tempo: String? = null,
    rpe: Int? = null,
    faseLabel: String? = null
) {
    // Card COMPACTA estilo Hevy/Strong: 1 sola línea con prescripción a la
    // izquierda + nombre + descanso a la derecha. Altura ~56dp. Antes era
    // ~120dp por ejercicio (icono + badge + nombre 2 líneas + 4 chips meta)
    // → el usuario decía "tanto scroll, es una mierda". Ahora la lista de un
    // día de 6 ejercicios cabe en una sola pantalla. Tap para info detallada.
    val borderColor = if (destacado) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onInfoClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 0.dp
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Badge prescripción a la izquierda: "4×8", "3×12", etc.
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (destacado) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.widthIn(min = 56.dp)
            ) {
                Text(
                    prescripcion,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (destacado) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 1
                )
            }
            // Nombre del ejercicio: 1 línea, ellipsis si no cabe.
            Text(
                nombre,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (destacado) FontWeight.SemiBold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            // Descanso a la derecha (lo más visual de la fila).
            descanso?.takeIf { it > 0 }?.let { d ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        Icons.Outlined.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        "${d}s",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // Chevron sutil indicando que es tappable.
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = "Ver detalle",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
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
                Icon(
                    imageVector = EnfoqueIcons.forEnfoque(dia.enfoque),
                    contentDescription = enfoqueLabel(dia.enfoque),
                    tint = if (dia.es_hoy) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    abbr,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (dia.es_hoy) FontWeight.Bold else FontWeight.Normal,
                    color = if (dia.es_hoy) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (dia.completada) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Completada",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp)
                    )
                } else {
                    Text(
                        "·",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
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
                        leadingIcon = {
                            Icon(
                                imageVector = EnfoqueIcons.forEnfoque(dia.enfoque),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text(enfoqueLabel(dia.enfoque)) }
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
            // Cabecera: título + chip de coaching debajo (no en SpaceBetween).
            // Antes el chip "Coaching automático" se descuadraba y partía el
            // texto en dos líneas a la derecha. Ahora título en una línea
            // limpia y el chip de fuente como subtítulo si toca.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    "Tu plan inteligente",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            // FlowRow para que entorno + nivel + fuente se reorganicen y no
            // se corten en pantallas pequeñas.
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!personalizacion.entorno_etiqueta.isNullOrBlank()) {
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                "${personalizacion.entorno_emoji ?: ""} ${personalizacion.entorno_etiqueta}",
                                maxLines = 1
                            )
                        }
                    )
                }
                // Nivel del plan (BÁSICO / INTERMEDIO / AVANZADO) — el usuario
                // pidió saber qué plan tiene. Antes no aparecía en ningún sitio.
                personalizacion.nivel_usado?.takeIf { it.isNotBlank() }?.let { nivel ->
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                "Nivel ${nivelLabel(nivel)}",
                                maxLines = 1,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                        )
                    )
                }
                coachingFuenteLabel(personalizacion.coaching_fuente)?.let { label ->
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(label, maxLines = 1, style = MaterialTheme.typography.labelSmall)
                        },
                        enabled = false,
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        )
                    )
                }
            }
            Text(
                textoIa,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                // Subido de 2 → 4 líneas colapsado. Antes el texto se cortaba
                // en "...(nota" justo antes de cerrar el paréntesis y quedaba
                // ilegible.
                maxLines = if (expandido) Int.MAX_VALUE else 4,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            if (textoIa.length > 200) {
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
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = EnfoqueIcons.forEnfoque(enfoque),
                        contentDescription = enfoqueLabel(enfoque),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(8.dp).size(28.dp)
                    )
                }
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = EnfoqueIcons.forEnfoque(enfoque),
                    contentDescription = null,
                    tint = AccentOrange,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    enfoqueLabel(enfoque),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                )
            }
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

/** Etiqueta legible para el chip de nivel del plan. */
private fun nivelLabel(nivel: String): String = when (nivel.uppercase()) {
    "BASICO", "BÁSICO" -> "Básico"
    "INTERMEDIO" -> "Intermedio"
    "AVANZADO" -> "Avanzado"
    else -> nivel.lowercase().replaceFirstChar { it.uppercase() }
}

private fun coachingFuenteLabel(fuente: String?): String? = when (fuente?.lowercase()) {
    "openai", "gemini" -> "Coaching IA"
    "reglas" -> "Coaching pro"
    "cache" -> null
    else -> null
}
