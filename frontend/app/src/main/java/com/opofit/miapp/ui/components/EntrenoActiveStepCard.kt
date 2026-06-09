package com.opofit.miapp.ui.components

import com.opofit.miapp.ui.components.ElevatedCard
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
import com.opofit.miapp.utils.TimeFormatUtil

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
    elapsedMsCronometro: Long,
    onValorChange: (String) -> Unit,
    onDistanciaChange: (String) -> Unit,
    onUsarCronometro: () -> Unit,
    onCompletar: () -> Unit,
    onGps: (() -> Unit)?,
    errorValor: String? = null,
    ritmoTexto: String = "-",
    velocidadTexto: String = "-",
    labelDistancia: String = "Distancia",
    onInfoClick: (() -> Unit)? = null,
    // --- Soporte serie-por-serie (estilo Strong / Hevy) ---
    seriesObjetivo: Int = 1,
    repsObjetivo: Int? = null,
    valoresPorSerie: List<String> = emptyList(),
    onValoresPorSerieChange: ((List<String>) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Paso $paso de $total",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    nombre,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                onInfoClick?.let { info ->
                    ExerciseInfoButton(onClick = info, size = 40.dp)
                }
            }
            if (objetivoSegundos != null && tipoCardio != null) {
                Text(
                    "Objetivo: ${formatMmSs(objetivoSegundos)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (tipoCardio != null) {
                OutlinedButton(
                    onClick = onUsarCronometro,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = elapsedMsCronometro > 0L
                ) {
                    Icon(Icons.Filled.Timer, null, Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    ButtonText(
                        if (valor.isNotBlank()) {
                            "Aplicado: $valor ${unidad ?: "min"}"
                        } else {
                            "Aplicar ${TimeFormatUtil.formatElapsedMs(elapsedMsCronometro)}"
                        }
                    )
                }
                if (elapsedMsCronometro <= 0L) {
                    Text(
                        "Inicia el cronómetro arriba para registrar el tiempo de este ejercicio.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    )
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
                        Text("Iniciar carrera con GPS")
                    }
                }
            } else {
                if (unidad == "min" && elapsedMsCronometro > 0) {
                    FilledTonalButton(onClick = onUsarCronometro, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Timer, null, Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        ButtonText("Aplicar ${TimeFormatUtil.formatElapsedMs(elapsedMsCronometro)}")
                    }
                }
                // Si la prescripción tiene >1 serie y tenemos callback de series,
                // mostramos el input estilo Strong/Hevy (una fila por serie).
                if (seriesObjetivo > 1 && onValoresPorSerieChange != null && unidad != null) {
                    SeriesInput(
                        seriesObjetivo = seriesObjetivo,
                        repsObjetivo = repsObjetivo,
                        unidad = unidad,
                        valoresPorSerie = valoresPorSerie,
                        onValoresChange = onValoresPorSerieChange
                    )
                    if (!errorValor.isNullOrBlank()) {
                        Text(
                            errorValor,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    ExerciseValueInput(
                        value = valor,
                        onValueChange = onValorChange,
                        unidad = unidad,
                        error = errorValor
                    )
                }
            }
            Button(onClick = onCompletar, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.CheckCircle, null, Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("Completar ejercicio")
            }
        }
    }
}

private fun formatMmSs(totalSec: Int): String =
    TimeFormatUtil.formatElapsedMs(totalSec * 1000L, showMs = false)
