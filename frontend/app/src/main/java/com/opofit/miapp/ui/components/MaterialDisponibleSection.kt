package com.opofit.miapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opofit.miapp.data.responsemodels.MaterialDisponibleItem

/**
 * Selector multi-chip de material disponible.
 *
 * El usuario marca todo lo que tiene a mano (KB, mancuernas, TRX...) y la IA
 * y los filtros de ejercicios libres respetarán esa lista.
 *
 * Reglas especiales gestionadas en el ViewModel (no aquí):
 *   - Marcar "Solo peso corporal" desmarca el resto.
 *   - Marcar "Gimnasio completo" desmarca el resto y equivale a "todo".
 */
@Composable
fun MaterialDisponibleSection(
    catalogo: List<MaterialDisponibleItem>,
    seleccionado: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (catalogo.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Material disponible",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            InfoTip(
                title = "¿Para qué sirve?",
                text = "Marca todo lo que tienes a mano. La IA que genera tu plan y los entrenos libres NO te propondrán ejercicios que requieran material que no tengas. Si entrenas en sitios distintos, marca lo que coincide en ambos."
            )
        }
        if (seleccionado.isNotEmpty()) {
            Text(
                "Seleccionado: ${seleccionado.size}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(catalogo, key = { it.id }) { item ->
                val activo = item.id in seleccionado
                FilterChip(
                    selected = activo,
                    onClick = { onToggle(item.id) },
                    label = {
                        val prefijo = item.icono?.let { "$it " } ?: ""
                        Text("$prefijo${item.label}")
                    },
                    leadingIcon = if (activo) {
                        {
                            Icon(
                                Icons.Filled.CheckCircle,
                                null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                )
            }
        }
    }
}
