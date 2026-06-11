package com.opofit.miapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Mini stat del perfil (Peso / Altura / IMC).
 * Label en una sola línea; valor grande debajo.
 */
@Composable
fun PerfilMiniStatCard(
    icono: ImageVector,
    label: String,
    valor: String,
    unidad: String,
    modifier: Modifier = Modifier,
    valorColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icono,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = valor,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = valorColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (unidad.isNotBlank()) {
                    Text(
                        text = unidad,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp),
                        maxLines = 1
                    )
                }
            }
        }
    }
}
