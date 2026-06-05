package com.opofit.miapp.ui.screens.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opofit.miapp.data.api.RetrofitClient
import com.opofit.miapp.data.local.TokenManager
import com.opofit.miapp.data.responsemodels.SegmentoItem
import com.opofit.miapp.data.responsemodels.SegmentoRankingEntry
import com.opofit.miapp.ui.components.ProfileAvatar
import com.opofit.miapp.gps.util.GpsMetrics
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentosScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val scope = rememberCoroutineScope()
    var segmentos by remember { mutableStateOf<List<SegmentoItem>>(emptyList()) }
    var selIdx by remember { mutableIntStateOf(0) }
    var ranking by remember { mutableStateOf<List<SegmentoRankingEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    fun cargarRanking(id: Int) {
        scope.launch {
            try {
                val token = tokenManager.getToken().first() ?: ""
                val r = RetrofitClient.segmentosApi.ranking("Bearer $token", id)
                ranking = if (r.ok) r.data?.ranking.orEmpty() else emptyList()
            } catch (_: Exception) {
                ranking = emptyList()
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            val token = tokenManager.getToken().first() ?: ""
            val r = RetrofitClient.segmentosApi.listar("Bearer $token")
            segmentos = if (r.ok) r.data.orEmpty() else emptyList()
            if (segmentos.isNotEmpty()) cargarRanking(segmentos[0].id_segmento)
        } finally {
            loading = false
        }
    }

    LaunchedEffect(selIdx, segmentos) {
        segmentos.getOrNull(selIdx)?.let { cargarRanking(it.id_segmento) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Segmentos y récords") },
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
    ) { pad ->
        if (loading) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(pad).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        "Virtuales: mejores tiempos por distancia. GPS: tramos geográficos (inicio→fin) detectados al grabar tu ruta.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        segmentos.forEachIndexed { i, s ->
                            FilterChip(
                                selected = selIdx == i,
                                onClick = { selIdx = i },
                                label = {
                                    val badge = if (s.tipo == "GPS") "📍" else "⏱"
                                    Text("$badge ${s.nombre}", maxLines = 1)
                                }
                            )
                        }
                    }
                }
                if (ranking.isEmpty()) {
                    item {
                        Text(
                            "Aún no hay tiempos en este segmento. ¡Sé el primero!",
                            modifier = Modifier.padding(vertical = 24.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(ranking) { e ->
                        Card(Modifier.fillMaxWidth()) {
                            Row(
                                Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "#${e.posicion}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (e.posicion <= 3) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                ProfileAvatar(e.usuarioNombre ?: "?", sizeDp = 40, avatarUrl = e.avatarUrl)
                                Column(Modifier.weight(1f)) {
                                    Text(e.usuarioNombre ?: "Usuario", fontWeight = FontWeight.SemiBold)
                                    Text(
                                        GpsMetrics.formatDuration(e.duracionSec.toInt()),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (e.esRecord) Text("👑", style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}
