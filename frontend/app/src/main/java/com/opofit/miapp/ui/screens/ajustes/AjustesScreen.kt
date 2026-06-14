package com.opofit.miapp.ui.screens.ajustes

import com.opofit.miapp.ui.components.ElevatedCard
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import com.opofit.miapp.data.api.EntornoBody
import com.opofit.miapp.data.responsemodels.EntornoEntrenoOpcion
import com.opofit.miapp.data.responsemodels.TogglePerfilPublicoRequest
import com.opofit.miapp.ui.components.EntornoEntrenoSheet
import com.opofit.miapp.ui.components.OpoFitLogo
import com.opofit.miapp.ui.components.SectionHeader
import com.opofit.miapp.ui.viewmodels.AjustesViewModel
import com.opofit.miapp.ui.viewmodels.AuthViewModel
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    val oposicionId = authState.oposicionId ?: 1
    val esFitness = authState.modoUso?.equals("FITNESS", ignoreCase = true) == true

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
    var entornoSeleccionado by remember { mutableStateOf<String?>(null) }
    var entornosOpciones by remember { mutableStateOf<List<EntornoEntrenoOpcion>>(emptyList()) }
    var mostrarSheetEntorno by remember { mutableStateOf(false) }
    var perfilPublico by remember { mutableStateOf(false) }

    LaunchedEffect(userId, oposicionId, esFitness) {
        if (userId <= 0) return@LaunchedEffect
        try {
            val token = tokenManager.getToken().first().orEmpty()
            if (token.isBlank()) return@LaunchedEffect
            val entornos = RetrofitClient.planesApi.getEntornos("Bearer $token")
            entornosOpciones = entornos.data.orEmpty().filter { it.id != "MIXTO" && it.id != "PISTA" }
            val usuario = RetrofitClient.planesApi.getEntornoUsuario("Bearer $token")
            entornoSeleccionado = usuario.data?.entorno
            if (!esFitness) {
                val det = RetrofitClient.rankingApi.detalleUsuario("Bearer $token", oposicionId, userId)
                if (det.ok && det.data != null) {
                    perfilPublico = det.data.perfilPublico == true
                }
            }
        } catch (_: Exception) { }
    }

    EntornoEntrenoSheet(
        visible = mostrarSheetEntorno,
        opciones = entornosOpciones,
        seleccionado = entornoSeleccionado,
        onDismiss = { mostrarSheetEntorno = false },
        onConfirmar = { ent ->
            scope.launch {
                try {
                    val token = tokenManager.getToken().first().orEmpty()
                    val res = RetrofitClient.planesApi.putEntornoUsuario("Bearer $token", EntornoBody(ent))
                    if (res.ok) {
                        entornoSeleccionado = ent
                        snackbarHostState.showSnackbar("Entorno actualizado. Regenera el plan si quieres nuevos ejercicios.")
                    }
                } catch (_: Exception) { }
                mostrarSheetEntorno = false
            }
        }
    )

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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
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
            SectionHeader(title = "Apariencia", subtitle = "Tema visual")

            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
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

            SectionHeader(title = "Unidades", subtitle = "Peso y distancia")

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

            SectionHeader(title = "Entrenamiento", subtitle = "Plan y recordatorios")

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Entorno de entreno", fontWeight = FontWeight.SemiBold)
                        com.opofit.miapp.ui.components.InfoTip(
                            title = "¿Para qué sirve el entorno?",
                            text = "Le dices a la app dónde entrenas habitualmente (gym, casa, parque calistenia, " +
                                "pista, CrossFit). El plan se adapta para usar ejercicios coherentes con tu material. " +
                                "Por ejemplo, si dices «casa» no te propone press banca olímpico."
                        )
                    }
                    Text(
                        entornosOpciones.find { it.id == entornoSeleccionado }?.let { "${it.emoji.orEmpty()} ${it.etiqueta}" }
                            ?: "Sin configurar — el plan usará material genérico",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedButton(onClick = { mostrarSheetEntorno = true }, modifier = Modifier.fillMaxWidth()) {
                        com.opofit.miapp.ui.components.ButtonText(
                            "Cambiar entorno de entreno"
                        )
                    }
                }
            }

            // El selector de material vive ahora en el flujo de selección de
            // entorno (sheet de 2 pasos en Plan): solo se pregunta cuando el
            // entorno no tiene material implícito (CASA, PISTA, MIXTO). Aquí
            // en Ajustes no pintaba nada y daba la impresión de "filosofía
            // rara de un solo instrumento" según feedback del usuario.

            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Recordatorio de entreno", fontWeight = FontWeight.SemiBold)
                        com.opofit.miapp.ui.components.InfoTip(
                            title = "¿Cómo funciona el recordatorio?",
                            text = "Si lo activas, OpoFit te manda una notificación push a la hora que elijas, " +
                                "SOLO los días que tienes sesión pendiente. " +
                                "Si ya hiciste la sesión de hoy, no te molesta. " +
                                "Necesitas permitir notificaciones en el sistema."
                        )
                    }
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
                com.opofit.miapp.ui.components.PrimaryButton(
                    text = "Guardar Ajustes",
                    onClick = {
                        ajustesViewModel.guardarAjustes(
                            userId,
                            unidadPeso,
                            unidadDistancia,
                            horaRecordatorio,
                            recordatorioActivo
                        )
                    }
                )
            }

            if (!esFitness) {
                SectionHeader(title = "Privacidad", subtitle = "Ranking y visibilidad")
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Aparecer en el ranking", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Otros opositores de tu convocatoria verán tu nota media.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = perfilPublico,
                            onCheckedChange = { checked ->
                                perfilPublico = checked
                                scope.launch {
                                    try {
                                        val token = tokenManager.getToken().first().orEmpty()
                                        RetrofitClient.rankingApi.togglePerfilPublico(
                                            "Bearer $token",
                                            TogglePerfilPublicoRequest(checked)
                                        )
                                    } catch (_: Exception) {
                                        perfilPublico = !checked
                                    }
                                }
                            }
                        )
                    }
                }
            }

            SectionHeader(title = "Dispositivos", subtitle = "Reloj y sincronización")

            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Conexiones y reloj",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Conecta Garmin, Polar, Mi Band, Amazfit u otro reloj para traer tus actividades automáticamente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    com.opofit.miapp.ui.components.SecondaryButton(
                        text = "Conexiones y reloj",
                        onClick = onNavigateToMisDispositivos
                    )
                }
            }

            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
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
                    com.opofit.miapp.ui.components.PrimaryButton(
                        text = "Actualizar contraseña",
                        enabled = passActual.length >= 4 && passNueva.length >= 6,
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
                        }
                    )
                }
            }

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.errorContainer
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Zona de peligro",
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

            // Sección Tutorial: permite resetear los coach marks para ver
            // los tips de cada pantalla otra vez. Útil cuando un nuevo usuario
            // entra a la cuenta o el usuario quiere refrescar la memoria.
            SectionHeader(title = "Tutorial", subtitle = "Vuelve a ver los tips de cada pantalla")
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Ver tutorial otra vez",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Resetea los tips de bienvenida. La próxima vez que entres a Home, GPS, Plan, Simulacro, Perfil… volverás a ver las explicaciones.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = {
                            com.opofit.miapp.ui.components.resetAllCoachMarks(context)
                            scope.launch {
                                snackbarHostState.showSnackbar("Tutorial reactivado. Vuelve a entrar a cada pantalla.")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reactivar tutorial")
                    }
                }
            }

            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
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
