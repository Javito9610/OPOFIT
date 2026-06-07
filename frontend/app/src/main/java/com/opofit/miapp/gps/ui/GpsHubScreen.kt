package com.opofit.miapp.gps.ui

import com.opofit.miapp.ui.components.ElevatedCard
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opofit.miapp.gps.model.ActivitySummary
import com.opofit.miapp.gps.model.ActivityType
import com.opofit.miapp.gps.service.EntrenoFlowContext
import com.opofit.miapp.gps.service.GpsRecordingContext
import com.opofit.miapp.gps.service.HrBleManager
import com.opofit.miapp.gps.util.GpsMetrics
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpsHubScreen(
    onNavigateBack: () -> Unit,
    onStartRecording: () -> Unit,
    onOpenMapa: (ActivityType) -> Unit = {},
    onOpenActivity: (String) -> Unit,
    viewModel: GpsViewModel = viewModel()
) {
    val selectedType by viewModel.selectedType.collectAsState()
    val history by viewModel.history.collectAsState()
    val tracking by viewModel.tracking.collectAsState()
    val hrState by viewModel.hrState.collectAsState()
    val importMsg by viewModel.importMessage.collectAsState()
    val flowCtx by EntrenoFlowContext.state.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showPermDialog by remember { mutableStateOf(false) }
    var showHrDialog by remember { mutableStateOf(false) }

    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        val ok = granted[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (ok) {
            GpsRecordingContext.prepare(selectedType, conRuta = false)
            onStartRecording()
        } else showPermDialog = true
    }

    val blePermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Después de la solicitud relanzamos el escaneo.
        if (viewModel.hrManager().hasPermissions()) {
            showHrDialog = true
            viewModel.startHrScan()
        } else {
            showPermDialog = true
        }
    }

    fun startWithPermission() {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED) {
            GpsRecordingContext.prepare(selectedType, conRuta = false)
            onStartRecording()
        } else {
            val perms = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                perms += Manifest.permission.POST_NOTIFICATIONS
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                perms += Manifest.permission.ACTIVITY_RECOGNITION
            }
            permLauncher.launch(perms.toTypedArray())
        }
    }

    fun openHrDialog() {
        if (viewModel.hrManager().hasPermissions()) {
            showHrDialog = true
            viewModel.startHrScan()
        } else {
            val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            } else {
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            blePermLauncher.launch(perms)
        }
    }

    val actividadPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importActividad(uri)
    }

    LaunchedEffect(Unit) {
        viewModel.loadHistory()
        viewModel.hrManager().autoConnectSavedDevice()
    }

    LaunchedEffect(importMsg) {
        importMsg?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeImportMessage()
        }
    }

    if (showPermDialog) {
        AlertDialog(
            onDismissRequest = { showPermDialog = false },
            confirmButton = {
                Button(onClick = { showPermDialog = false }) { Text("Entendido") }
            },
            title = { Text("Permisos necesarios") },
            text = {
                Text(
                    "Necesitamos acceso al GPS para registrar tu ruta. " +
                        "Opcionalmente puedes conectar una banda o reloj para ver el pulso en vivo."
                )
            }
        )
    }

    if (showHrDialog) {
        HrConnectDialog(
            viewModel = viewModel,
            hrState = hrState,
            onDismiss = {
                showHrDialog = false
                viewModel.stopHrScan()
            },
            onOpenHealthConnect = {
                // Abre directamente la pantalla de permisos de Health Connect.
                // Si HC no está instalado, abre Play Store.
                val hc = com.opofit.miapp.integraciones.HealthConnectManager.get(context)
                if (!hc.openHealthConnect(context)) {
                    try {
                        val storeIntent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(
                                "https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata"
                            )
                        ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(storeIntent)
                    } catch (_: Exception) { /* sin Play Store */ }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Rutas GPS", fontWeight = FontWeight.Bold)
                        Text(
                            "Carreras, paseos y bici al aire libre",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
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
        PullToRefreshBox(
            isRefreshing = history.loading,
            onRefresh = { viewModel.syncDesdeReloj() },
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            flowCtx?.returnRoute?.let { _ ->
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Entrenamiento en curso",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            flowCtx?.tituloSesion?.let { t ->
                                Text("Sesión: $t", style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                "Al guardar la carrera volverás al registro del entreno con la distancia rellenada.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                            )
                            OutlinedButton(
                                onClick = { onOpenMapa(selectedType) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.Explore, null, Modifier.size(18.dp))
                                Spacer(Modifier.size(6.dp))
                                Text("Ver o cambiar ruta sugerida")
                            }
                        }
                    }
                }
            }

            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Empieza una actividad",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        val hrConnected = hrState is HrBleManager.State.Connected
                        if (hrConnected) {
                            Text(
                                "Reloj conectado · el pulso se mostrará durante la actividad.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        } else {
                            Text(
                                "Ritmo, distancia, mapa en vivo y pulso opcional.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                TypeChip(
                                    ActivityType.RUN, Icons.AutoMirrored.Filled.DirectionsRun,
                                    selected = selectedType == ActivityType.RUN
                                ) { viewModel.selectType(ActivityType.RUN) }
                            }
                            item {
                                TypeChip(
                                    ActivityType.WALK, Icons.AutoMirrored.Filled.DirectionsWalk,
                                    selected = selectedType == ActivityType.WALK
                                ) { viewModel.selectType(ActivityType.WALK) }
                            }
                            item {
                                TypeChip(
                                    ActivityType.BIKE, Icons.AutoMirrored.Filled.DirectionsBike,
                                    selected = selectedType == ActivityType.BIKE
                                ) { viewModel.selectType(ActivityType.BIKE) }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val connected = hrState is HrBleManager.State.Connected
                            AssistChip(
                                onClick = { openHrDialog() },
                                label = {
                                    val name = (hrState as? HrBleManager.State.Connected)?.device?.name
                                    Text(
                                        if (connected) "Pulso: ${name ?: "conectado"} ✓"
                                        else "Conectar pulso (opcional)"
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        if (connected) Icons.Filled.BluetoothConnected else Icons.Filled.Bluetooth,
                                        null
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (connected) MaterialTheme.colorScheme.tertiaryContainer
                                    else MaterialTheme.colorScheme.surface
                                )
                            )
                            if (connected) {
                                TextButton(onClick = { viewModel.disconnectHr() }) { Text("Desconectar") }
                            }
                        }
                        if (tracking.active) {
                            OutlinedButton(
                                onClick = onStartRecording,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.PlayArrow, null, Modifier.size(20.dp))
                                Spacer(Modifier.size(8.dp))
                                Text("Continuar actividad en curso")
                            }
                        } else {
                            Button(
                                onClick = { startWithPermission() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.PlayArrow, null, Modifier.size(20.dp))
                                Spacer(Modifier.size(8.dp))
                                Text("Iniciar ${selectedType.display.lowercase()}")
                            }
                            Text(
                                "Verás tu ruta dibujándose en el mapa en tiempo real (estilo Strava). Activa el GPS del móvil para mejor precisión.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = { onOpenMapa(selectedType) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Explore, null, Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text(
                                when (selectedType) {
                                    ActivityType.BIKE -> "Preparar ruta de bici"
                                    ActivityType.WALK -> "Preparar ruta de paseo"
                                    ActivityType.RUN -> "Preparar ruta de carrera"
                                }
                            )
                        }
                    }
                }
            }

            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.UploadFile, null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                "  Traer actividad del reloj",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            "Desliza hacia abajo para sincronizar automáticamente, o sube un archivo " +
                                "exportado desde Garmin, Polar, Strava u otra app.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = {
                                actividadPicker.launch(
                                    arrayOf("application/gpx+xml", "application/xml", "application/octet-stream", "*/*")
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.UploadFile, null, Modifier.size(18.dp))
                            Text("  Subir archivo del reloj")
                        }
                    }
                }
            }

            item {
                Text(
                    "Historial",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (history.items.isEmpty()) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Aún no tienes actividades GPS. Sal a entrenar y aparecerán aquí.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(history.items, key = { it.id }) { activity ->
                    ActivityRow(activity, onClick = { onOpenActivity(activity.id) })
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
        }
    }
}

@Composable
private fun TypeChip(
    type: ActivityType,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(type.display) },
        leadingIcon = { Icon(icon, null, Modifier.size(18.dp)) }
    )
}

@Composable
private fun ActivityRow(activity: ActivitySummary, onClick: () -> Unit) {
    val date = SimpleDateFormat("d MMM · HH:mm", Locale.forLanguageTag("es-ES"))
        .format(Date(activity.startedAtMs))
    ElevatedCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(activity.type.emoji, style = MaterialTheme.typography.headlineMedium)
            Column(Modifier.weight(1f)) {
                Text(
                    "${activity.type.display} · $date",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${GpsMetrics.formatDistance(activity.distanceM)}  ·  " +
                        "${GpsMetrics.formatDuration(activity.durationSec)}  ·  " +
                        "${GpsMetrics.formatPace(activity.avgPaceSecPerKm)}/km",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (activity.elevationGainM > 0) {
                        Text(
                            "+${activity.elevationGainM.toInt()} m",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    activity.kcal?.takeIf { it > 0 }?.let {
                        Text("$it kcal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    activity.avgHrBpm?.let {
                        Text("♥ $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun HrConnectDialog(
    viewModel: GpsViewModel,
    hrState: HrBleManager.State,
    onDismiss: () -> Unit,
    onOpenHealthConnect: () -> Unit = {}
) {
    val found by viewModel.hrFound.collectAsState()
    val liveHr by viewModel.hrManager().heartRate.collectAsState()
    val connectingAddress = (hrState as? HrBleManager.State.Connecting)?.device?.address
    val connectedAddress = (hrState as? HrBleManager.State.Connected)?.device?.address

    // Emparejados al sistema (BT Classic + BLE bonded). Se recargan cada vez que se abre
    // el diálogo. Aquí saldrá el Amazfit/Mi Band aunque NO se anuncie en el escaneo BLE.
    val paired = remember(hrState) { viewModel.pairedHrDevices() }

    Dialog(onDismissRequest = onDismiss) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
        ) {
            // Scroll exterior: cabe TODO el contenido, ningún botón ni texto queda cortado.
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Conectar pulso en vivo",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                // Estado actual (compacto: 1-2 líneas máx)
                when (val st = hrState) {
                    is HrBleManager.State.Idle -> Text(
                        if (found.isEmpty()) "Selecciona un dispositivo emparejado o pulsa Buscar."
                        else "${found.size} encontrado(s)",
                        style = MaterialTheme.typography.labelMedium
                    )
                    is HrBleManager.State.Scanning -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.size(6.dp))
                        Text("Buscando…", style = MaterialTheme.typography.labelMedium)
                    }
                    is HrBleManager.State.Connecting -> Text(
                        "Conectando a ${st.device.name ?: st.device.address}…",
                        style = MaterialTheme.typography.labelMedium
                    )
                    is HrBleManager.State.Connected -> Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Favorite, null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.size(6.dp))
                            Text(
                                "Conectado a ${st.device.name ?: st.device.address}",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            liveHr?.let { "$it bpm en vivo" } ?: "Esperando pulso del reloj…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    is HrBleManager.State.Error -> Text(
                        st.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // SECCIÓN 1: emparejados (aquí va a estar SU Amazfit aunque no aparezca en escaneo).
                if (paired.isNotEmpty()) {
                    Text(
                        "Dispositivos emparejados a este móvil",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    paired.forEach { device ->
                        HrDeviceRow(
                            device = device,
                            isConnecting = connectingAddress == device.address,
                            isConnected = connectedAddress == device.address,
                            showSignal = false,
                            onClick = { viewModel.connectHr(device) }
                        )
                    }
                }

                // SECCIÓN 2: encontrados al escanear.
                if (found.isNotEmpty()) {
                    Text(
                        "Encontrados al escanear",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    found.forEach { device ->
                        HrDeviceRow(
                            device = device,
                            isConnecting = connectingAddress == device.address,
                            isConnected = connectedAddress == device.address,
                            showSignal = true,
                            onClick = { viewModel.connectHr(device) }
                        )
                    }
                }

                // Botones SIEMPRE activos. Si estás escaneando y los pulsas, se reinicia el escaneo.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.startHrScan() },
                        modifier = Modifier.weight(1f)
                    ) { Text("Buscar pulso") }
                    OutlinedButton(
                        onClick = { viewModel.startHrScanBroad() },
                        modifier = Modifier.weight(1f)
                    ) { Text("Mostrar todo BT") }
                }

                // Si está escaneando, dar opción de parar
                if (hrState is HrBleManager.State.Scanning) {
                    TextButton(
                        onClick = { viewModel.stopHrScan() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Detener búsqueda") }
                }

                // Tarjeta de Health Connect (compacta).
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "¿Amazfit, Mi Band o Garmin?",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            "Si el reloj no se conecta por BLE, usa Health Connect: " +
                                "el pulso llega vía la app del reloj.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Button(
                            onClick = { onDismiss(); onOpenHealthConnect() },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Abrir Health Connect") }
                    }
                }

                if (hrState is HrBleManager.State.Connected) {
                    OutlinedButton(
                        onClick = { viewModel.disconnectHr() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Desconectar") }
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) { Text("Cerrar") }
            }
        }
    }
}

@Composable
private fun HrDeviceRow(
    device: HrBleManager.FoundDevice,
    isConnecting: Boolean,
    isConnected: Boolean,
    showSignal: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isConnecting, onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = when {
            isConnected -> MaterialTheme.colorScheme.tertiaryContainer
            isConnecting -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        tonalElevation = 1.dp
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                if (isConnected) Icons.Filled.BluetoothConnected else Icons.Filled.Bluetooth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(Modifier.weight(1f)) {
                Text(
                    device.name?.ifBlank { null } ?: "Dispositivo sin nombre",
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (showSignal) {
                    Text(
                        when {
                            device.rssi >= -60 -> "Señal fuerte"
                            device.rssi >= -75 -> "Señal media"
                            else -> "Señal débil — acércate"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        "Emparejado",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            when {
                isConnecting -> CircularProgressIndicator(
                    modifier = Modifier.size(18.dp), strokeWidth = 2.dp
                )
                isConnected -> Text(
                    "OK",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                else -> Text(
                    "Conectar",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
