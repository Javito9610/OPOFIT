package com.opofit.miapp.ui.screens.rutinas

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.first
import com.opofit.miapp.data.responsemodels.EjercicioLibreItem
import com.opofit.miapp.data.api.RetrofitClient
import com.opofit.miapp.data.local.TokenManager
import com.opofit.miapp.data.responsemodels.Ejercicio
import com.opofit.miapp.ui.viewmodels.AuthViewModel
import com.opofit.miapp.ui.viewmodels.RutinasLibresViewModel

private data class EjercicioFormRow(
    val idEjercicio: Int? = null,
    val nombreEjercicio: String = "",
    val series: String = "",
    val repeticiones: String = ""
)

private fun esCardioContinuo(nombre: String): Boolean {
    val n = nombre.lowercase()
    val esCardio = n.contains("carrera") || n.contains("trote") || n.contains("rodaje") || n.contains("fartlek") ||
        n.contains("natación") || n.contains("natacion") || n.contains("nadar")
    if (!esCardio) return false
    val tieneMin = Regex("(\\d+)\\s*min\\b", RegexOption.IGNORE_CASE).containsMatchIn(nombre)
    val tieneContinuo = n.contains("continu")
    val mencionaSeries = n.contains("series") || n.contains("x ")
    return (tieneMin || tieneContinuo) && !mencionaSeries
}

private fun tipoEntradaEjercicio(nombre: String): Pair<String, String> {
    val n = nombre.lowercase()
    return when {
        n.contains("carrera") || n.contains("rodaje") || n.contains("fartlek") -> "Duración" to "min"
        n.contains("series") && (n.contains("m") || n.contains("400") || n.contains("200")) -> "Distancia" to "m"
        n.contains("natación") || n.contains("natacion") -> "Distancia" to "m"
        n.contains("plancha") || n.contains("suspensión") || n.contains("suspension") -> "Tiempo" to "s"
        else -> "Repeticiones" to "reps"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearRutinaScreen(
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    rutinasLibresViewModel: RutinasLibresViewModel = viewModel()
) {
    val authState by authViewModel.uiState.collectAsState()
    val uiState by rutinasLibresViewModel.uiState.collectAsState()

    val userId = authState.userId ?: 0

    var nombreRutina by remember { mutableStateOf("") }
    val ejercicios = remember { mutableStateListOf(EjercicioFormRow()) }

    var ejerciciosDisponibles by remember { mutableStateOf<List<Ejercicio>>(emptyList()) }
    var errorCargaEjercicios by remember { mutableStateOf("") }
    var busquedaEjercicio by remember { mutableStateOf("") }
    var filtroPilar by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }

    LaunchedEffect(busquedaEjercicio, filtroPilar) {
        try {
            val token = tokenManager.getToken().first() ?: ""
            val response = RetrofitClient.ejerciciosApi.listarEjercicios(
                "Bearer $token",
                busqueda = busquedaEjercicio.ifBlank { null },
                pilar = filtroPilar
            )
            if (response.ok && response.data != null) {
                ejerciciosDisponibles = response.data
                errorCargaEjercicios = ""
            } else {
                errorCargaEjercicios = "No se pudieron cargar los ejercicios"
            }
        } catch (e: Exception) {
            errorCargaEjercicios = "Error al cargar ejercicios: ${e.message ?: "Error de conexión"}"
        }
    }

    LaunchedEffect(uiState.guardadoExitoso) {
        if (uiState.guardadoExitoso) {
            rutinasLibresViewModel.resetGuardado()
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva rutina libre") },
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
                OutlinedTextField(
                    value = nombreRutina,
                    onValueChange = { nombreRutina = it },
                    label = { Text("Nombre de la rutina") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                Text(
                    text = "Banco de ejercicios (${ejerciciosDisponibles.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                OutlinedTextField(
                    value = busquedaEjercicio,
                    onValueChange = { busquedaEjercicio = it },
                    label = { Text("Buscar ejercicio") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(null to "Todos", "FUERZA" to "Fuerza", "RESISTENCIA" to "Resist.", "VELOCIDAD" to "Veloc.")
                        .forEach { (pilar, label) ->
                            OutlinedButton(
                                onClick = { filtroPilar = pilar },
                                enabled = filtroPilar != pilar
                            ) { Text(label) }
                        }
                }
                if (errorCargaEjercicios.isNotEmpty()) {
                    Text(
                        text = errorCargaEjercicios,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            itemsIndexed(ejercicios) { index, row ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ejercicio ${index + 1}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (ejercicios.size > 1) {
                                IconButton(onClick = { ejercicios.removeAt(index) }) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Eliminar",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = row.nombreEjercicio,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Ejercicio") },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                ejerciciosDisponibles.forEach { ej ->
                                    DropdownMenuItem(
                                        text = { Text(ej.nombre) },
                                        onClick = {
                                            ejercicios[index] = row.copy(idEjercicio = ej.id_ejercicio, nombreEjercicio = ej.nombre)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                        val (labelReps, unit) = tipoEntradaEjercicio(row.nombreEjercicio)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!esCardioContinuo(row.nombreEjercicio)) {
                                OutlinedTextField(
                                    value = row.series,
                                    onValueChange = { ejercicios[index] = row.copy(series = it) },
                                    label = { Text("Series") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                            }
                            OutlinedTextField(
                                value = row.repeticiones,
                                onValueChange = { ejercicios[index] = row.copy(repeticiones = it) },
                                label = { Text("$labelReps ($unit)") },
                                modifier = if (esCardioContinuo(row.nombreEjercicio)) Modifier.fillMaxWidth() else Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = { ejercicios.add(EjercicioFormRow()) },
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
                            val items = ejercicios.mapNotNull { row ->
                                val id = row.idEjercicio ?: return@mapNotNull null
                                val s = row.series.toIntOrNull() ?: 1
                                val r = row.repeticiones.toIntOrNull() ?: 1
                                EjercicioLibreItem(id, s, r)
                            }
                            rutinasLibresViewModel.crearRutina(userId, nombreRutina, items)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Guardar Rutina")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancelar")
                    }
                }
            }
        }
    }
}
