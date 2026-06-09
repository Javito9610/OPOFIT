package com.opofit.miapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.opofit.miapp.utils.EntrenoValidation

/**
 * Input por serie (estilo Strong / Hevy / FitNotes).
 *
 * Si la prescripción es "4×12" (4 series × 12 reps), muestra 4 filas con un input
 * en cada una. El usuario anota la marca conseguida en cada serie (puede ser
 * distinta en cada una). El resultado final se serializa como "12,11,10,9".
 *
 * Si el usuario solo quiere un valor único (no por serie), el botón "Mismo valor"
 * rellena todas las series con el mismo número.
 */
@Composable
fun SeriesInput(
    seriesObjetivo: Int,
    repsObjetivo: Int?,
    unidad: String,
    valoresPorSerie: List<String>,
    onValoresChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val unidadLeg = EntrenoValidation.unidadLegible(unidad)
    val series = if (seriesObjetivo <= 0) 1 else seriesObjetivo.coerceAtMost(12)
    // Aseguramos que la lista tenga el tamaño correcto
    val valores = remember(series, valoresPorSerie) {
        (0 until series).map { idx -> valoresPorSerie.getOrNull(idx) ?: "" }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Anota cada serie ($seriesObjetivo en total)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (repsObjetivo != null && repsObjetivo > 0) {
                Text(
                    "Objetivo: $repsObjetivo $unidadLeg/serie",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        (0 until series).forEach { idx ->
            val valorActual = valores[idx]
            val completada = valorActual.isNotBlank() &&
                valorActual.replace(",", ".").toDoubleOrNull()?.let { it > 0 } == true
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (completada) MaterialTheme.colorScheme.tertiaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.shapes.small
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (completada) {
                        Icon(
                            Icons.Filled.CheckCircle, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            "${idx + 1}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                OutlinedTextField(
                    value = valorActual,
                    onValueChange = { nuevo ->
                        val limpio = if (unidad in listOf("reps", "rep")) {
                            nuevo.filter { it.isDigit() }
                        } else {
                            nuevo.filter { it.isDigit() || it == '.' || it == ',' }
                        }
                        val nueva = valores.toMutableList()
                        nueva[idx] = limpio
                        onValoresChange(nueva)
                    },
                    label = { Text("Serie ${idx + 1} ($unidadLeg)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (unidad in listOf("reps", "rep"))
                            KeyboardType.Number else KeyboardType.Decimal
                    ),
                    singleLine = true,
                    placeholder = repsObjetivo?.let { { Text("$it") } }
                )
            }
        }

        // Botón de utilidad: rellena todas las series con la primera
        if (series > 1) {
            TextButton(
                onClick = {
                    val primer = valores.firstOrNull()?.takeIf { it.isNotBlank() }
                        ?: repsObjetivo?.toString() ?: ""
                    if (primer.isNotBlank()) {
                        onValoresChange(List(series) { primer })
                    }
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Igualar todas las series")
            }
        }
    }
}

/** Convierte la lista de valores por serie a string CSV para guardar en valorConseguido. */
fun valoresPorSerieToCsv(valores: List<String>): String =
    valores.filter { it.isNotBlank() }.joinToString(",")

/** Devuelve los valores parseados desde CSV o lista vacía. */
fun csvToValoresPorSerie(csv: String, series: Int): List<String> {
    val partes = csv.split(',').map { it.trim() }
    return (0 until series).map { idx -> partes.getOrNull(idx) ?: "" }
}

/**
 * Calcula valor representativo de una serie de valores (para mostrar resumen).
 * Si todas son iguales → ese valor. Si no, devuelve "media (X-Y)" con rango.
 */
fun resumenSeries(valoresCsv: String, unidad: String): String {
    val partes = valoresCsv.split(',').mapNotNull { it.trim().replace(",", ".").toDoubleOrNull() }
    if (partes.isEmpty()) return valoresCsv
    val unidadLeg = EntrenoValidation.unidadLegible(unidad)
    if (partes.size == 1) return "${formatNum(partes[0])} $unidadLeg"
    val todasIguales = partes.distinct().size == 1
    if (todasIguales) return "${partes.size}×${formatNum(partes[0])} $unidadLeg"
    val media = partes.average()
    val min = partes.min()
    val max = partes.max()
    return if (min == max) "${formatNum(media)} $unidadLeg"
    else "media ${formatNum(media)} ${unidadLeg} (${formatNum(min)}–${formatNum(max)})"
}

private fun formatNum(d: Double): String =
    if (d == d.toLong().toDouble()) d.toLong().toString()
    else "%.1f".format(d)
