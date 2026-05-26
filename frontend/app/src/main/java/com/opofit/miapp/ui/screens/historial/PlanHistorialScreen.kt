package com.opofit.miapp.ui.screens.historial

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Card
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
import com.opofit.miapp.ui.components.MetricBadge
import com.opofit.miapp.ui.viewmodels.HistorialAvanzadoViewModel
import com.opofit.miapp.utils.DateFormatUtil

private val NOMBRES_DIA = listOf("", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanHistorialScreen(
    idPlan: Int,
    onNavigateBack: () -> Unit,
    onOpenSesion: (Int) -> Unit,
    viewModel: HistorialAvanzadoViewModel = viewModel()
) {
    LaunchedEffect(idPlan) { viewModel.cargarHistorialPlan(idPlan) }
    val hist by viewModel.historialPlan.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Historial del plan", fontWeight = FontWeight.Bold)
                        hist?.plan?.nombre?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
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
        if (hist == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("Cargando…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }
        val h = hist!!
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricBadge("Sesiones", "${h.totalSesiones}", Modifier.weight(1f))
                    h.plan?.dias_por_semana?.let {
                        MetricBadge("Días/semana", "$it", Modifier.weight(1f))
                    }
                    h.plan?.nivel?.let {
                        MetricBadge("Nivel", it, Modifier.weight(1f))
                    }
                }
            }
            if (h.sesiones.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "Aún no has registrado sesiones de este plan.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(h.sesiones, key = { it.id }) { s ->
                    Card(modifier = Modifier.fillMaxWidth(), onClick = { onOpenSesion(s.id) }) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Filled.FitnessCenter, null, tint = MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f)) {
                                Text(
                                    s.fechaEntreno?.let { DateFormatUtil.formatearFechaHora(it) } ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    s.tituloSesion ?: "Sesión",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2
                                )
                                val mins = (s.duracionSeg ?: 0) / 60
                                Text(
                                    buildString {
                                        s.diaSemana?.let {
                                            append(NOMBRES_DIA.getOrNull(it) ?: "")
                                            append(" · ")
                                        }
                                        s.enfoque?.let { append(it).append(" · ") }
                                        append("$mins min")
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
