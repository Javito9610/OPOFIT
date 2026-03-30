package com.opofit.miapp.ui.screens.perfil

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opofit.miapp.data.responsemodels.MarcaActualizar
import com.opofit.miapp.ui.viewmodels.AuthViewModel
import com.opofit.miapp.ui.viewmodels.PerfilViewModel
import kotlin.math.pow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarPerfilScreen(
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    perfilViewModel: PerfilViewModel = viewModel()
) {
    val authState by authViewModel.uiState.collectAsState()
    val perfilState by perfilViewModel.uiState.collectAsState()

    val userId = authState.userId ?: 0
    val oposicionId = authState.oposicionId ?: 1
    val genero = authState.genero ?: "HOMBRE"

    var peso by remember { mutableStateOf("") }
    var altura by remember { mutableStateOf("") }

    data class MarcaRow(val idPrueba: Int? = null, val nombrePrueba: String = "", val valor: String = "")
    val marcas = remember { mutableStateListOf(MarcaRow()) }

    var expandedDropdownIndex by remember { mutableStateOf(-1) }

    val pruebasDisponibles = remember(perfilState.infoPruebas) {
        perfilState.infoPruebas
            .distinctBy { it.id_pruebas_oficiales }
            .map { it.id_pruebas_oficiales to it.nombre_prueba }
    }

    val imc = remember(peso, altura) {
        val p = peso.toDoubleOrNull()
        val a = altura.toDoubleOrNull()
        if (p != null && a != null && a > 0) {
            val alturaM = a / 100.0
            "%.2f".format(p / alturaM.pow(2))
        } else "-"
    }

    LaunchedEffect(oposicionId, genero) {
        perfilViewModel.cargarInfoPruebas(oposicionId, genero)
    }

    LaunchedEffect(perfilState.guardadoExitoso) {
        if (perfilState.guardadoExitoso) {
            perfilViewModel.resetGuardado()
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar Perfil") },
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
                    text = "Datos físicos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                OutlinedTextField(
                    value = peso,
                    onValueChange = { peso = it },
                    label = { Text("Peso (kg)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = altura,
                    onValueChange = { altura = it },
                    label = { Text("Altura (cm)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("IMC calculado:", style = MaterialTheme.typography.bodyMedium)
                        Text(imc, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Actualizar Marcas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            itemsIndexed(marcas) { index, row ->
                val expanded = expandedDropdownIndex == index

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expandedDropdownIndex = if (it) index else -1 }
                    ) {
                        OutlinedTextField(
                            value = row.nombrePrueba,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Prueba") },
                            placeholder = { Text("Selecciona una prueba") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expandedDropdownIndex = -1 }
                        ) {
                            pruebasDisponibles.forEach { (id, nombre) ->
                                DropdownMenuItem(
                                    text = { Text(nombre) },
                                    onClick = {
                                        marcas[index] = row.copy(idPrueba = id, nombrePrueba = nombre)
                                        expandedDropdownIndex = -1
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = row.valor,
                        onValueChange = { marcas[index] = row.copy(valor = it) },
                        label = { Text("Valor") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
            }

            item {
                Button(
                    onClick = { marcas.add(MarcaRow()) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("+ Añadir marca")
                }
            }

            if (perfilState.nuevoNivel != null || perfilState.nuevaNota != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("¡Perfil actualizado!", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            perfilState.nuevoNivel?.let { Text("Nuevo nivel: $it") }
                            perfilState.nuevaNota?.let { Text("Nueva nota: $it") }
                        }
                    }
                }
            }

            if (perfilState.error.isNotEmpty()) {
                item {
                    Text(
                        text = perfilState.error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                if (perfilState.isLoading) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Button(
                        onClick = {
                            val p = peso.toDoubleOrNull() ?: 0.0
                            val a = altura.toDoubleOrNull() ?: 0.0
                            val nuevasMarcas = marcas.mapNotNull { row ->
                                val id = row.idPrueba ?: return@mapNotNull null
                                val v = row.valor.toDoubleOrNull() ?: return@mapNotNull null
                                MarcaActualizar(id, v)
                            }
                            perfilViewModel.actualizarPerfil(userId, p, a, oposicionId, nuevasMarcas)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Guardar Cambios")
                    }
                }
            }
        }
    }
}
