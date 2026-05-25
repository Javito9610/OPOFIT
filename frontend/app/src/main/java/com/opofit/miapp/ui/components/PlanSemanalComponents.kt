package com.opofit.miapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opofit.miapp.data.responsemodels.DiaPlan

fun enfoqueEmoji(enfoque: String): String = when (enfoque.uppercase()) {
    "FUERZA" -> "💪"
    "RESISTENCIA" -> "🏃"
    "VELOCIDAD" -> "⚡"
    else -> "📋"
}

fun enfoqueLabel(enfoque: String): String = when (enfoque.uppercase()) {
    "FUERZA" -> "Fuerza"
    "RESISTENCIA" -> "Resistencia"
    "VELOCIDAD" -> "Velocidad"
    else -> enfoque
}

@Composable
fun PlanDiaCard(
    dia: DiaPlan,
    modifier: Modifier = Modifier,
    onEntrenar: (String) -> Unit,
    expanded: Boolean = false
) {
    val container = when {
        dia.es_hoy && !dia.completada -> MaterialTheme.colorScheme.primaryContainer
        dia.completada -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.surface
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = if (dia.es_hoy) 4.dp else 1.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${dia.nombre_dia} · ${enfoqueEmoji(dia.enfoque)} ${enfoqueLabel(dia.enfoque)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        dia.titulo,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (dia.completada) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Completada",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                } else if (dia.es_hoy) {
                    Text(
                        "HOY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (!dia.descripcion.isNullOrBlank()) {
                Text(
                    dia.descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "${dia.ejercicios.size} ejercicios en sesión",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            if (dia.es_hoy && !dia.completada) {
                Button(
                    onClick = { onEntrenar(dia.enfoque) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Empezar entreno de hoy")
                }
            } else if (!dia.completada && expanded) {
                OutlinedButton(
                    onClick = { onEntrenar(dia.enfoque) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Entrenar ${enfoqueLabel(dia.enfoque).lowercase()}")
                }
            }
        }
    }
}

@Composable
fun EntrenoHoyHeroCard(
    titulo: String,
    subtitulo: String,
    enfoque: String,
    onEmpezar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                titulo,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
            )
            Text(
                subtitulo,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                "${enfoqueEmoji(enfoque)} ${enfoqueLabel(enfoque)}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
            )
            Button(
                onClick = onEmpezar,
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimary,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Empezar ahora", fontWeight = FontWeight.Bold)
            }
        }
    }
}
