package com.opofit.miapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opofit.miapp.utils.TimeFormatUtil

/**
 * Input estructurado para ejercicios con modalidad CrossFit (WOD, AMRAP, EMOM,
 * For Time, Tabata, Death by) y levantamientos olímpicos.
 *
 * El score se serializa como un string con la convención del backend para que
 * el historial pueda graficar PR por tipo:
 *
 *   - AMRAP   →  "5+12"   (5 rondas completas + 12 reps)
 *   - EMOM    →  "18/20"  (18 rondas completadas de 20 prescritas)
 *   - FOR_TIME→  "11:35"  (mm:ss del cronómetro al acabar)
 *   - TABATA  →  "82"     (suma total de reps de las 8 rondas)
 *   - DEATH_BY→  "14"     (última ronda completada)
 *   - LIFT    →  "120"    (peso máximo en kg de la sesión)
 *
 * Si la modalidad no es WOD se usa el SeriesInput / ExerciseValueInput
 * tradicional (ver `EntrenoActiveStepCard`).
 */
@Composable
fun WodInput(
    modalidad: String,
    scoreTipo: String?,
    timeCapSeg: Int?,
    valor: String,
    onValorChange: (String) -> Unit,
    elapsedMsCronometro: Long,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ModalidadChip(modalidad)
            timeCapSeg?.takeIf { it > 0 }?.let {
                AssistChip(
                    onClick = {},
                    label = { Text("Cap ${TimeFormatUtil.formatElapsedMs(it * 1000L, showMs = false)}") }
                )
            }
        }

        when (modalidad.lowercase()) {
            "amrap" -> AmrapBody(valor, onValorChange)
            "emom" -> EmomBody(valor, onValorChange)
            "for_time", "chipper", "ladder" -> ForTimeBody(valor, onValorChange, elapsedMsCronometro)
            "tabata" -> TabataBody(valor, onValorChange)
            "death_by" -> DeathByBody(valor, onValorChange)
            "crossfit_lift" -> LiftBody(valor, onValorChange, scoreTipo)
            "wod" -> WodGenericBody(valor, onValorChange, scoreTipo, elapsedMsCronometro)
            else -> AmrapBody(valor, onValorChange)
        }
    }
}

@Composable
private fun ModalidadChip(modalidad: String) {
    val (label, color) = when (modalidad.lowercase()) {
        "amrap" -> "AMRAP" to MaterialTheme.colorScheme.tertiaryContainer
        "emom" -> "EMOM" to MaterialTheme.colorScheme.tertiaryContainer
        "for_time" -> "For Time" to MaterialTheme.colorScheme.tertiaryContainer
        "tabata" -> "Tabata" to MaterialTheme.colorScheme.tertiaryContainer
        "death_by" -> "Death by" to MaterialTheme.colorScheme.errorContainer
        "wod" -> "WOD" to MaterialTheme.colorScheme.tertiaryContainer
        "crossfit_lift" -> "Levantamiento" to MaterialTheme.colorScheme.secondaryContainer
        "chipper" -> "Chipper" to MaterialTheme.colorScheme.tertiaryContainer
        "ladder" -> "Ladder" to MaterialTheme.colorScheme.tertiaryContainer
        else -> modalidad.uppercase() to MaterialTheme.colorScheme.surfaceVariant
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * AMRAP: rondas completas + reps en la última (parcial).
 * Score se serializa como "rondas+reps", ej "5+12".
 */
@Composable
private fun AmrapBody(valor: String, onValorChange: (String) -> Unit) {
    val partes = valor.split("+", limit = 2)
    val rondas = partes.getOrNull(0)?.toIntOrNull() ?: 0
    val repsExtra = partes.getOrNull(1)?.toIntOrNull() ?: 0
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Counter(
            label = "Rondas",
            value = rondas,
            onChange = { onValorChange("$it+$repsExtra") },
            modifier = Modifier.weight(1f)
        )
        Counter(
            label = "Reps extra",
            value = repsExtra,
            onChange = { onValorChange("$rondas+$it") },
            modifier = Modifier.weight(1f),
            max = 999
        )
    }
    Text(
        "Score: $rondas+$repsExtra rondas",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** EMOM: cuántos minutos completaste del total. */
@Composable
private fun EmomBody(valor: String, onValorChange: (String) -> Unit) {
    val partes = valor.split("/", limit = 2)
    val completados = partes.getOrNull(0)?.toIntOrNull() ?: 0
    val total = partes.getOrNull(1)?.toIntOrNull()?.takeIf { it > 0 } ?: 10
    Counter(
        label = "Minutos completados (de $total)",
        value = completados,
        onChange = { onValorChange("$it/$total") },
        max = total
    )
}

/** For Time: cronómetro final mm:ss — si no hay valor se autocompleta con el tracker. */
@Composable
private fun ForTimeBody(
    valor: String,
    onValorChange: (String) -> Unit,
    elapsedMsCronometro: Long
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onValorChange,
        label = { Text("Tiempo final (mm:ss)") },
        placeholder = { Text("11:35") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
    )
    if (elapsedMsCronometro > 0 && valor.isBlank()) {
        Text(
            "Cronómetro: ${TimeFormatUtil.formatElapsedMs(elapsedMsCronometro, showMs = false)} — toca para aplicar",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    RoundedCornerShape(6.dp)
                )
                .padding(8.dp)
        )
    }
}

/** Tabata: suma total de reps de las 8 rondas (20s on/10s off). */
@Composable
private fun TabataBody(valor: String, onValorChange: (String) -> Unit) {
    OutlinedTextField(
        value = valor,
        onValueChange = { onValorChange(it.filter { c -> c.isDigit() }) },
        label = { Text("Reps totales (suma de 8 rondas)") },
        placeholder = { Text("82") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

/** Death by: última ronda en la que completaste las reps prescritas. */
@Composable
private fun DeathByBody(valor: String, onValorChange: (String) -> Unit) {
    val ronda = valor.toIntOrNull() ?: 0
    Counter(
        label = "Última ronda completada",
        value = ronda,
        onChange = { onValorChange(it.toString()) },
        max = 99
    )
    Text(
        "En Death by no aguantas: anota la ÚLTIMA ronda en la que cerraste las reps en el minuto.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Levantamiento (snatch, clean, jerk, deadlift…): peso máximo de la sesión. */
@Composable
private fun LiftBody(valor: String, onValorChange: (String) -> Unit, scoreTipo: String?) {
    val unidad = if (scoreTipo == "peso") "kg" else "reps"
    OutlinedTextField(
        value = valor,
        onValueChange = { s -> onValorChange(s.filter { it.isDigit() || it == '.' || it == ',' }) },
        label = { Text("Peso máximo ($unidad)") },
        placeholder = { Text("120") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}

/** WOD genérico: si tiene scoreTipo lo respeta, si no asume For Time. */
@Composable
private fun WodGenericBody(
    valor: String,
    onValorChange: (String) -> Unit,
    scoreTipo: String?,
    elapsedMsCronometro: Long
) {
    when (scoreTipo) {
        "rondas_reps" -> AmrapBody(valor, onValorChange)
        "rondas_completadas" -> EmomBody(valor, onValorChange)
        else -> ForTimeBody(valor, onValorChange, elapsedMsCronometro)
    }
}

/** Counter +/- compacto: número grande en el medio, botones a los lados. */
@Composable
private fun Counter(
    label: String,
    value: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 0,
    max: Int = 999
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledIconButton(
                onClick = { if (value > min) onChange(value - 1) },
                enabled = value > min,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Filled.Remove, contentDescription = "-")
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$value",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
            FilledIconButton(
                onClick = { if (value < max) onChange(value + 1) },
                enabled = value < max,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "+")
            }
        }
    }
}
