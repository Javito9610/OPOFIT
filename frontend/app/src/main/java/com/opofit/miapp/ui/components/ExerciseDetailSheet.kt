package com.opofit.miapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            RowTipoHeader(tipoLabel)

            Text(
                ejercicio.nombre,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                prescripcion,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

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
            }

            HorizontalDivider()
            Text("Cómo hacerlo", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                EntrenoExerciseUtil.deduplicarInstrucciones(ejercicio.instrucciones_tecnicas)
                    ?.takeIf { it.isNotBlank() }
                    ?: "Ejecuta el movimiento con técnica controlada y progresión según tu nivel.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

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
        }
    }
}

private fun esMotivoAjusteInterno(motivo: String): Boolean {
    val m = motivo.lowercase()
    return m.contains("mantenimiento") && m.contains("recuperacion") && !m.contains("volumen")
}

@Composable
private fun RowTipoHeader(tipoLabel: String) {
    val (icon, label) = when (tipoLabel) {
        "Cardio", "Agilidad" -> Icons.AutoMirrored.Outlined.DirectionsRun to "Ejercicio cardiovascular"
        "Core" -> Icons.Outlined.SelfImprovement to "Ejercicio de core"
        else -> Icons.Outlined.FitnessCenter to "Ejercicio de fuerza"
    }
    androidx.compose.foundation.layout.Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
