package com.opofit.miapp.ui.screens.historial

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.opofit.miapp.data.api.RetrofitClient
import com.opofit.miapp.data.local.TokenManager
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.opofit.miapp.ui.components.ElevatedCard
import com.opofit.miapp.ui.theme.AccentOrange
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opofit.miapp.data.responsemodels.EjercicioSesion
import com.opofit.miapp.gps.util.GpsMetrics
import com.opofit.miapp.ui.components.MetricBadge
import com.opofit.miapp.ui.viewmodels.HistorialAvanzadoViewModel
import com.opofit.miapp.utils.DateFormatUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SesionDetalleScreen(
    sesionId: Int,
    onNavigateBack: () -> Unit,
    onOpenEjercicio: (Int) -> Unit,
    onOpenPlan: (Int) -> Unit,
    onOpenGpsActividad: (String) -> Unit,
    onNavigateToCompartir: () -> Unit = {},
    viewModel: HistorialAvanzadoViewModel = viewModel()
) {
    LaunchedEffect(sesionId) { viewModel.cargarDetalleSesion(sesionId) }
    val sesion by viewModel.detalle.collectAsState()

    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val scope = rememberCoroutineScope()
    var mostrarDialogoBorrar by remember { mutableStateOf(false) }
    var borrando by remember { mutableStateOf(false) }

    if (mostrarDialogoBorrar) {
        AlertDialog(
            onDismissRequest = { if (!borrando) mostrarDialogoBorrar = false },
            icon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("¿Borrar esta sesión?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Se eliminará del historial de forma permanente. No podrás recuperarla. " +
                    "Las marcas y récords personales que generó SE MANTIENEN.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            borrando = true
                            try {
                                val token = tokenManager.getToken().first().orEmpty()
                                val r = RetrofitClient.progresoApi.borrarSesion("Bearer $token", sesionId)
                                if (r.ok) {
                                    mostrarDialogoBorrar = false
                                    onNavigateBack()
                                }
                            } catch (_: Exception) {
                            } finally {
                                borrando = false
                            }
                        }
                    },
                    enabled = !borrando,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (borrando) "Borrando…" else "Borrar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoBorrar = false }, enabled = !borrando) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sesión", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    // Botón Compartir: prepara el PendingShare y navega al editor.
                    sesion?.let { s ->
                        IconButton(onClick = {
                            val duracionMin = ((s.duracionSeg ?: 0) / 60).coerceAtLeast(1)
                            val titulo = when {
                                s.tipoRutina == "PERS" -> s.nombrePersonalizado ?: "Rutina personal"
                                s.tituloSesion != null -> s.tituloSesion ?: "Entrenamiento"
                                else -> "Entrenamiento"
                            }
                            com.opofit.miapp.gps.service.ShareActivityContext.set(
                                com.opofit.miapp.gps.service.buildPendingShareFromEntreno(
                                    titulo = titulo,
                                    idHistorial = s.id,
                                    duracionMin = duracionMin,
                                    ejerciciosCount = s.ejercicios.size
                                )
                            )
                            onNavigateToCompartir()
                        }) {
                            Icon(Icons.Filled.Share, "Compartir")
                        }
                        IconButton(onClick = { mostrarDialogoBorrar = true }) {
                            Icon(Icons.Filled.Delete, "Borrar sesión")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (sesion == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("Cargando…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }
        val s = sesion!!
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            s.fechaEntreno?.let { DateFormatUtil.formatearFechaHora(it) } ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            when {
                                s.tipoRutina == "PERS" -> s.nombrePersonalizado ?: "Rutina personal"
                                s.tituloSesion != null -> s.tituloSesion
                                s.enfoque != null -> "Entreno ${s.enfoque.lowercase()}"
                                else -> "Entreno"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        s.planNombre?.let { plan ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Plan: $plan",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (s.idPlan != null) {
                                    IconButton(onClick = { onOpenPlan(s.idPlan) }) {
                                        Icon(Icons.AutoMirrored.Filled.TrendingUp, "Ver plan")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricBadge("Duración", "${(s.duracionSeg ?: 0) / 60} min", Modifier.weight(1f))
                    MetricBadge("Ejercicios", "${s.ejercicios.size}", Modifier.weight(1f))
                    MetricBadge(
                        "PRs",
                        s.ejercicios.count { it.esPr }.toString(),
                        Modifier.weight(1f)
                    )
                }
            }
            s.gpsActividad?.let { gps ->
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { s.gpsActividadUuid?.let(onOpenGpsActividad) }
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Actividad GPS vinculada",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "${GpsMetrics.formatDistance(gps.distanceM)} · ${GpsMetrics.formatDuration(gps.durationSec)} · ${GpsMetrics.formatPace(gps.avgPaceSecPerKm)}/km",
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Toca para ver mapa, splits y gráficas",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            item {
                Text(
                    "Ejercicios realizados",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            items(s.ejercicios, key = { it.idEjercicio }) { ej ->
                EjercicioRealizadoRow(ej, onClick = { onOpenEjercicio(ej.idEjercicio) })
            }
        }
    }
}

@Composable
private fun EjercicioRealizadoRow(ej: EjercicioSesion, onClick: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Filled.Bolt, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(ej.nombre, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                ej.categoria?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "%.2f".format(ej.valor),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                ej.delta?.let { d ->
                    val up = d >= 0
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (up) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                            null,
                            modifier = Modifier.size(14.dp),
                            tint = if (up) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Text(
                            "${if (up) "+" else ""}%.2f".format(d),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (up) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }
                if (ej.esPr) {
                    Box(
                        Modifier
                            .background(
                                AccentOrange.copy(alpha = 0.12f),
                                RoundedCornerShape(50)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.EmojiEvents,
                                null,
                                modifier = Modifier.size(12.dp),
                                tint = AccentOrange
                            )
                            Text(
                                " PR",
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentOrange
                            )
                        }
                    }
                }
            }
        }
    }
}

