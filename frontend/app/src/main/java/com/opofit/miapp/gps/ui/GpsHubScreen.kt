package com.opofit.miapp.gps.ui

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opofit.miapp.gps.model.ActivitySummary
import com.opofit.miapp.gps.model.ActivityType
import com.opofit.miapp.gps.util.GpsMetrics
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpsHubScreen(
    onNavigateBack: () -> Unit,
    onStartRecording: () -> Unit,
    onOpenActivity: (String) -> Unit,
    viewModel: GpsViewModel = viewModel()
) {
    val selectedType by viewModel.selectedType.collectAsState()
    val history by viewModel.history.collectAsState()
    val tracking by viewModel.tracking.collectAsState()
    val context = LocalContext.current

    var showPermDialog by remember { mutableStateOf(false) }

    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        val ok = granted[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (ok) onStartRecording() else showPermDialog = true
    }

    fun startWithPermission() {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED) {
            onStartRecording()
        } else {
            permLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            )
        }
    }

    LaunchedEffect(Unit) { viewModel.loadHistory() }

    if (showPermDialog) {
        AlertDialog(
            onDismissRequest = { showPermDialog = false },
            confirmButton = {
                Button(onClick = { showPermDialog = false }) { Text("Entendido") }
            },
            title = { Text("Permiso necesario") },
            text = {
                Text(
                    "Para registrar tu ruta necesitamos acceso al GPS. " +
                        "Concede el permiso de Ubicación desde los Ajustes del sistema."
                )
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
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
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
                            "Ritmo, velocidad, altitud y trazado en el mapa.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TypeChip(
                                ActivityType.RUN, Icons.Filled.DirectionsRun,
                                selected = selectedType == ActivityType.RUN
                            ) { viewModel.selectType(ActivityType.RUN) }
                            TypeChip(
                                ActivityType.WALK, Icons.Filled.DirectionsWalk,
                                selected = selectedType == ActivityType.WALK
                            ) { viewModel.selectType(ActivityType.WALK) }
                            TypeChip(
                                ActivityType.BIKE, Icons.Filled.DirectionsBike,
                                selected = selectedType == ActivityType.BIKE
                            ) { viewModel.selectType(ActivityType.BIKE) }
                        }
                        if (tracking.active) {
                            OutlinedButton(
                                onClick = onStartRecording,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.PlayArrow, null, Modifier.size(20.dp))
                                Spacer(Modifier.padding(start = 8.dp))
                                Text("Continuar actividad en curso")
                            }
                        } else {
                            Button(
                                onClick = { startWithPermission() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.PlayArrow, null, Modifier.size(20.dp))
                                Spacer(Modifier.padding(start = 8.dp))
                                Text("Iniciar ${selectedType.display.lowercase()}")
                            }
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
    val date = SimpleDateFormat("d MMM · HH:mm", Locale("es", "ES")).format(Date(activity.startedAtMs))
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
                if (activity.elevationGainM > 0) {
                    Text(
                        "Desnivel +${activity.elevationGainM.toInt()} m",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
