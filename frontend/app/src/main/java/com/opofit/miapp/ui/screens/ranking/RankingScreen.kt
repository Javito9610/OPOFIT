package com.opofit.miapp.ui.screens.ranking

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opofit.miapp.data.api.RetrofitClient
import com.opofit.miapp.data.local.TokenManager
import com.opofit.miapp.data.responsemodels.RankingDetallePrueba
import com.opofit.miapp.data.responsemodels.RankingEntry
import com.opofit.miapp.data.responsemodels.TogglePerfilPublicoRequest
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
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Solo aspirantes de tu misma oposición con perfil público.",
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
                loading -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                error.isNotBlank() -> Text(error, color = MaterialTheme.colorScheme.error)
                ranking.isEmpty() -> Text(
                    "Aún no hay clasificación. Activa tu perfil público y registra marcas en las pruebas oficiales.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(ranking) { entry ->
                        RankingCard(entry, onClick = { abrirDetalle(entry) })
                    }
                }
            }
            if (cargandoDetalle) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Surface(color = medalColor, modifier = Modifier.fillMaxSize()) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "#${entry.posicion}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "${entry.pruebasCompletadas} pruebas registradas",
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
