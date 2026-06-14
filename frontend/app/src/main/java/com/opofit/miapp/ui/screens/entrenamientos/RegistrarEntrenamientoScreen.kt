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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.runtime.mutableIntStateOf
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
import com.opofit.miapp.ui.components.ExerciseValueInput
import com.opofit.miapp.utils.EntrenoValidation
import com.opofit.miapp.gps.service.ShareActivityContext
import com.opofit.miapp.gps.service.buildPendingShareFromEntreno
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
    onRegistrado: (offerShare: Boolean) -> Unit,
    historialViewModel: HistorialViewModel = viewModel()
) {
    val authState by authViewModel.uiState.collectAsState()
    val uiState by historialViewModel.uiState.collectAsState()

    val userId = authState.userId ?: 0

    val fechaHoy = remember {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    }

    var duracion by remember { mutableStateOf("") }
    var oposicionSeleccionada by remember { mutableStateOf("Guardia Civil") }
    var tipoRutinaSeleccionada by remember { mutableStateOf("OPO") } 
    var rutinaId by remember { mutableStateOf("") } 

    data class EjercicioRow(val idEjercicio: String = "", val valor: String = "", val nombre: String = "")
    val ejercicios = remember { mutableStateListOf(EjercicioRow()) }
    var erroresValor by remember { mutableStateOf(mapOf<Int, String>()) }

    val oposiciones = listOf(
        "Policía Nacional" to 1,
        "Guardia Civil" to 2
    )
    val tiposRutina = listOf(
        "Oposición (rutina oficial)" to "OPO",
        "Personalizada" to "PERS"
    )
    var expandedOpo by remember { mutableStateOf(false) }
    var expandedTipo by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.registradoExitoso) {
        if (uiState.registradoExitoso) {
            val offerShare = uiState.ultimoEntreno?.let { entreno ->
                ShareActivityContext.set(
                    buildPendingShareFromEntreno(
                        titulo = entreno.titulo,
                        idHistorial = entreno.idHistorial,
                        duracionMin = entreno.duracionMin,
                        ejerciciosCount = entreno.ejerciciosCount
                    )
                )
                true
            } ?: false
            historialViewModel.resetRegistrado()
            onRegistrado(offerShare)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrar Entrenamiento") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
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
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = expandedOpo,
                        onExpandedChange = { expandedOpo = it }
                    ) {
                        OutlinedTextField(
                            value = oposicionSeleccionada,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Oposición") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedOpo) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = expandedOpo,
                            onDismissRequest = { expandedOpo = false }
                        ) {
                            oposiciones.forEach { (label, _) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        oposicionSeleccionada = label
                                        expandedOpo = false
                                    }
                                )
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = expandedTipo,
                        onExpandedChange = { expandedTipo = it }
                    ) {
                        val tipoLabel = tiposRutina.firstOrNull { it.second == tipoRutinaSeleccionada }?.first ?: "Oposición (rutina oficial)"
                        OutlinedTextField(
                            value = tipoLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tipo de entrenamiento") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTipo) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = expandedTipo,
                            onDismissRequest = { expandedTipo = false }
                        ) {
                            tiposRutina.forEach { (label, value) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        tipoRutinaSeleccionada = value
                                        expandedTipo = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = rutinaId,
                        onValueChange = { rutinaId = it },
                        label = { Text(if (tipoRutinaSeleccionada == "OPO") "ID rutina oficial" else "ID rutina personalizada") },
                        placeholder = { Text("Ej: 1") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
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
                        label = { Text("ID del ejercicio") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        ExerciseValueInput(
                            value = row.valor,
                            onValueChange = { v ->
                                ejercicios[index] = row.copy(valor = v)
                                val err = EntrenoValidation.validarValor(v, EntrenoValidation.inferirUnidad(row.nombre, null))
                                erroresValor = if (err == null) erroresValor - index else erroresValor + (index to err)
                            },
                            unidad = EntrenoValidation.inferirUnidad(row.nombre, null),
                            error = erroresValor[index]
                        )
                    }
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
                            // El usuario escribe minutos pero el backend almacena
                            // segundos en historial_sesiones.duracion_oficial.
                            val segundos = (mins * 60).coerceAtLeast(60)
                            val realizados = ejercicios.mapNotNull { row ->
                                val id = row.idEjercicio.toIntOrNull() ?: return@mapNotNull null
                                val v = row.valor.toDoubleOrNull() ?: 0.0
                                EjercicioRealizado(id, v)
                            }
                            val tituloRutina = when (tipoRutinaSeleccionada) {
                                "PERS" -> "Rutina personalizada"
                                else -> "Entrenamiento $oposicionSeleccionada"
                            }
                            historialViewModel.registrarEntrenamiento(
                                userId = userId,
                                tipoRutina = tipoRutinaSeleccionada,
                                idRutina = rutinaId.toIntOrNull() ?: 1,
                                duracion = segundos,
                                ejercicios = realizados,
                                tituloRutina = tituloRutina
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
