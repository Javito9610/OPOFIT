package com.opofit.miapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opofit.miapp.data.responsemodels.DiaActividad

@Composable
fun WeekActivityChart(
    dias: List<DiaActividad>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Actividad ultimos 7 dias",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            // El backend rellena siempre 7 dias con 0 si no hubo entreno;
            // por eso no basta con isEmpty(): hay que ver si TODOS los dias tienen 0.
            val sinActividad = dias.isEmpty() || dias.all { it.sesiones == 0 }
            if (sinActividad) {
                Text(
                    "Sin entrenos esta semana - empieza hoy!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                ColumnsChart(
                    values = dias.map { it.sesiones.toDouble() },
                    labels = dias.map { it.etiqueta ?: "" },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .padding(top = 4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    valueLabel = { v -> if (v == 0.0) "" else v.toInt().toString() }
                )
            }
        }
    }
}
