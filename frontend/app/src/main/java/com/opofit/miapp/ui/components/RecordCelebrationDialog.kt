package com.opofit.miapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opofit.miapp.data.responsemodels.RecordRotoItem

@Composable
fun RecordCelebrationDialog(
    records: List<RecordRotoItem>,
    onDismiss: () -> Unit
) {
    if (records.isEmpty()) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("🏆 ¡Nuevo récord personal!", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Has superado tu mejor marca en:",
                    style = MaterialTheme.typography.bodyMedium
                )
                records.forEach { r ->
                    val ant = r.valorAnterior?.let { String.format("%.2f", it) } ?: "—"
                    val nuevo = String.format("%.2f", r.valorNuevo)
                    Text(
                        "• ${r.nombreEjercicio}: $ant → $nuevo",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("¡Genial!")
            }
        }
    )
}
