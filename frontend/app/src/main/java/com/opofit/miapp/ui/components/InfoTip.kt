package com.opofit.miapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Botón "?" pequeño que abre un popup explicativo. Estilo Notion/Linear.
 *
 * Uso típico al lado de un título de sección o métrica:
 * ```
 * Row(verticalAlignment = Alignment.CenterVertically) {
 *     Text("Pilares débiles", style = MaterialTheme.typography.titleMedium)
 *     InfoTip(
 *         title = "¿Qué son los pilares débiles?",
 *         text = "Son las áreas donde tu nota media está por debajo de 5..."
 *     )
 * }
 * ```
 *
 * Idea: explicar conceptos complejos SIN llenar la pantalla de texto.
 * El usuario decide cuándo quiere la explicación.
 */
@Composable
fun InfoTip(
    title: String,
    text: String,
    modifier: Modifier = Modifier
) {
    var open by remember { mutableStateOf(false) }
    IconButton(
        onClick = { open = true },
        modifier = modifier.size(28.dp)
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.HelpOutline,
            contentDescription = "Más info: $title",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            modifier = Modifier.size(18.dp)
        )
    }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = { open = false }) { Text("Entendido") }
            },
            icon = {
                Icon(
                    Icons.AutoMirrored.Outlined.HelpOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(title, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(text, style = MaterialTheme.typography.bodyMedium)
            }
        )
    }
}

/**
 * Versión inline para títulos: muestra el texto y a su lado el ? alineado.
 * Reemplaza al patrón Row + Text + InfoTip que se repite mucho.
 */
@Composable
fun TitleWithTip(
    title: String,
    infoTitle: String,
    infoText: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = style,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(end = 4.dp)
        )
        InfoTip(title = infoTitle, text = infoText)
    }
}
