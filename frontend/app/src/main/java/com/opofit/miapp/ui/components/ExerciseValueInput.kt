package com.opofit.miapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Entrada de valor más profesional: stepper para reps, campo decimal para tiempo/distancia.
 */
@Composable
fun ExerciseValueInput(
    value: String,
    onValueChange: (String) -> Unit,
    unidad: String?,
    modifier: Modifier = Modifier,
    error: String? = null
) {
    val u = unidad?.lowercase() ?: "reps"
    val esReps = u in listOf("reps", "rep", "repeticiones")

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (esReps) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalIconButton(
                    onClick = {
                        val n = value.toIntOrNull() ?: 0
                        onValueChange((n - 1).coerceAtLeast(0).toString())
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "Menos")
                }
                OutlinedTextField(
                    value = value,
                    onValueChange = { v -> onValueChange(v.filter { it.isDigit() }) },
                    label = { Text("Repeticiones") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = error != null
                )
                FilledTonalIconButton(
                    onClick = {
                        val n = value.toIntOrNull() ?: 0
                        onValueChange((n + 1).toString())
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Más")
                }
            }
        } else {
            val label = when (u) {
                "min" -> "Minutos"
                "s", "seg" -> "Segundos"
                "km" -> "Kilómetros"
                "m" -> "Metros"
                else -> "Valor ($u)"
            }
            val placeholder = when (u) {
                "min" -> "Ej: 30"
                "km" -> "Ej: 5.2"
                "m" -> "Ej: 1500"
                else -> "Ej: 12.5"
            }
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(label) },
                placeholder = { Text(placeholder) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                isError = error != null
            )
        }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
        }
    }
}
