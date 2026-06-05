package com.opofit.miapp.gps.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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

    LaunchedEffect(Unit) { viewModel.loadHistory() }

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
                    "Para registrar tu ruta necesitamos acceso al GPS, y para detectar cadencia el sensor de pasos. " +
                        "Si quieres también pulso, conecta una banda BLE (Polar, Garmin en broadcast, etc.). " +
                        "Apple Watch no es compatible con Android."
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
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Empieza una actividad",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "Ritmo, velocidad, cadencia, altitud, kcal y trazado en el mapa.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
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
                                        if (connected) "Reloj: ${name ?: "conectado"}"
                                        else "Conectar reloj/banda"
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
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.UploadFile, null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                "  Importar actividad (GPX / TCX)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            "¿Entrenaste sin el móvil? Exporta desde Garmin, Polar, Suunto o Strava " +
                                "como GPX/TCX, o desliza abajo para sincronizar Health Connect.",
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
                            Text("  Elegir fichero .gpx o .tcx")
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
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
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
    onDismiss: () -> Unit
) {
    val found by viewModel.hrFound.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Conectar pulsómetro",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Bandas BLE con pulso en vivo (Polar, Wahoo, Garmin broadcast HR). " +
                        "Amazfit/Zepp: activa «Broadcast HR» en la app del reloj; si no, usa Health Connect. " +
                        "Prueba «Buscar todos» si no aparece el tuyo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                when (val st = hrState) {
                    is HrBleManager.State.Idle -> {
                        Text(
                            if (found.isEmpty()) "Pulsa Buscar para escanear."
                            else "${found.size} dispositivo(s) — selecciona uno:",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    is HrBleManager.State.Scanning -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.size(8.dp))
                            Text("Buscando dispositivos BLE...")
                        }
                    }
                    is HrBleManager.State.Connecting -> Text("Conectando a ${st.device.name ?: st.device.address}...")
                    is HrBleManager.State.Connected -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Favorite, null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.size(6.dp))
                            Text("Conectado a ${st.device.name ?: st.device.address}")
                        }
                    }
                    is HrBleManager.State.Error -> Text(
                        st.message,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp, max = 260.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (found.isEmpty()) {
                        item {
                            Text(
                                "La lista se actualiza al escanear.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(found, key = { it.address }) { device ->
                            OutlinedButton(
                                onClick = { viewModel.connectHr(device) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.fillMaxWidth()) {
                                    Text(
                                        device.name?.ifBlank { null } ?: "Dispositivo sin nombre",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "${device.address} · ${device.rssi} dBm",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.startHrScan() },
                        modifier = Modifier.weight(1f)
                    ) { Text("Buscar HR") }
                    OutlinedButton(
                        onClick = { viewModel.startHrScanBroad() },
                        modifier = Modifier.weight(1f)
                    ) { Text("Buscar todos") }
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
