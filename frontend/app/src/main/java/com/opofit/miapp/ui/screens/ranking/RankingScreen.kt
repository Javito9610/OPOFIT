package com.opofit.miapp.ui.screens.ranking

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opofit.miapp.data.api.RetrofitClient
import com.opofit.miapp.data.local.TokenManager
import com.opofit.miapp.data.responsemodels.RankingEntry
import com.opofit.miapp.data.responsemodels.TogglePerfilPublicoRequest
import com.opofit.miapp.ui.viewmodels.AuthViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingScreen(
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit
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

    fun cargar() {
        scope.launch {
            loading = true
            error = ""
            try {
                val token = tokenManager.getToken().first() ?: ""
                val resp = RetrofitClient.rankingApi.getRanking("Bearer $token", oposicionId)
                ranking = if (resp.ok) resp.data.orEmpty() else emptyList()
            } catch (e: Exception) {
                error = e.message ?: "Error"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(oposicionId) { cargar() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ranking") },
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
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Aparecer en el ranking", style = MaterialTheme.typography.bodyLarge)
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
            Text(
                "Solo se muestran aspirantes con perfil público activado.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            when {
                loading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                error.isNotBlank() -> Text(error, color = MaterialTheme.colorScheme.error)
                ranking.isEmpty() -> Text("Aún no hay datos públicos en tu oposición. Activa tu perfil público para participar.")
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ranking) { entry ->
                        Card(Modifier.fillMaxWidth()) {
                            Row(
                                Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("#${entry.posicion}", fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(entry.nombre, fontWeight = FontWeight.SemiBold)
                                    Text(entry.nombrePrueba, style = MaterialTheme.typography.bodySmall)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("${entry.valor} ${entry.unidad}")
                                    Text("Nota ${entry.nota}", color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
