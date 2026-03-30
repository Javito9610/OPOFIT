package com.opofit.miapp.ui.screens.historial

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opofit.miapp.ui.viewmodels.AuthViewModel
import com.opofit.miapp.ui.viewmodels.HistorialViewModel
import com.opofit.miapp.ui.viewmodels.RutinasViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    historialViewModel: HistorialViewModel = viewModel(),
    rutinasViewModel: RutinasViewModel = viewModel()
) {
    val authState by authViewModel.uiState.collectAsState()
    val uiState by historialViewModel.uiState.collectAsState()
    val rutinasState by rutinasViewModel.uiState.collectAsState()

    val userId = authState.userId ?: 0
    val oposicionId = authState.oposicionId ?: 1

    var ejercicioIdInput by remember { mutableStateOf("") }

    val ejerciciosDeRutina = remember(rutinasState.rutinaCompleta) {
        rutinasState.rutinaCompleta.flatMap { bloque ->
            bloque.ejercicios.mapNotNull { ej ->
                ej.id_ejercicio?.let { id -> Pair(id, ej.nombre) }
            }
        }.distinctBy { it.first }
    }

    LaunchedEffect(userId) {
        if (userId > 0) {
            rutinasViewModel.cargarRutina(userId, oposicionId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de Progreso") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Ver progreso de un ejercicio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (ejerciciosDeRutina.isNotEmpty()) {
                            Text(
                                text = "Ejercicios de tu rutina:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            ejerciciosDeRutina.forEach { (id, nombre) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$nombre (ID: $id)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Button(
                                        onClick = {
                                            ejercicioIdInput = id.toString()
                                            historialViewModel.cargarEvolucion(userId, id)
                                        }
                                    ) {
                                        Text("Ver")
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = ejercicioIdInput,
                                onValueChange = { ejercicioIdInput = it },
                                label = { Text("ID Ejercicio") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                            Button(
                                onClick = {
                                    val id = ejercicioIdInput.toIntOrNull()
                                    if (id != null && userId > 0) {
                                        historialViewModel.cargarEvolucion(userId, id)
                                    }
                                }
                            ) {
                                Text("Buscar")
                            }
                        }
                    }
                }
            }

            if (uiState.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }

            if (uiState.error.isNotEmpty()) {
                item {
                    Text(
                        text = uiState.error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            if (!uiState.isLoading && uiState.error.isEmpty() && uiState.evolucion.isEmpty() && ejercicioIdInput.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📊", style = MaterialTheme.typography.displayMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Selecciona un ejercicio para ver tu progreso.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (uiState.evolucion.isNotEmpty()) {
                val agrupado = uiState.evolucion.groupBy { punto ->
                    punto.fecha_entreno.take(10)
                }
                agrupado.entries.sortedByDescending { it.key }.forEach { (fecha, puntos) ->
                    item {
                        Text(
                            text = fecha,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(puntos) { punto ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = punto.fecha_entreno,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "Valor: ${punto.valor_conseguido}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                    item {
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}
