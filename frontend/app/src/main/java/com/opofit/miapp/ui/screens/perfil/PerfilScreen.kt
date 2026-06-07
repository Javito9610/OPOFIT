package com.opofit.miapp.ui.screens.perfil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import com.opofit.miapp.ui.components.ElevatedCard
import com.opofit.miapp.ui.components.SectionHeader
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opofit.miapp.ui.components.PostFeedCard
import com.opofit.miapp.ui.components.ProfileAvatar
import com.opofit.miapp.ui.components.StatCard
import com.opofit.miapp.data.responsemodels.ActividadPost
import com.opofit.miapp.data.api.RetrofitClient
import com.opofit.miapp.data.responsemodels.LogrosData
import com.opofit.miapp.data.responsemodels.PerfilUsuarioData
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Terrain
import com.opofit.miapp.ui.viewmodels.AuthViewModel
import com.opofit.miapp.ui.viewmodels.PerfilViewModel
import com.opofit.miapp.ui.viewmodels.RutinasViewModel
import com.opofit.miapp.data.local.TokenManager
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AssistChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.platform.LocalContext
import com.opofit.miapp.utils.Units
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlin.math.pow

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PerfilScreen(
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEditarPerfil: () -> Unit,
    onNavigateToAjustes: () -> Unit = {},
    onNavigateToComunidad: () -> Unit = {},
    onOpenPost: (Int) -> Unit = {},
    perfilViewModel: PerfilViewModel = viewModel(),
    rutinasViewModel: RutinasViewModel = viewModel()
) {
    val authState by authViewModel.uiState.collectAsState()
    val perfilState by perfilViewModel.uiState.collectAsState()
    val rutinasState by rutinasViewModel.uiState.collectAsState()
    var misPosts by remember { mutableStateOf<List<ActividadPost>>(emptyList()) }
    var logros by remember { mutableStateOf<LogrosData?>(null) }

    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    var unitPeso by remember { mutableStateOf("kg") }
    var unitDist by remember { mutableStateOf("km") }
    LaunchedEffect(Unit) {
        tokenManager.getUnitPeso().collectLatest { u ->
            if (!u.isNullOrBlank()) unitPeso = u
        }
    }
    LaunchedEffect(Unit) {
        tokenManager.getUnitDistancia().collectLatest { u ->
            if (!u.isNullOrBlank()) unitDist = u
        }
    }

    val userId = authState.userId ?: 0
    val oposicionId = authState.oposicionId
    val genero = authState.genero ?: "HOMBRE"
    var perfilData by remember { mutableStateOf<PerfilUsuarioData?>(null) }
    val esFitness = perfilData?.modoUso?.equals("FITNESS", true) == true

    LaunchedEffect(userId) {
        if (userId > 0) {
            if (oposicionId != null) {
                perfilViewModel.cargarMarcasUsuario(userId, oposicionId)
                rutinasViewModel.cargarRutina(userId, oposicionId)
            }
            try {
                val token = tokenManager.getToken().first().orEmpty()
                if (token.isNotBlank()) {
                    perfilData = RetrofitClient.usuarioApi.obtenerPerfil("Bearer $token").data
                    val posts = RetrofitClient.postsApi.porUsuario("Bearer $token", userId)
                    misPosts = if (posts.ok) posts.data.orEmpty() else emptyList()
                    val lr = RetrofitClient.logrosApi.misLogros("Bearer $token")
                    logros = if (lr.ok) lr.data else null
                }
            } catch (_: Exception) { }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        topBar = {
            TopAppBar(
                title = { Text("Mi Perfil") },
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
    ) { innerPadding ->
        if (perfilState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 16.dp, bottom = 8.dp)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                ProfileAvatar(
                                    perfilData?.nombre ?: authState.userName ?: "Usuario",
                                    sizeDp = 64,
                                    avatarUrl = perfilData?.avatarUrl
                                )
                                Column {
                                    Text(
                                        text = perfilData?.nombre ?: authState.userName ?: "Usuario",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = perfilData?.email ?: authState.userEmail ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    AssistChip(
                                        onClick = {},
                                        label = {
                                            Text(
                                                if (esFitness) "Modo fitness" else (perfilData?.oposicionNombre ?: "Opositor")
                                            )
                                        }
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = onNavigateToEditarPerfil, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Filled.Edit, null, Modifier.size(16.dp))
                                    Text(" Editar", maxLines = 1)
                                }
                                OutlinedButton(onClick = onNavigateToAjustes, modifier = Modifier.weight(1f)) {
                                    Text("Ajustes", maxLines = 1)
                                }
                            }
                            OutlinedButton(onClick = onNavigateToComunidad, modifier = Modifier.fillMaxWidth()) {
                                Text("Comunidad y grupos")
                            }

                            val peso = authState.peso
                            val altura = authState.altura
                            val imc = authState.imc ?: run {
                                val p = peso
                                val a = altura
                                if (p != null && a != null && a > 0) {
                                    val alturaM = a / 100.0
                                    p / alturaM.pow(2)
                                } else null
                            }

                            if (peso != null || altura != null || imc != null) {
                                val pesoShown = if (peso != null && unitPeso == "lb") Units.kgToLb(peso) else peso
                                Spacer(modifier = Modifier.height(8.dp))
                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Peso", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(pesoShown?.let { String.format("%.1f %s", it, unitPeso) } ?: "-", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Altura", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            altura?.let { Units.formatAltura(it, unitDist) } ?: "-",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("IMC", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(imc?.let { String.format("%.2f", it) } ?: "-", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                    }
                }

                if (!esFitness && rutinasState.notaActual.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatCard(
                                label = "Nota media",
                                value = rutinasState.notaActual,
                                supporting = "oficial /10",
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                label = "Nivel",
                                value = rutinasState.nivelAsignado,
                                supporting = "rutina asignada",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                logros?.stats?.let { stats ->
                    item {
                        SectionHeader(title = "Tu actividad", subtitle = "Estilo Strava — totales")
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatCard(
                                label = "Sesiones",
                                value = "${stats.sesiones}",
                                supporting = "totales",
                                icon = Icons.AutoMirrored.Filled.DirectionsRun,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                label = "Distancia",
                                value = String.format("%.1f", stats.distanciaKm),
                                supporting = if (unitDist == "mi") "mi equiv." else "km GPS",
                                icon = Icons.Filled.Terrain,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatCard(
                                label = "Racha máx.",
                                value = "${logros?.rachas?.maxima ?: 0}",
                                supporting = "días seguidos",
                                icon = Icons.Filled.LocalFireDepartment,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                label = "Desnivel",
                                value = "${stats.desnivelM.toInt()}",
                                supporting = "metros",
                                icon = Icons.Filled.EmojiEvents,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                logros?.medallas?.takeIf { it.isNotEmpty() }?.let { medallas ->
                    item {
                        SectionHeader(
                            title = "Logros",
                            subtitle = "${logros?.medallasDesbloqueadas ?: 0} de ${logros?.medallasTotales ?: medallas.size} desbloqueados"
                        )
                        Spacer(Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            medallas.filter { it.desbloqueada }.take(8).forEach { m ->
                                AssistChip(
                                    onClick = {},
                                    label = { Text(m.nombre) },
                                    enabled = false
                                )
                            }
                            if (medallas.none { it.desbloqueada }) {
                                Text(
                                    "Entrena y comparte actividades para desbloquear medallas.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (misPosts.isNotEmpty()) {
                    item {
                        Text(
                            text = "Mis publicaciones",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(misPosts, key = { it.idPost }) { post ->
                        PostFeedCard(
                            post = post,
                            onClick = { onOpenPost(post.idPost) }
                        )
                    }
                }

                if (perfilState.marcasUsuario.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Mis marcas",
                            subtitle = "Tus mejores registros oficiales"
                        )
                    }
                    items(perfilState.marcasUsuario) { marca ->
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = Units.nombreConEquivalenciaDistancia(marca.nombre_prueba, unitDist),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Divider(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(
                                        text = "Marca: ${Units.formatMarcaDisplay(marca.valord_record, marca.unidad, unitDist)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = marca.fecha_logro?.take(10) ?: "-",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                if (perfilState.error.isNotEmpty()) {
                    item {
                        Text(
                            text = perfilState.error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

            }
            Button(
                onClick = onNavigateToEditarPerfil,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("  Editar perfil completo", maxLines = 1)
            }
            }
        }
    }
}
