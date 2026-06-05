package com.opofit.miapp.ui.screens.perfil

import com.opofit.miapp.ui.components.ElevatedCard
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opofit.miapp.data.local.TokenManager
import com.opofit.miapp.data.responsemodels.MarcaActualizar
import com.opofit.miapp.ui.components.ProfileAvatar
import com.opofit.miapp.ui.viewmodels.AuthViewModel
import com.opofit.miapp.ui.viewmodels.PerfilViewModel
import com.opofit.miapp.utils.FitnessMode
import com.opofit.miapp.utils.ImagePickerUtil
import com.opofit.miapp.utils.Units
import kotlinx.coroutines.flow.collectLatest
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
    val esFitness = FitnessMode.isFitness(authState.modoUso)
    val oposicionId = FitnessMode.planOposicionId(authState.oposicionId, authState.modoUso)
    val genero = authState.genero ?: "HOMBRE"

    var nombre by remember { mutableStateOf(authState.userName.orEmpty()) }
    var avatarUrl by remember { mutableStateOf(authState.avatarUrl.orEmpty()) }
    var subiendoAvatar by remember { mutableStateOf(false) }
    var peso by remember { mutableStateOf("") }
    var altura by remember { mutableStateOf("") }

    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    var unitPeso by remember { mutableStateOf("kg") }
    var unitDist by remember { mutableStateOf("km") }
    LaunchedEffect(Unit) {
        tokenManager.getUnitPeso().collectLatest { u ->
            if (!u.isNullOrBlank()) unitPeso = u
        }
    }
    LaunchedEffect(Unit) {
        tokenManager.getUnitDistancia().collectLatest { u ->
            if (!u.isNullOrBlank()) unitDist = u
        }
    }

    data class MarcaRow(val idPrueba: Int? = null, val nombrePrueba: String = "", val valor: String = "")
    val marcas = remember { mutableStateListOf(MarcaRow()) }

    var expandedDropdownIndex by remember { mutableStateOf(-1) }

    val pruebasDisponibles = remember(perfilState.infoPruebas, unitDist) {
        perfilState.infoPruebas
            .distinctBy { it.id_pruebas_oficiales }
            .map {
                val u = it.unidad ?: if ((it.mejor_si_es_menor ?: 0) == 1) "s" else "reps"
                Triple(it.id_pruebas_oficiales, Units.nombreConEquivalenciaDistancia(it.nombre_prueba, unitDist), u)
            }
    }

    val imc = remember(peso, altura, unitPeso) {
        val pRaw = peso.toDoubleOrNull()
        val a = altura.toDoubleOrNull()
        val pKg = pRaw?.let { if (unitPeso == "lb") Units.lbToKg(it) else it }
        if (pKg != null && a != null && a > 0) {
            val alturaM = a / 100.0
            "%.2f".format(pKg / alturaM.pow(2))
        } else "-"
    }

    LaunchedEffect(oposicionId, genero, esFitness) {
        if (!esFitness) perfilViewModel.cargarInfoPruebas(oposicionId, genero)
    }

    LaunchedEffect(authState.peso, unitPeso) {
        if (peso.isBlank() && authState.peso != null) {
            val shown = if (unitPeso == "lb") Units.kgToLb(authState.peso!!) else authState.peso!!
            peso = String.format("%.1f", shown)
        }
    }

    LaunchedEffect(authState.altura) {
        if (altura.isBlank() && authState.altura != null) {
            altura = String.format("%.0f", authState.altura!!)
        }
    }

    LaunchedEffect(authState.avatarUrl) {
        authState.avatarUrl?.let { if (avatarUrl.isBlank()) avatarUrl = it }
    }

    val pickFoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val b64 = ImagePickerUtil.uriToJpegBase64(context, uri) ?: run {
            perfilViewModel.resetGuardado()
            return@rememberLauncherForActivityResult
        }
        subiendoAvatar = true
        perfilViewModel.subirAvatar(
            b64,
            onOk = { url ->
                avatarUrl = url
                authViewModel.refreshSessionFromBackend()
            },
            onFinished = { subiendoAvatar = false }
        )
    }

    LaunchedEffect(perfilState.guardadoExitoso) {
        if (perfilState.guardadoExitoso) {
            authViewModel.refreshSessionFromBackend()
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
                Text("Perfil público", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            item {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProfileAvatar(
                        name = nombre.ifBlank { "Usuario" },
                        sizeDp = 72,
                        avatarUrl = avatarUrl.ifBlank { null }
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                pickFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            enabled = !subiendoAvatar && !perfilState.isLoading
                        ) {
                            Text("Elegir de galería")
                        }
                        if (subiendoAvatar) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Text(
                                "Toca para cambiar tu foto de perfil",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
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
                    label = { Text("Peso ($unitPeso)") },
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
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
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

            if (!esFitness) {
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
                val selectedPruebaMeta = pruebasDisponibles.firstOrNull { it.first == row.idPrueba }
                val esTiempo = selectedPruebaMeta?.third == "s"
                val unidadLabel = if (row.idPrueba != null) {
                    if (esTiempo) "Tiempo (segundos)" else "Repeticiones (nº)"
                } else null

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
                            pruebasDisponibles.forEach { (id, nombre, _) ->
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
                        label = { Text(unidadLabel ?: "Valor") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        supportingText = {
                            if (unidadLabel != null) {
                                Text(if (esTiempo) "Introduce tu marca en segundos" else "Introduce tu marca en repeticiones")
                            }
                        }
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
            }

            if (!esFitness && (perfilState.nuevoNivel != null || perfilState.nuevaNota != null)) {
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth()
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
                            val pRaw = peso.toDoubleOrNull() ?: 0.0
                            val p = if (unitPeso == "lb") Units.lbToKg(pRaw) else pRaw
                            val a = altura.toDoubleOrNull() ?: 0.0
                            val nuevasMarcas = marcas.mapNotNull { row ->
                                val id = row.idPrueba ?: return@mapNotNull null
                                val v = row.valor.toDoubleOrNull() ?: return@mapNotNull null
                                MarcaActualizar(id, v)
                            }
                            perfilViewModel.actualizarPerfil(
                                userId, p, a, oposicionId, nuevasMarcas,
                                nombre = nombre.trim().ifBlank { null }
                            )
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
