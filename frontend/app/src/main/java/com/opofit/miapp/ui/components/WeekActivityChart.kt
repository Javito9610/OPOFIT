package com.opofit.miapp.ui.components

import com.opofit.miapp.ui.components.ElevatedCard
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import com.opofit.miapp.ui.theme.AccentOrange
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
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Actividad últimos 7 días",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            val sinActividad = dias.isEmpty() || dias.all { it.sesiones == 0 }
            if (sinActividad) {
                Text(
                    "Sin entrenos esta semana — empieza hoy",
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
                    color = AccentOrange,
                    valueLabel = { v -> if (v == 0.0) "" else v.toInt().toString() }
                )
            }
        }
    }
}
