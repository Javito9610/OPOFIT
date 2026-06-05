package com.opofit.miapp.ui.screens.ranking

import com.opofit.miapp.ui.components.ElevatedCard
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.opofit.miapp.data.api.RetrofitClient
import com.opofit.miapp.data.local.TokenManager
import com.opofit.miapp.data.responsemodels.RankingDetallePrueba
import com.opofit.miapp.data.responsemodels.RankingEntry
import com.opofit.miapp.data.responsemodels.TogglePerfilPublicoRequest
import com.opofit.miapp.ui.components.EmptyState
import com.opofit.miapp.ui.components.ErrorState
import com.opofit.miapp.ui.components.ProfileAvatar
import com.opofit.miapp.ui.viewmodels.AuthViewModel
import com.opofit.miapp.utils.ApiErrorParser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingScreen(
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onNavigateComunidad: () -> Unit = {}
) {
    val authState by authViewModel.uiState.collectAsState()
    val oposicionId = authState.oposicionId ?: 1
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val scope = rememberCoroutineScope()

    var ranking by remember { mutableStateOf<List<RankingEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var perfilPublico by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var detalleUsuario by remember { mutableStateOf<Pair<String, List<RankingDetallePrueba>>?>(null) }
    var cargandoDetalle by remember { mutableStateOf(false) }

    fun cargar() {
        scope.launch {
            loading = true
            error = ""
            try {
                val token = tokenManager.getToken().first() ?: ""
                val resp = RetrofitClient.rankingApi.getRanking("Bearer $token", oposicionId)
                ranking = if (resp.ok) resp.data.orEmpty() else emptyList()
            } catch (e: Exception) {
                error = ApiErrorParser.message(e)
            } finally {
                loading = false
            }
        }
    }

    fun abrirDetalle(entry: RankingEntry) {
        scope.launch {
            cargandoDetalle = true
            try {
                val token = tokenManager.getToken().first() ?: ""
                val resp = RetrofitClient.rankingApi.detalleUsuario(
                    "Bearer $token", oposicionId, entry.userId
                )
                if (resp.ok && resp.data != null) {
                    detalleUsuario = resp.data.nombre to (resp.data.pruebas.orEmpty())
                }
            } catch (e: Exception) {
                error = ApiErrorParser.message(e)
            } finally {
                cargandoDetalle = false
            }
        }
    }

    LaunchedEffect(oposicionId) { cargar() }

    if (detalleUsuario != null) {
        AlertDialog(
            onDismissRequest = { detalleUsuario = null },
            title = { Text(detalleUsuario!!.first, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Nota media global",
                        style = MaterialTheme.typography.labelMedium
                    )
                    detalleUsuario!!.second.forEach { p ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(p.nombrePrueba, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${formatValor(p)} ${etiquetaUnidad(p.unidad)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text(
                                "${p.nota}/10",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        HorizontalDivider()
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { detalleUsuario = null }) { Text("Cerrar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ranking de tu oposición") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = loading && ranking.isNotEmpty(),
            onRefresh = { cargar() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                ElevatedCard(
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Clasificación por nota media (estilo leaderboard). Solo perfiles públicos de tu oposición.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Aparecer en el ranking")
                            Switch(
                                checked = perfilPublico,
                                onCheckedChange = { checked ->
                                    perfilPublico = checked
                                    scope.launch {
                                        try {
                                            val token = tokenManager.getToken().first() ?: ""
                                            RetrofitClient.rankingApi.togglePerfilPublico(
                                                "Bearer $token",
                                                TogglePerfilPublicoRequest(checked)
                                            )
                                            cargar()
                                        } catch (_: Exception) { }
                                    }
                                }
                            )
                        }
                        OutlinedButton(onClick = onNavigateComunidad, modifier = Modifier.fillMaxWidth()) {
                            Text("Comunidad: amigos y chat")
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                when {
                    loading && ranking.isEmpty() -> Box(
                        Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                    error.isNotBlank() && ranking.isEmpty() -> ErrorState(error, onRetry = { cargar() })
                    ranking.isEmpty() -> EmptyState(
                        emoji = "🏆",
                        title = "Sin clasificación aún",
                        message = "Activa tu perfil público y registra las 3 marcas oficiales para aparecer y compararte con otros opositores.",
                        modifier = Modifier.weight(1f),
                        actionLabel = "Ir a mi perfil",
                        onAction = onNavigateBack
                    )
                    else -> {
                        val top3 = ranking.take(3)
                        val resto = ranking.drop(3)
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (top3.isNotEmpty()) {
                                item {
                                    PodiumRow(top3)
                                }
                            }
                            items(resto) { entry ->
                                RankingCard(entry, onClick = { abrirDetalle(entry) })
                            }
                        }
                    }
                }
                if (cargandoDetalle) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun PodiumRow(top3: List<RankingEntry>) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            top3.getOrNull(1)?.let { PodiumPlace(it, medal = "🥈", emphasis = false) }
            top3.getOrNull(0)?.let { PodiumPlace(it, medal = "🥇", emphasis = true) }
            top3.getOrNull(2)?.let { PodiumPlace(it, medal = "🥉", emphasis = false) }
        }
    }
}

@Composable
private fun PodiumPlace(entry: RankingEntry, medal: String, emphasis: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Bottom)
    ) {
        Text(medal, style = MaterialTheme.typography.titleLarge)
        ProfileAvatar(entry.nombre, sizeDp = if (emphasis) 48 else 40)
        Text(
            entry.nombre.split(" ").firstOrNull() ?: entry.nombre,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Text(
            String.format("%.1f", entry.notaMedia),
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RankingCard(entry: RankingEntry, onClick: () -> Unit) {
    val medalColor = when (entry.posicion) {
        1 -> MaterialTheme.colorScheme.tertiary
        2 -> MaterialTheme.colorScheme.secondary
        3 -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileAvatar(entry.nombre, sizeDp = 44)
            Spacer(Modifier.width(4.dp))
            Surface(
                color = medalColor,
                shape = CircleShape,
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "${entry.posicion}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    if (entry.totalPruebasOpo != null && entry.totalPruebasOpo > 0) {
                        "${entry.pruebasCompletadas} de ${entry.totalPruebasOpo} pruebas oficiales"
                    } else {
                        "${entry.pruebasCompletadas} pruebas registradas"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Toca para ver desglose",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Media", style = MaterialTheme.typography.labelSmall)
                Text(
                    String.format("%.1f", entry.notaMedia),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text("/ 10", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun formatValor(p: RankingDetallePrueba): String =
    if (p.unidad == "s") String.format("%.2f", p.valor) else p.valor.toInt().toString()

private fun etiquetaUnidad(u: String) = if (u == "s") "s" else "rep"
