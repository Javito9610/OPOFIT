package com.opofit.miapp.ui.screens.integraciones

import com.opofit.miapp.ui.components.ElevatedCard
import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opofit.miapp.data.api.IntegracionesApi
import com.opofit.miapp.data.api.ProveedorEstado
import com.opofit.miapp.data.local.TokenManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.opofit.miapp.integraciones.GoogleFitManager
import com.opofit.miapp.integraciones.HealthConnectManager
import com.opofit.miapp.gps.ui.GpsViewModel
import com.opofit.miapp.integraciones.IntegracionesViewModel
import com.opofit.miapp.ui.MainActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisDispositivosScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGpsHub: () -> Unit = {},
    viewModel: IntegracionesViewModel = viewModel(),
    gpsViewModel: GpsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val importMsg by gpsViewModel.importMessage.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val tokenManager = remember { TokenManager(context) }

    LaunchedEffect(Unit) { viewModel.refresh() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    LaunchedEffect(importMsg) {
        importMsg?.let {
            snackbarHostState.showSnackbar(it)
            gpsViewModel.consumeImportMessage()
        }
    }

    val archivoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) gpsViewModel.importActividad(uri)
    }

    val hcManager = remember { HealthConnectManager.get(context) }
    val gfManager = remember { GoogleFitManager.get(context) }
    val activity = context as? Activity

    val gfSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        scope.launch {
            if (gfManager.hasPermissions()) {
                snackbarHostState.showSnackbar("Google Fit conectado. Sincronizando…")
                viewModel.syncGoogleFit()
            } else {
                snackbarHostState.showSnackbar("Activa los permisos de Google Fit para OpoFit")
                viewModel.refresh()
            }
        }
    }
    val hcPermLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        scope.launch {
            when {
                granted.containsAll(hcManager.permissions) -> {
                    snackbarHostState.showSnackbar("Permisos concedidos. Sincronizando actividades…")
                    viewModel.syncHealthConnect()
                }
                granted.isNotEmpty() -> {
                    snackbarHostState.showSnackbar(
                        "Permisos parciales. Abre Health Connect y activa todos los datos de OpoFit."
                    )
                    viewModel.refresh()
                }
                else -> {
                    snackbarHostState.showSnackbar(
                        "Permisos no concedidos. Abriendo Health Connect para autorizar OpoFit…"
                    )
                    hcManager.openManagePermissions(context)
                    viewModel.refresh()
                }
            }
        }
    }

    fun requestHealthConnectPermissions() {
        if (hcManager.availability() != HealthConnectManager.Availability.AVAILABLE) return
        runCatching {
            hcPermLauncher.launch(hcManager.permissions)
        }.onFailure {
            scope.launch {
                snackbarHostState.showSnackbar("Abriendo Health Connect…")
                if (!hcManager.openHealthConnect(context)) {
                    snackbarHostState.showSnackbar("Instala Health Connect desde Play Store")
                }
            }
        }
    }

    fun openOauthUrl(url: String) {
        scope.launch {
            val token = tokenManager.getToken().first().orEmpty()
            val withToken = "$url?token=${Uri.encode(token)}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(withToken))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { startActivity(context, intent, null) }
        }
    }

    val proveedores = state.estado.proveedores.associateBy { it.provider }
    var showAvanzado by remember { mutableStateOf(false) }
    var showGoogleFitAviso by remember { mutableStateOf(false) }

    fun lanzarGoogleFitOAuth() {
        val mainActivity = activity as? MainActivity ?: return
        scope.launch {
            gfManager.trySilentSignIn()
            when {
                gfManager.hasPermissions() -> viewModel.syncGoogleFit()
                GoogleSignIn.getLastSignedInAccount(context) == null -> {
                    gfSignInLauncher.launch(gfManager.getSignInIntent(mainActivity))
                }
                else -> {
                    mainActivity.requestGoogleFitPermissions { granted ->
                        scope.launch {
                            when {
                                granted -> {
                                    snackbarHostState.showSnackbar(
                                        "Permisos concedidos. Sincronizando actividades…"
                                    )
                                    viewModel.syncGoogleFit()
                                }
                                else -> {
                                    snackbarHostState.showSnackbar(
                                        "Permisos no concedidos. Pulsa de nuevo o abre Google Fit."
                                    )
                                    viewModel.refresh()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun requestGoogleFitPermissions() {
        if (gfManager.hasPermissions()) {
            viewModel.syncGoogleFit()
            return
        }
        showGoogleFitAviso = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conexiones y reloj", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
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
        if (showGoogleFitAviso) {
            AlertDialog(
                onDismissRequest = { showGoogleFitAviso = false },
                title = { Text("Permisos de Google Fit") },
                text = {
                    Text(
                        "Google puede mostrar que la app «no está verificada». Es normal en esta fase.\n\n" +
                            "Si aparece una advertencia:\n" +
                            "1. Pulsa «Avanzado» (no «Volver»).\n" +
                            "2. Pulsa «Ir al proyecto» o «Continuar».\n" +
                            "3. Elige tu cuenta y acepta los permisos de actividad.\n\n" +
                            "Si no puedes continuar, usa Health Connect como alternativa principal."
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        showGoogleFitAviso = false
                        lanzarGoogleFitOAuth()
                    }) {
                        Text("Continuar con Google")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showGoogleFitAviso = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Sincroniza tu reloj con OpoFit",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "Compatible con Garmin, Polar, Samsung, Fitbit, Mi Band, Amazfit, Suunto y Coros.\n\n" +
                                "• Automático: Health Connect (recomendado) o Google Fit.\n" +
                                "• Manual: sube un archivo exportado desde tu app del reloj.\n" +
                                "• Pulso en vivo: opcional durante una carrera GPS.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            item {
                HealthConnectCard(
                    availability = state.hcAvailability,
                    connected = state.hcConnected,
                    loading = state.loading,
                    onRequest = { requestHealthConnectPermissions() },
                    onOpenHealthConnect = {
                        if (!hcManager.openManagePermissions(context)) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Instala Health Connect desde Play Store")
                            }
                        }
                    },
                    onSync = { viewModel.syncHealthConnect() },
                    onInstall = {
                        runCatching {
                            startActivity(
                                context,
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("market://details?id=com.google.android.apps.healthdata")
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                null
                            )
                        }
                    }
                )
            }
            item {
                GoogleFitCard(
                    connected = state.gfConnected,
                    loading = state.loading,
                    onRequest = { requestGoogleFitPermissions() },
                    onSync = { viewModel.syncGoogleFit() },
                    onOpenGoogleFit = { gfManager.openGoogleFit() }
                )
            }
            item {
                ManualImportCard(
                    onPickFile = {
                        archivoPicker.launch(
                            arrayOf("application/gpx+xml", "application/xml", "application/octet-stream", "*/*")
                        )
                    },
                    onOpenHistorial = onNavigateToGpsHub
                )
            }
            item {
                BleDirectoCard(onOpenGps = onNavigateToGpsHub)
            }
            item {
                RelojesGuiaCard()
            }
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showAvanzado = !showAvanzado }
                ) {
                    Row(
                        Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Más opciones",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Strava, Polar y Apple Watch",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            if (showAvanzado) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null
                        )
                    }
                }
            }
            if (showAvanzado) {
                item {
                    AppleWatchCard()
                }
                item {
                    StravaCard(
                        estado = proveedores["STRAVA"],
                        configurado = state.estado.stravaConfigured,
                        onConnect = { openOauthUrl(IntegracionesApi.stravaStartUrl()) },
                        onSync = { viewModel.syncStrava() },
                        onDisconnect = { viewModel.disconnectStrava() }
                    )
                }
                item {
                    PolarCard(
                        estado = proveedores["POLAR"],
                        configurado = state.estado.polarConfigured,
                        onConnect = { openOauthUrl(IntegracionesApi.polarStartUrl()) },
                        onSync = { viewModel.syncPolar() },
                        onDisconnect = { viewModel.disconnectPolar() }
                    )
                }
            }
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Enviar plan al reloj",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Durante un entrenamiento del plan, pulsa «Enviar plan al reloj». " +
                                "Tu app del reloj (Garmin Connect, Polar Flow, Zepp…) lo importará y podrás seguir la sesión desde la muñeca.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    estado: String,
    estadoOk: Boolean,
    content: @Composable () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.padding(start = 10.dp).weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AssistChip(
                    onClick = { },
                    label = { Text(estado) },
                    leadingIcon = {
                        Icon(
                            if (estadoOk) Icons.Filled.CheckCircle else Icons.Filled.CloudOff,
                            null,
                            Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (estadoOk) MaterialTheme.colorScheme.tertiaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
            content()
        }
    }
}

@Composable
private fun RelojesGuiaCard() {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "¿Cómo se conecta mi reloj?",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "OpoFit no empareja el reloj como un auricular. Tus entrenos llegan vía Health Connect " +
                    "o Google Fit, o puedes subir un archivo exportado manualmente.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text("Garmin", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "Garmin Connect → Configuración → Salud conectada / Health Connect → Activar y marcar actividades.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text("Amazfit / Zepp / Coros", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "Zepp / Mi Fitness / COROS → Ajustes → Health Connect o «Apps de terceros» → Permitir compartir entrenos.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text("Samsung / Fitbit / Polar", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "Samsung Health o la app del fabricante → Conexiones → Health Connect → Activar sincronización.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HealthConnectCard(
    availability: HealthConnectManager.Availability,
    connected: Boolean,
    loading: Boolean,
    onRequest: () -> Unit,
    onOpenHealthConnect: () -> Unit,
    onSync: () -> Unit,
    onInstall: () -> Unit
) {
    val estadoTxt = when {
        availability == HealthConnectManager.Availability.NOT_SUPPORTED -> "No disponible"
        availability == HealthConnectManager.Availability.NOT_INSTALLED -> "Instala HC"
        connected -> "Conectado"
        else -> "Sin permisos"
    }
    ProviderCard(
        title = "Sincronización automática",
        subtitle = "Health Connect · Garmin, Samsung, Fitbit, Mi Band, Amazfit…",
        icon = Icons.Filled.Cloud,
        estado = estadoTxt,
        estadoOk = connected
    ) {
        Text(
            "1. Instala Health Connect si tu móvil no lo tiene.\n" +
                "2. En la app de tu reloj, activa «Compartir con Health Connect».\n" +
                "3. Pulsa «Conceder permisos» y acepta todos los datos de actividad.\n" +
                "4. Pulsa «Sincronizar» para traer tus entrenos recientes.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        when (availability) {
            HealthConnectManager.Availability.NOT_INSTALLED -> {
                Button(onClick = onInstall, enabled = !loading) { Text("Instalar Health Connect") }
            }
            HealthConnectManager.Availability.NOT_SUPPORTED -> {
                Text(
                    "Tu Android no soporta Health Connect.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            HealthConnectManager.Availability.AVAILABLE -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!connected) {
                        Button(onClick = onRequest, enabled = !loading) { Text("Conceder permisos") }
                    } else {
                        Button(onClick = onSync, enabled = !loading) {
                            Icon(Icons.Filled.Sync, null, Modifier.size(16.dp))
                            Text("  Sincronizar")
                        }
                    }
                    OutlinedButton(onClick = onOpenHealthConnect, enabled = !loading) {
                        Text("Abrir Health Connect")
                    }
                    if (loading) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StravaCard(
    estado: ProveedorEstado?,
    configurado: Boolean,
    onConnect: () -> Unit,
    onSync: () -> Unit,
    onDisconnect: () -> Unit
) {
    val conectado = estado != null
    val estadoTxt = when {
        !configurado -> "No configurado"
        conectado -> "Conectado"
        else -> "Sin conectar"
    }
    ProviderCard(
        title = "Strava",
        subtitle = "Puerta universal: Garmin, Polar, Suunto, Coros, Apple Watch (vía iPhone+sync)",
        icon = Icons.Filled.Cloud,
        estado = estadoTxt,
        estadoOk = conectado
    ) {
        if (!configurado) {
            Text(
                "Opcional. Para la mayoría de usuarios basta con Health Connect o subir un archivo manualmente.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@ProviderCard
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!conectado) {
                Button(onClick = onConnect) { Text("Conectar Strava") }
            } else {
                Button(onClick = onSync) {
                    Icon(Icons.Filled.Sync, null, Modifier.size(16.dp))
                    Text("  Sincronizar")
                }
                OutlinedButton(onClick = onDisconnect) { Text("Desconectar") }
            }
        }
    }
}

@Composable
private fun PolarCard(
    estado: ProveedorEstado?,
    configurado: Boolean,
    onConnect: () -> Unit,
    onSync: () -> Unit,
    onDisconnect: () -> Unit
) {
    val conectado = estado != null
    val estadoTxt = when {
        !configurado -> "No configurado"
        conectado -> "Conectado"
        else -> "Sin conectar"
    }
    ProviderCard(
        title = "Polar AccessLink",
        subtitle = "Sincroniza directamente con tu cuenta Polar Flow",
        icon = Icons.Filled.Cloud,
        estado = estadoTxt,
        estadoOk = conectado
    ) {
        if (!configurado) {
            Text(
                "Conexión directa con Polar Flow. Si no está disponible, usa Health Connect.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@ProviderCard
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!conectado) {
                Button(onClick = onConnect) { Text("Conectar Polar") }
            } else {
                Button(onClick = onSync) {
                    Icon(Icons.Filled.Sync, null, Modifier.size(16.dp))
                    Text("  Sincronizar")
                }
                OutlinedButton(onClick = onDisconnect) { Text("Desconectar") }
            }
        }
    }
}

@Composable
private fun ManualImportCard(
    onPickFile: () -> Unit,
    onOpenHistorial: () -> Unit
) {
    ProviderCard(
        title = "Subir actividad manualmente",
        subtitle = "Garmin, Polar, Strava, Wikiloc…",
        icon = Icons.Filled.UploadFile,
        estado = "Manual",
        estadoOk = true
    ) {
        Text(
            "Si entrenaste sin sincronización automática, exporta la actividad desde la app de tu reloj " +
                "y súbela aquí. OpoFit la añadirá a tu historial GPS.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onPickFile) { Text("Elegir archivo") }
            OutlinedButton(onClick = onOpenHistorial) { Text("Ver historial GPS") }
        }
    }
}

@Composable
private fun GoogleFitCard(
    connected: Boolean,
    loading: Boolean,
    onRequest: () -> Unit,
    onSync: () -> Unit,
    onOpenGoogleFit: () -> Unit
) {
    ProviderCard(
        title = "Alternativa: Google Fit",
        subtitle = "Si tu móvil no tiene Health Connect",
        icon = Icons.Filled.Cloud,
        estado = if (connected) "Conectado" else "Sin permisos",
        estadoOk = connected
    ) {
        Text(
            "1. Instala Google Fit y activa la sincronización en la app de tu reloj.\n" +
                "2. Pulsa «Conceder permisos» y acepta los permisos de actividad.\n" +
                "3. Si Google muestra una advertencia, pulsa Avanzado y continúa.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!connected) {
                Button(onClick = onRequest, enabled = !loading) { Text("Conceder permisos") }
            } else {
                Button(onClick = onSync, enabled = !loading) {
                    Icon(Icons.Filled.Sync, null, Modifier.size(16.dp))
                    Text("  Sincronizar")
                }
            }
            OutlinedButton(onClick = onOpenGoogleFit, enabled = !loading) {
                Text("Abrir Google Fit")
            }
            if (loading) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            }
        }
    }
}

@Composable
private fun BleDirectoCard(onOpenGps: () -> Unit) {
    ProviderCard(
        title = "Pulso en vivo (opcional)",
        subtitle = "Banda de pecho o reloj con pulso por Bluetooth",
        icon = Icons.Filled.Bluetooth,
        estado = "Durante carrera",
        estadoOk = true
    ) {
        Text(
            "Conecta tu banda o reloj antes de empezar una carrera GPS. Verás el pulso en tiempo real " +
                "mientras grabas la actividad.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(onClick = onOpenGps) { Text("Ir a Rutas GPS") }
    }
}

@Composable
private fun AppleWatchCard() {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Watch, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "  Apple Watch",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "Apple Watch no se puede leer directamente desde Android (Apple lo cierra a iPhone). " +
                    "Para que sus actividades lleguen a OpoFit, sincroniza tu Apple Health con Strava desde el iPhone " +
                    "(apps como HealthFit o RunGap lo hacen automático) y conecta Strava aquí arriba.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
