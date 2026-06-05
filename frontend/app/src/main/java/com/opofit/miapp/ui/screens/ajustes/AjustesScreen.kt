package com.opofit.miapp.ui.screens.ajustes

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opofit.miapp.data.api.RetrofitClient
import com.opofit.miapp.data.local.TokenManager
import com.opofit.miapp.data.responsemodels.CambiarPasswordRequest
import com.opofit.miapp.ui.components.OpoFitLogo
import com.opofit.miapp.ui.viewmodels.AjustesViewModel
import com.opofit.miapp.ui.viewmodels.AuthViewModel
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesScreen(
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToMisDispositivos: () -> Unit = {},
    ajustesViewModel: AjustesViewModel = viewModel()
) {
    val authState by authViewModel.uiState.collectAsState()
    val uiState by ajustesViewModel.uiState.collectAsState()

    val userId = authState.userId ?: 0

    var unidadPeso by remember { mutableStateOf("kg") }
    var unidadDistancia by remember { mutableStateOf("km") }
    var expandedPeso by remember { mutableStateOf(false) }
    var expandedDistancia by remember { mutableStateOf(false) }
    var horaRecordatorio by remember { mutableIntStateOf(18) }
    var recordatorioActivo by remember { mutableStateOf(true) }

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val scope = rememberCoroutineScope()
    var passActual by remember { mutableStateOf("") }
    var passNueva by remember { mutableStateOf("") }
    var passMsg by remember { mutableStateOf("") }
    var mostrarDialogoEliminar by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.unidadPeso, uiState.unidadDistancia) {
        unidadPeso = uiState.unidadPeso
        unidadDistancia = uiState.unidadDistancia
    }

    // Sincronizamos el estado local con el del backend para no sobrescribirlo al guardar.
    LaunchedEffect(uiState.horaRecordatorio, uiState.recordatorioActivo) {
        horaRecordatorio = uiState.horaRecordatorio
        recordatorioActivo = uiState.recordatorioActivo
    }

    LaunchedEffect(uiState.guardadoExitoso) {
        if (uiState.guardadoExitoso) {
            snackbarHostState.showSnackbar("Ajustes guardados correctamente")
            ajustesViewModel.resetGuardado()
        }
    }

    LaunchedEffect(uiState.cuentaEliminada) {
        if (uiState.cuentaEliminada) {
            onLogout()
            ajustesViewModel.clearCuentaEliminadaFlag()
        }
    }

    if (mostrarDialogoEliminar) {
        AlertDialog(
            onDismissRequest = {
                if (!uiState.eliminandoCuenta) mostrarDialogoEliminar = false
            },
            title = {
                Text(
                    "¿Eliminar cuenta?",
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Se eliminarán de forma permanente tu perfil, marcas, historial de entrenos y rutinas personales. No podrás recuperarlos.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (uiState.error.isNotEmpty()) {
                        Text(
                            text = uiState.error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { ajustesViewModel.eliminarCuenta() },
                    enabled = !uiState.eliminandoCuenta
                ) {
                    if (uiState.eliminandoCuenta) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "Eliminar definitivamente",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        mostrarDialogoEliminar = false
                        ajustesViewModel.clearMensajeError()
                    },
                    enabled = !uiState.eliminandoCuenta
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        // Sin verticalScroll los ultimos cards (eliminar cuenta, cerrar sesion, info)
        // quedaban fuera de la pantalla y eran inalcanzables en moviles pequenos.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Unidades de medida",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Modo oscuro",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Cambia el tema de la aplicación",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.darkMode,
                        onCheckedChange = { enabled -> ajustesViewModel.setDarkMode(enabled) }
                    )
                }
            }

            ExposedDropdownMenuBox(
                expanded = expandedPeso,
                onExpandedChange = { expandedPeso = it }
            ) {
                OutlinedTextField(
                    value = unidadPeso,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Unidad de Peso") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPeso) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expandedPeso,
                    onDismissRequest = { expandedPeso = false }
                ) {
                    listOf("kg", "lb").forEach { opcion ->
                        DropdownMenuItem(
                            text = { Text(opcion) },
                            onClick = {
                                unidadPeso = opcion
                                expandedPeso = false
                            }
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = expandedDistancia,
                onExpandedChange = { expandedDistancia = it }
            ) {
                OutlinedTextField(
                    value = unidadDistancia,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Unidad de Distancia") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDistancia) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expandedDistancia,
                    onDismissRequest = { expandedDistancia = false }
                ) {
                    listOf("km", "mi").forEach { opcion ->
                        DropdownMenuItem(
                            text = { Text(opcion) },
                            onClick = {
                                unidadDistancia = opcion
                                expandedDistancia = false
                            }
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Recordatorio de entreno", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Te avisamos a la hora que elijas si hoy tienes sesión pendiente en tu plan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Activar recordatorio")
                        Switch(
                            checked = recordatorioActivo,
                            onCheckedChange = { recordatorioActivo = it }
                        )
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (7..21).forEach { h ->
                            FilterChip(
                                selected = horaRecordatorio == h,
                                onClick = { horaRecordatorio = h },
                                label = { Text("${h}:00") }
                            )
                        }
                    }
                }
            }

            if (uiState.error.isNotEmpty()) {
                Text(
                    text = uiState.error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Button(
                    onClick = {
                        ajustesViewModel.guardarAjustes(
                            userId,
                            unidadPeso,
                            unidadDistancia,
                            horaRecordatorio,
                            recordatorioActivo
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Guardar Ajustes")
                }
            }

            Divider()

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Dispositivos y relojes",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Conecta tu reloj o pulsera (Garmin, Polar, Mi Band, Amazfit...) para que tus actividades se sincronicen automáticamente con OpoFit.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onNavigateToMisDispositivos,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Gestionar dispositivos")
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Cambiar contraseña", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = passActual,
                        onValueChange = { passActual = it },
                        label = { Text("Contraseña actual") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = passNueva,
                        onValueChange = { passNueva = it },
                        label = { Text("Nueva contraseña") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (passMsg.isNotBlank()) {
                        Text(passMsg, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    val token = tokenManager.getToken().first().orEmpty()
                                    val r = RetrofitClient.retrofit.create(com.opofit.miapp.data.api.AuthApi::class.java)
                                        .cambiarPassword("Bearer $token", CambiarPasswordRequest(passActual, passNueva))
                                    passMsg = r.msg ?: if (r.ok) "Contraseña actualizada" else "Error"
                                    if (r.ok) { passActual = ""; passNueva = "" }
                                } catch (e: Exception) {
                                    passMsg = e.message ?: "Error al cambiar contraseña"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = passActual.length >= 4 && passNueva.length >= 6
                    ) { Text("Actualizar contraseña") }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Cuenta",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Eliminar tu cuenta borra todos los datos asociados en OpoFit.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    OutlinedButton(
                        onClick = {
                            ajustesViewModel.clearMensajeError()
                            mostrarDialogoEliminar = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Eliminar cuenta")
                    }
                    OutlinedButton(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cerrar Sesión")
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OpoFitLogo(size = 52.dp)
                        Column {
                            Text(
                                text = "OpoFit v1.0.0",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "App de fitness para opositores",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
