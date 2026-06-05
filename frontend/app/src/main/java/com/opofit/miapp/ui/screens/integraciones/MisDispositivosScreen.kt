package com.opofit.miapp.ui.screens.integraciones

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
import com.opofit.miapp.integraciones.IntegracionesViewModel
import com.opofit.miapp.ui.MainActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisDispositivosScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGpsHub: () -> Unit = {},
    viewModel: IntegracionesViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
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
                title = { Text("Mis dispositivos", fontWeight = FontWeight.Bold) },
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
                title = { Text("Aviso de Google (normal en desarrollo)") },
                text = {
                    Text(
                        "Google Fit pide permisos sensibles y muestra que la app «no está verificada». " +
                            "Es normal mientras OpoFit está en pruebas — no es un virus.\n\n" +
                            "Si sale la pantalla roja de Google:\n" +
                            "1. NO pulses «Back to safety» / «Volver a un lugar seguro».\n" +
                            "2. Abajo, pulsa «Advanced» / «Avanzado».\n" +
                            "3. Pulsa «Go to project… (unsafe)» / «Ir al proyecto…».\n" +
                            "4. Elige tu cuenta Google y acepta TODOS los permisos de actividad.\n\n" +
                            "Si tu correo no está en usuarios de prueba del proyecto Google Cloud, " +
                            "añádelo en la consola OAuth (miapp-opofit-7088e)."
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Conecta tu reloj sin APIs de pago",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "Funciona con Garmin, Polar, Samsung, Fitbit, Mi Band, Amazfit, Suunto, Coros y más.\n\n" +
                                "Recomendado:\n" +
                                "1. Health Connect — sincronización automática (Android 9+ con HC).\n" +
                                "2. Google Fit — alternativa si tu móvil no tiene Health Connect.\n" +
                                "3. Importar GPX — exporta desde Strava/Garmin/Wikiloc e impórtalo en Rutas GPS.\n" +
                                "4. Banda BLE — pulso en vivo durante una actividad GPS.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            item {
                ImportGpxCard(onOpenGpsHub = onNavigateToGpsHub)
            }
            item {
                RelojesGuiaCard()
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
                BleDirectoCard()
            }
            item {
                Card(
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
                                "Conexiones opcionales (avanzado)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Strava / Polar — requieren configuración del servidor",
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
                AppleWatchCard()
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.UploadFile,
                                null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                "  Enviar entrenamientos al reloj",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        Text(
                            "Desde el plan o el detalle de una sesión, pulsa \"Enviar al reloj\": OpoFit genera un fichero TCX estándar que tu app del reloj (Garmin Connect, Polar Flow, Zepp, etc.) importa con un toque.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
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
    Card(modifier = Modifier.fillMaxWidth()) {
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "¿Cómo se conecta mi reloj?",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "OpoFit no empareja el reloj por Bluetooth como un auricular. " +
                    "Tu reloj envía los entrenos a Health Connect o Google Fit y OpoFit los lee desde ahí.",
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
        title = "Health Connect (recomendado)",
        subtitle = "Garmin, Samsung, Fitbit, Mi Band, Amazfit, Whoop, Oura...",
        icon = Icons.Filled.Cloud,
        estado = estadoTxt,
        estadoOk = connected
    ) {
        Text(
            "Pasos para conectar tu reloj:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "Cuando funcione verás «Conectado» arriba a la derecha.\n\n" +
                "1. Instala Health Connect si no lo tienes.\n" +
                "2. En la app de tu reloj activa «Compartir con Health Connect».\n" +
                "3. Pulsa «Conceder permisos» y marca TODOS los datos que pide OpoFit.\n" +
                "4. Si no sale el diálogo, pulsa «Abrir Health Connect» → Permisos de aplicaciones → OpoFit → Permitir todo.",
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
                "Strava exige suscripción de desarrollador para nuevas apps. " +
                    "No es necesario para usar OpoFit: usa Health Connect o importa GPX desde Rutas GPS.",
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
                "Requiere credenciales Polar en el servidor (POLAR_CLIENT_ID/SECRET). " +
                    "Alternativa: Health Connect o importar GPX desde Rutas GPS.",
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
private fun ImportGpxCard(onOpenGpsHub: () -> Unit) {
    ProviderCard(
        title = "Importar desde Strava / Garmin / Wikiloc",
        subtitle = "Sin API de pago — exporta GPX e impórtalo en OpoFit",
        icon = Icons.Filled.UploadFile,
        estado = "Recomendado",
        estadoOk = true
    ) {
        Text(
            "En Strava: actividad → ⋮ → Exportar GPX. En Garmin/Wikiloc: exportar ruta como .gpx. " +
                "Luego en Rutas GPS pulsa \"Elegir fichero .gpx\".",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onOpenGpsHub) {
            Text("Ir a Rutas GPS e importar")
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
        title = "Google Fit (alternativa)",
        subtitle = "Para móviles sin Health Connect · Wear OS, Xiaomi, apps con sync a Fit",
        icon = Icons.Filled.Cloud,
        estado = if (connected) "Conectado" else "Sin permisos",
        estadoOk = connected
    ) {
        Text(
            "Pasos para conectar tu reloj:\n\n" +
                "1. Instala Google Fit y activa sync en la app de tu reloj.\n" +
                "2. Pulsa «Conceder permisos».\n" +
                "3. Google puede avisar «app no verificada» — es normal en desarrollo.\n" +
                "   Pulsa Avanzado → Ir al proyecto → Aceptar permisos.\n" +
                "4. Si no deja entrar, el desarrollador debe añadir tu Gmail como " +
                "«usuario de prueba» en Google Cloud Console.",
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
private fun BleDirectoCard() {
    ProviderCard(
        title = "Banda BLE directa",
        subtitle = "Pulso en vivo: Polar H10/H9, Wahoo TICKR, chest straps genéricos, Garmin (broadcast HR)",
        icon = Icons.Filled.Bluetooth,
        estado = "Activable en GPS",
        estadoOk = true
    ) {
        Text(
            "Desde el Hub de Rutas GPS pulsa \"Conectar reloj/banda\" y vincula tu dispositivo. El pulso se mostrará en tiempo real durante la actividad.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AppleWatchCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
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
