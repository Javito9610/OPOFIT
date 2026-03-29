package com.opofit.miapp.ui.screens.entrenamientos

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opofit.miapp.data.responsemodels.EjercicioRealizado
import com.opofit.miapp.ui.viewmodels.AuthViewModel
import com.opofit.miapp.ui.viewmodels.HistorialViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrarEntrenamientoScreen(
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onRegistrado: () -> Unit,
    historialViewModel: HistorialViewModel = viewModel()
) {
    val authState by authViewModel.uiState.collectAsState()
    val uiState by historialViewModel.uiState.collectAsState()

    val userId = authState.userId ?: 0

    val fechaHoy = remember {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    }

    var duracion by remember { mutableStateOf("") }

    data class EjercicioRow(val idEjercicio: String = "", val valor: String = "")
    val ejercicios = remember { mutableStateListOf(EjercicioRow()) }

    LaunchedEffect(uiState.registradoExitoso) {
        if (uiState.registradoExitoso) {
            historialViewModel.resetRegistrado()
            onRegistrado()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrar Entrenamiento") },
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
                Text(
                    text = "Fecha: $fechaHoy",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                OutlinedTextField(
                    value = duracion,
                    onValueChange = { duracion = it },
                    label = { Text("Duración (minutos)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            item {
                Text(
                    text = "Ejercicios realizados",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            itemsIndexed(ejercicios) { index, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = row.idEjercicio,
                        onValueChange = { ejercicios[index] = row.copy(idEjercicio = it) },
                        label = { Text("ID Ejercicio") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = row.valor,
                        onValueChange = { ejercicios[index] = row.copy(valor = it) },
                        label = { Text("Valor") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
            }

            item {
                Button(
                    onClick = { ejercicios.add(EjercicioRow()) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("+ Añadir ejercicio")
                }
            }

            if (uiState.error.isNotEmpty()) {
                item {
                    Text(
                        text = uiState.error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Button(
                        onClick = {
                            val mins = duracion.toIntOrNull() ?: 0
                            val realizados = ejercicios.mapNotNull { row ->
                                val id = row.idEjercicio.toIntOrNull() ?: return@mapNotNull null
                                val v = row.valor.toDoubleOrNull() ?: 0.0
                                EjercicioRealizado(id, v)
                            }
                            historialViewModel.registrarEntrenamiento(
                                userId = userId,
                                tipoRutina = "PERS",
                                idRutina = 1,
                                duracion = mins,
                                ejercicios = realizados
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Registrar")
                    }
                }
            }
        }
    }
}
