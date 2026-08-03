package com.opofit.miapp.ui.screens.rutinas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opofit.miapp.data.responsemodels.Ejercicio
import com.opofit.miapp.ui.components.ElevatedCard
import com.opofit.miapp.ui.components.ExerciseInfoButton
import com.opofit.miapp.ui.components.inferGrupoMuscular

private val ORDEN_GRUPOS = listOf(
    "Pecho", "Espalda", "Pierna", "Brazos", "Hombros", "Core", "Cardio", "General"
)

@Composable
fun BancoEjerciciosPorGrupo(
    ejercicios: List<Ejercicio>,
    idsYaAnadidos: Set<Int>,
    onSeleccionar: (Ejercicio) -> Unit,
    onVerDetalle: (Ejercicio) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val grupos = remember(ejercicios) {
        ejercicios
            .groupBy { inferGrupoMuscular(it.nombre, it.grupo_muscular, it.pilar) }
            .toSortedMap(compareBy { g -> ORDEN_GRUPOS.indexOf(g).let { if (it < 0) 99 else it } })
    }
    val expandidos = remember { mutableStateMapOf<String, Boolean>() }
    val busquedas = remember { mutableStateMapOf<String, String>() }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        grupos.forEach { (grupo, lista) ->
            val expandido = expandidos[grupo] ?: false
            val busqueda = busquedas[grupo].orEmpty()
            val filtrados = if (busqueda.isBlank()) lista
            else lista.filter {
                it.nombre.contains(busqueda, ignoreCase = true) ||
                    (it.equipamiento?.contains(busqueda, ignoreCase = true) == true)
            }
            val disponibles = filtrados.filter { it.id_ejercicio !in idsYaAnadidos }
            if (disponibles.isEmpty() && busqueda.isBlank()) return@forEach

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandidos[grupo] = !expandido },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$grupo (${disponibles.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            if (expandido) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (expandido) "Contraer" else "Expandir"
                        )
                    }
                    AnimatedVisibility(visible = expandido) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = busqueda,
                                onValueChange = { busquedas[grupo] = it },
                                label = { Text("Buscar en $grupo") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            if (disponibles.isEmpty()) {
                                Text(
                                    "Sin resultados en este grupo",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                disponibles.take(40).forEach { ej ->
                                    // Fila SIN clickable global: antes tocar cualquier
                                    // parte añadía el ejercicio y chocaba con el botón
                                    // de info. Ahora info (ⓘ) y añadir (+) son dos
                                    // botones claros, grandes y separados.
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { onVerDetalle(ej) }
                                        ) {
                                            Text(ej.nombre, style = MaterialTheme.typography.bodyMedium)
                                            ej.equipamiento?.takeIf { it.isNotBlank() }?.let { eq ->
                                                Text(
                                                    eq,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        ExerciseInfoButton(
                                            onClick = { onVerDetalle(ej) },
                                            size = 40.dp
                                        )
                                        FilledIconButton(
                                            onClick = { onSeleccionar(ej) },
                                            modifier = Modifier.size(44.dp),
                                            colors = IconButtonDefaults.filledIconButtonColors(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary
                                            )
                                        ) {
                                            Icon(
                                                Icons.Filled.Add,
                                                contentDescription = "Añadir ${ej.nombre}"
                                            )
                                        }
                                    }
                                }
                                if (disponibles.size > 40) {
                                    Text(
                                        "+${disponibles.size - 40} más — afina la búsqueda",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
