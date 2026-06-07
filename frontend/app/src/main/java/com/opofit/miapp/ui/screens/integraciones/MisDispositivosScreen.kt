package com.opofit.miapp.ui.screens.integraciones

import com.opofit.miapp.ui.components.ElevatedCard
import com.opofit.miapp.ui.components.InfoTip
import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
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
import androidx.compose.material3.TextButton
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.opofit.miapp.data.api.IntegracionesApi
import com.opofit.miapp.data.api.ProveedorEstado
import com.opofit.miapp.data.local.TokenManager
import com.opofit.miapp.gps.ui.GpsViewModel
import com.opofit.miapp.integraciones.GoogleFitManager
import com.opofit.miapp.integraciones.HealthConnectManager
import com.opofit.miapp.integraciones.IntegracionesViewModel
import com.opofit.miapp.ui.MainActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Pantalla "Conexiones y reloj".
 *
 * Versión simplificada (anterior tenía 11 secciones, era ilegible):
 *   1. Hero con 1 línea de qué hace esta pantalla.
 *   2. Health Connect (camino principal — 95% de usuarios).
 *   3. Pulso en vivo BLE (banda de pecho durante carrera).
 *   4. Subir archivo manualmente.
 *   5. "Más opciones" desplegable: Strava, Polar, Google Fit (legacy).
 *
 * Cada sección tiene un botón `?` (InfoTip) que abre popup con detalle.
 * Se eliminaron: "Enviar plan al reloj" (no tenía funcionalidad), RelojesGuiaCard
 * (texto kilométrico) y Apple Watch (informativo sin acción).
 */
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
                    snackbarHostState.showSnackbar("Permisos OK. Sincronizando…")
                    viewModel.syncHealthConnect()
                }
                granted.isNotEmpty() -> {
                    snackbarHostState.showSnackbar(
                        "Faltan permisos. Abre Health Connect y activa todos los datos."
                    )
                    viewModel.refresh()
                }
                else -> {
                    snackbarHostState.showSnackbar("Concede los permisos a OpoFit en Health Connect")
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
                            if (granted) {
                                snackbarHostState.showSnackbar("Permisos OK. Sincronizando…")
                                viewModel.syncGoogleFit()
                            } else {
                                snackbarHostState.showSnackbar("Permisos no concedidos.")
                                viewModel.refresh()
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

    if (showGoogleFitAviso) {
        AlertDialog(
            onDismissRequest = { showGoogleFitAviso = false },
            title = { Text("Permisos de Google Fit") },
            text = {
                Text(
                    "Google puede mostrar que la app «no está verificada». Si aparece esa advertencia:\n" +
                        "1. Pulsa «Avanzado» (no «Volver»).\n" +
                        "2. Pulsa «Continuar».\n" +
                        "3. Acepta los permisos de actividad."
                )
            },
            confirmButton = {
                Button(onClick = {
                    showGoogleFitAviso = false
                    lanzarGoogleFitOAuth()
                }) { Text("Continuar") }
            },
            dismissButton = {
                TextButton(onClick = { showGoogleFitAviso = false }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conexiones", fontWeight = FontWeight.Bold) },
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
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // SECCIÓN 1: Hero con 1 línea
            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Conecta tu reloj o banda",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Tus entrenos llegan automáticamente. Sin emparejar nada raro.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // SECCIÓN 2: Health Connect (camino principal)
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

            // SECCIÓN 3: Pulso en vivo BLE
            item { BleDirectoCard(onOpenGps = onNavigateToGpsHub) }

            // SECCIÓN 4: Subir manualmente
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

            // SECCIÓN 5: Avanzado (Strava + Polar + Google Fit legacy)
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
                                "Conexiones avanzadas",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Strava · Polar · Google Fit",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            if (showAvanzado) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            null
                        )
                    }
                }
            }
            if (showAvanzado) {
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
                item {
                    GoogleFitCard(
                        connected = state.gfConnected,
                        loading = state.loading,
                        onRequest = { requestGoogleFitPermissions() },
                        onSync = { viewModel.syncGoogleFit() },
                        onOpenGoogleFit = { gfManager.openGoogleFit() }
                    )
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

/* ------- Componentes simplificados ------- */

@Composable
private fun ProviderCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    estado: String,
    estadoOk: Boolean,
    infoTitle: String? = null,
    infoText: String? = null,
    content: @Composable () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.padding(start = 10.dp).weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (infoTitle != null && infoText != null) {
                            InfoTip(title = infoTitle, text = infoText)
                        }
                    }
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AssistChip(
                    onClick = { },
                    label = { Text(estado, style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = {
                        Icon(
                            if (estadoOk) Icons.Filled.CheckCircle else Icons.Filled.CloudOff,
                            null,
                            Modifier.size(14.dp)
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
        title = "Health Connect",
        subtitle = "Recomendado · sincronización automática",
        icon = Icons.Filled.Cloud,
        estado = estadoTxt,
        estadoOk = connected,
        infoTitle = "¿Cómo funciona Health Connect?",
        infoText = "Es un \"puente\" oficial de Google entre apps de salud. " +
            "Tu reloj (Garmin/Samsung/Xiaomi/Amazfit/Fitbit…) envía datos a su app, " +
            "esa app los escribe en Health Connect, y OpoFit los lee.\n\n" +
            "1. Instala Health Connect si tu Android no lo trae (Android 14+ ya viene).\n" +
            "2. En la app de tu reloj activa «Compartir con Health Connect».\n" +
            "3. Aquí pulsa «Conceder permisos» y acepta todos los datos.\n" +
            "4. «Sincronizar» trae tus entrenos recientes."
    ) {
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
                        Text("Abrir")
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
private fun BleDirectoCard(onOpenGps: () -> Unit) {
    ProviderCard(
        title = "Pulso en vivo",
        subtitle = "Banda de pecho o reloj con broadcast HR",
        icon = Icons.Filled.Bluetooth,
        estado = "Carrera GPS",
        estadoOk = true,
        infoTitle = "¿Para qué sirve el pulso en vivo?",
        infoText = "Mientras grabas una carrera GPS, ves tu pulso en tiempo real " +
            "(graficado y resaltado por zona cardíaca).\n\n" +
            "Funciona con:\n" +
            "• Bandas de pecho Polar (H9, H10) y Wahoo TICKR.\n" +
            "• Relojes Amazfit/Garmin con Broadcast HR activado.\n\n" +
            "Para conectar, abre Rutas GPS → Conectar pulso."
    ) {
        OutlinedButton(onClick = onOpenGps) { Text("Ir a Rutas GPS") }
    }
}

@Composable
private fun ManualImportCard(
    onPickFile: () -> Unit,
    onOpenHistorial: () -> Unit
) {
    ProviderCard(
        title = "Subir actividad manualmente",
        subtitle = "Archivo GPX, TCX o FIT",
        icon = Icons.Filled.UploadFile,
        estado = "Manual",
        estadoOk = true,
        infoTitle = "¿Cuándo usar la subida manual?",
        infoText = "Si entrenaste sin sincronización automática, o si Health Connect no te funcionó. " +
            "Exporta el archivo de tu actividad desde Garmin Connect, Polar Flow, Zepp o Wikiloc, " +
            "y súbelo aquí. OpoFit lo añade al historial GPS."
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onPickFile) { Text("Elegir archivo") }
            OutlinedButton(onClick = onOpenHistorial) { Text("Ver historial") }
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
        subtitle = "Puente universal: Garmin, Polar, Suunto, Apple Watch (vía iPhone)",
        icon = Icons.Filled.Cloud,
        estado = estadoTxt,
        estadoOk = conectado,
        infoTitle = "¿Cuándo usar Strava?",
        infoText = "Si ya usas Strava como repositorio central, conectar aquí mantiene los entrenos " +
            "sincronizados sin pasar por Health Connect.\n\n" +
            "Para la mayoría de usuarios, Health Connect es más simple."
    ) {
        if (!configurado) {
            Text(
                "Esta conexión necesita configuración en el servidor (admin).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@ProviderCard
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!conectado) {
                Button(onClick = onConnect) { Text("Conectar") }
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
        subtitle = "Conexión directa con Polar Flow",
        icon = Icons.Filled.Cloud,
        estado = estadoTxt,
        estadoOk = conectado,
        infoTitle = "¿Cuándo usar Polar AccessLink?",
        infoText = "Si tienes un Polar (H10, Vantage, Ignite…) y quieres sincronización directa " +
            "sin pasar por Health Connect. Necesita autorización en Polar Flow."
    ) {
        if (!configurado) {
            Text(
                "Esta conexión necesita configuración en el servidor (admin).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@ProviderCard
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!conectado) {
                Button(onClick = onConnect) { Text("Conectar") }
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
private fun GoogleFitCard(
    connected: Boolean,
    loading: Boolean,
    onRequest: () -> Unit,
    onSync: () -> Unit,
    onOpenGoogleFit: () -> Unit
) {
    ProviderCard(
        title = "Google Fit",
        subtitle = "Legado · Google lo retira en 2026, usa Health Connect",
        icon = Icons.Filled.Cloud,
        estado = if (connected) "Conectado" else "Sin permisos",
        estadoOk = connected,
        infoTitle = "¿Por qué Google Fit aparece como legado?",
        infoText = "Google está reemplazando Google Fit por Health Connect. " +
            "Si tu móvil es Android 14 o superior, usa Health Connect arriba.\n\n" +
            "Mantenemos esta opción para móviles antiguos que aún no tienen Health Connect."
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!connected) {
                Button(onClick = onRequest, enabled = !loading) { Text("Conceder permisos") }
            } else {
                Button(onClick = onSync, enabled = !loading) {
                    Icon(Icons.Filled.Sync, null, Modifier.size(16.dp))
                    Text("  Sincronizar")
                }
            }
            OutlinedButton(onClick = onOpenGoogleFit, enabled = !loading) { Text("Abrir") }
            if (loading) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            }
        }
    }
}
