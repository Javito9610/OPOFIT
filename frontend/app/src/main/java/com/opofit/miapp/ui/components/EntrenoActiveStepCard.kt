package com.opofit.miapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun EntrenoActiveStepCard(
    paso: Int,
    total: Int,
    nombre: String,
    objetivoSegundos: Int?,
    tipoCardio: String?,
    unidad: String?,
    valor: String,
    distancia: String,
    segundosCronometro: Int,
    onValorChange: (String) -> Unit,
    onDistanciaChange: (String) -> Unit,
    onUsarCronometro: () -> Unit,
    onCompletar: () -> Unit,
    onGps: (() -> Unit)?,
    errorValor: String? = null,
    ritmoTexto: String = "-",
    velocidadTexto: String = "-",
    labelDistancia: String = "Distancia",
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Paso $paso de $total",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            Text(
                nombre,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (objetivoSegundos != null && tipoCardio != null) {
                Text(
                    "Objetivo: ${formatMmSs(objetivoSegundos)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                )
            }
            if (tipoCardio != null) {
                OutlinedButton(onClick = onUsarCronometro, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Timer, null, Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Usar tiempo del cronómetro (${formatMmSs(segundosCronometro)})")
                }
                androidx.compose.material3.OutlinedTextField(
                    value = distancia,
                    onValueChange = onDistanciaChange,
                    label = { Text(labelDistancia) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Ritmo", style = MaterialTheme.typography.labelMedium)
                        Text(ritmoTexto, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Velocidad", style = MaterialTheme.typography.labelMedium)
                        Text(velocidadTexto, fontWeight = FontWeight.Bold)
                    }
                }
                onGps?.let { gps ->
                    OutlinedButton(onClick = gps, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Explore, null, Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Ruta e iniciar carrera")
                    }
                }
            } else {
                if (unidad == "min" && segundosCronometro > 0) {
                    FilledTonalButton(onClick = onUsarCronometro, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Timer, null, Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Aplicar ${formatMmSs(segundosCronometro)} del cronómetro")
                    }
                }
                ExerciseValueInput(
                    value = valor,
                    onValueChange = onValorChange,
                    unidad = unidad,
                    error = errorValor
                )
            }
            Button(onClick = onCompletar, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.CheckCircle, null, Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("Completar ejercicio")
            }
        }
    }
}

private fun formatMmSs(total: Int): String = "%02d:%02d".format(total / 60, total % 60)
