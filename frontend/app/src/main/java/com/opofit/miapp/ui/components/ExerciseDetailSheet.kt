package com.opofit.miapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opofit.miapp.data.responsemodels.EjercicioPlan
import com.opofit.miapp.utils.EntrenoExerciseUtil

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExerciseDetailSheet(
    ejercicio: EjercicioPlan?,
    prescripcion: String,
    visible: Boolean,
    onDismiss: () -> Unit
) {
    if (!visible || ejercicio == null) return

    val grupo = inferGrupoMuscular(ejercicio.nombre, ejercicio.grupo_muscular, ejercicio.pilar)
    val tipoLabel = tipoEjercicioLabel(ejercicio.tipo_ilustracion, ejercicio.nombre, ejercicio.pilar)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // contentWindowInsets a 0 → el container del sheet llega al borde inferior
    // real (no deja franja blanca de la gesture bar). El Column de dentro usa
    // navigationBarsPadding() para que el contenido no quede tapado.
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Icon(
                        EnfoqueIcons.forEnfoque(ejercicio.pilar),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(24.dp)
                    )
                }
                Column {
                    Text(
                        tipoLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ejercicio.fase_label?.takeIf { it.isNotBlank() }?.let { fase ->
                        Text(
                            fase,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            Text(
                ejercicio.nombre,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            ) {
                Column(
                    Modifier
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f)
                                )
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Prescripción",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        prescripcion,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    val proMeta = buildList {
                        if (ejercicio.descanso > 0) add("${ejercicio.descanso}s descanso")
                        ejercicio.tempo?.takeIf { it.isNotBlank() }?.let { add("Tempo $it") }
                        ejercicio.rpe_objetivo?.let { add("RPE objetivo $it") }
                        ejercicio.rango_rm?.takeIf { it.isNotBlank() }?.let { add(it) }
                        ejercicio.nota_carga?.takeIf { it.isNotBlank() }?.let { add(it) }
                    }
                    if (proMeta.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            proMeta.forEach { line ->
                                AssistChip(
                                    onClick = {},
                                    enabled = false,
                                    label = { Text(line, style = MaterialTheme.typography.labelSmall) },
                                    leadingIcon = when {
                                        line.contains("descanso", ignoreCase = true) -> {
                                            { Icon(Icons.Outlined.Timer, null, Modifier.size(16.dp)) }
                                        }
                                        line.contains("RPE", ignoreCase = true) -> {
                                            { Icon(Icons.Outlined.Speed, null, Modifier.size(16.dp)) }
                                        }
                                        line.contains("Tempo", ignoreCase = true) -> {
                                            { Icon(Icons.AutoMirrored.Outlined.TrendingUp, null, Modifier.size(16.dp)) }
                                        }
                                        else -> null
                                    }
                                )
                            }
                        }
                    }
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (grupo != "General") {
                    AssistChip(onClick = {}, label = { Text(grupo) }, enabled = false)
                }
                AssistChip(onClick = {}, label = { Text(tipoLabel) }, enabled = false)
                ejercicio.pilar?.takeIf { it.isNotBlank() }?.let { pilar ->
                    AssistChip(onClick = {}, label = { Text(pilar) }, enabled = false)
                }
                ejercicio.equipamiento?.takeIf { it.isNotBlank() && it != "—" && it != "Variable" }?.let { eq ->
                    AssistChip(onClick = {}, label = { Text(eq) }, enabled = false)
                }
                ejercicio.patron_movimiento?.takeIf { it.isNotBlank() }?.let { patron ->
                    AssistChip(onClick = {}, label = { Text(patron) }, enabled = false)
                }
            }

            HorizontalDivider()

            // Coach Pro: explicación en 5 bloques jerarquizados (Setup,
            // Ejecución, Cues, Errores, Por qué). Antes solo había un
            // párrafo plano de "Cómo hacerlo" + texto del banco — el usuario
            // se quejó de que "es una mierda". Ahora Strava/Caliber level.
            val exp = ejercicio.explicacion
            if (exp != null) {
                CoachSection(
                    titulo = "Setup",
                    contenido = exp.setup,
                    accent = MaterialTheme.colorScheme.primary
                )
                CoachSection(
                    titulo = "Ejecución",
                    contenido = exp.ejecucion,
                    accent = MaterialTheme.colorScheme.tertiary
                )
                if (exp.coaching_cues.isNotEmpty()) {
                    CoachSectionLista(
                        titulo = "Claves del entrenador",
                        items = exp.coaching_cues,
                        accent = MaterialTheme.colorScheme.primary
                    )
                }
                if (exp.errores_comunes.isNotEmpty()) {
                    CoachSectionLista(
                        titulo = "Errores a evitar",
                        items = exp.errores_comunes,
                        accent = MaterialTheme.colorScheme.error
                    )
                }
                CoachSection(
                    titulo = "Por qué entrenas esto",
                    contenido = exp.porque,
                    accent = MaterialTheme.colorScheme.secondary
                )
            } else {
                // Fallback retrocompat: ejercicios del banco legacy sin la
                // explicación nueva del backend.
                Text(
                    "Cómo hacerlo",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    EntrenoExerciseUtil.deduplicarInstrucciones(ejercicio.instrucciones_tecnicas)
                        ?.takeIf { it.isNotBlank() }
                        ?: "Ejecuta el movimiento con técnica controlada y progresión según tu nivel.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            ejercicio.objetivo?.takeIf { it.isNotBlank() }?.let { objetivo ->
                Text(
                    objetivo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            val motivoSust = ejercicio.motivo_sustitucion?.takeIf { it.isNotBlank() }
            if (motivoSust != null) {
                Text(
                    motivoSust,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            } else if (ejercicio.sustituido && !ejercicio.nombre_original.isNullOrBlank()) {
                val orig = ejercicio.nombre_original!!.trim()
                val actual = ejercicio.nombre.trim()
                if (!orig.equals(actual, ignoreCase = true)) {
                    Text(
                        "Alternativa a: $orig",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            ejercicio.motivo_ajuste?.takeIf { it.isNotBlank() && !esMotivoAjusteInterno(it) }?.let { ajuste ->
                Text(
                    ajuste,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            ejercicio.regresion?.let { reg ->
                Text(
                    "Regresión: ${reg.nombre}${reg.motivo?.let { " — $it" } ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ejercicio.progresion?.let { prog ->
                Text(
                    "Progresión: ${prog.nombre}${prog.motivo?.let { " — $it" } ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun esMotivoAjusteInterno(motivo: String): Boolean {
    val m = motivo.lowercase()
    return m.contains("mantenimiento") && m.contains("recuperacion") && !m.contains("volumen")
}

/**
 * Sección de coach pro: header + párrafo con accent vertical en la izquierda.
 * Imita el estilo de Caliber/Hevy para los bloques de explicación.
 */
@Composable
private fun CoachSection(
    titulo: String,
    contenido: String,
    accent: androidx.compose.ui.graphics.Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            titulo,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = accent
        )
        Text(
            contenido,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Lista bullet de claves/errores. Cada item con bullet y texto.
 */
@Composable
private fun CoachSectionLista(
    titulo: String,
    items: List<String>,
    accent: androidx.compose.ui.graphics.Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            titulo,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = accent
        )
        items.forEach { item ->
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "•",
                    style = MaterialTheme.typography.bodyMedium,
                    color = accent,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    item,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
