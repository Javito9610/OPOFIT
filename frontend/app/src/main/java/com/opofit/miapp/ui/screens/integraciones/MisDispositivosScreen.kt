package com.opofit.miapp.ui.screens.integraciones

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
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
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.opofit.miapp.integraciones.HealthConnectManager
import com.opofit.miapp.integraciones.IntegracionesViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisDispositivosScreen(
    onNavigateBack: () -> Unit,
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

    val hcPermLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        if (granted.isNotEmpty()) viewModel.syncHealthConnect()
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
                            "Conecta tu reloj o pulsera",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "OpoFit se conecta con prácticamente cualquier reloj o pulsera del mercado " +
                                "(Garmin, Polar, Mi Band, Amazfit, Fitbit, Samsung Galaxy Watch, Suunto, Coros, " +
                                "Wahoo, Whoop, Oura, y más).\n\n" +
                                "Los tres caminos:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "1. Health Connect — el camino recomendado en Android. Tu reloj envía datos " +
                                "a Health Connect y OpoFit los lee.\n" +
                                "2. Strava — funciona para todos los relojes que ya sincronizan a Strava " +
                                "(incluso Apple Watch vía iPhone).\n" +
                                "3. Polar AccessLink — si tienes un Polar y prefieres conexión directa.",
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
                    onRequest = { hcPermLauncher.launch(HealthConnectManager.get(context).permissions) },
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
                BleDirectoCard()
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
private fun HealthConnectCard(
    availability: HealthConnectManager.Availability,
    connected: Boolean,
    loading: Boolean,
    onRequest: () -> Unit,
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
            "1. Asegúrate de tener Health Connect instalado (botón abajo si no).\n" +
                "2. Abre la app oficial de tu reloj (Garmin Connect, Mi Fitness, Zepp Life, Samsung Health, etc.) " +
                "y activa \"Compartir con Health Connect\".\n" +
                "3. Vuelve aquí y pulsa \"Conceder permisos\". OpoFit leerá automáticamente tus actividades.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when (availability) {
                HealthConnectManager.Availability.NOT_INSTALLED -> {
                    Button(onClick = onInstall) { Text("Instalar Health Connect") }
                }
                HealthConnectManager.Availability.NOT_SUPPORTED -> {
                    Text(
                        "Tu Android no soporta Health Connect.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                HealthConnectManager.Availability.AVAILABLE -> {
                    if (!connected) {
                        Button(onClick = onRequest, enabled = !loading) { Text("Conceder permisos") }
                    } else {
                        Button(onClick = onSync, enabled = !loading) {
                            Icon(Icons.Filled.Sync, null, Modifier.size(16.dp))
                            Text("  Sincronizar")
                        }
                    }
                }
            }
            if (loading) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
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
                "El backend no tiene credenciales de Strava. Añade STRAVA_CLIENT_ID y STRAVA_CLIENT_SECRET al .env del servidor.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
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
                "El backend no tiene credenciales de Polar. Añade POLAR_CLIENT_ID y POLAR_CLIENT_SECRET al .env del servidor.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
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
