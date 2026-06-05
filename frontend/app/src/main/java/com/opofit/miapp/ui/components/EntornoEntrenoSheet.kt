package com.opofit.miapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opofit.miapp.data.responsemodels.EntornoEntrenoOpcion

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EntornoEntrenoSheet(
    visible: Boolean,
    opciones: List<EntornoEntrenoOpcion>,
    seleccionado: String?,
    onDismiss: () -> Unit,
    onConfirmar: (String) -> Unit
) {
    if (!visible) return
    var picked by remember(seleccionado, visible) { mutableStateOf(seleccionado) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "¿Dónde entrenas?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Adaptamos ejercicios y material a tu entorno. Puedes cambiarlo cuando quieras.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                opciones.forEach { op ->
                    FilterChip(
                        selected = picked == op.id,
                        onClick = { picked = op.id },
                        label = { Text("${op.emoji ?: ""} ${op.etiqueta}") }
                    )
                }
            }
            picked?.let { id ->
                opciones.find { it.id == id }?.descripcion?.let { desc ->
                    Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
            Button(
                onClick = { picked?.let(onConfirmar) },
                enabled = picked != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar y adaptar mi plan")
            }
        }
    }
}
