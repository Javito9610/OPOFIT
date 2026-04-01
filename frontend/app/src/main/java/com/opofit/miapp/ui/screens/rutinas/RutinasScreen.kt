package com.opofit.miapp.ui.screens.rutinas

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opofit.miapp.data.local.TokenManager
import com.opofit.miapp.ui.viewmodels.AuthViewModel
import com.opofit.miapp.ui.viewmodels.RutinasViewModel
import com.opofit.miapp.utils.Units
import com.opofit.miapp.utils.UrlOpener
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RutinasScreen(
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEntrenamientos: (String?) -> Unit,
    onNavigateToRutinasLibres: () -> Unit,
    onNavigateToEditarPerfil: () -> Unit,
    rutinasViewModel: RutinasViewModel = viewModel()
) {
    val authState by authViewModel.uiState.collectAsState()
    val uiState by rutinasViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    var unitDist by remember { mutableStateOf("km") }
    LaunchedEffect(Unit) {
        tokenManager.getUnitDistancia().collectLatest { u ->
            if (!u.isNullOrBlank()) unitDist = u
        }
    }

    val userId = authState.userId ?: 0
    val oposicionId = authState.oposicionId ?: 1

    LaunchedEffect(userId) {
        if (userId > 0) {
            rutinasViewModel.cargarRutina(userId, oposicionId)
        }
    }

    
    val enfoqueTabs = listOf(
        "💪 Fuerza" to "FUERZA",
        "🏃 Resistencia" to "RESISTENCIA",
        "⚡ Velocidad" to "VELOCIDAD"
    )
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        topBar = {
            TopAppBar(
                title = { Text("Mi Entrenamiento") },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.error.isNotEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = "No se pudo cargar el entrenamiento",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = uiState.error,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Start
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { rutinasViewModel.cargarRutina(userId, oposicionId) }) {
                            Text("Reintentar")
                        }
                    }
                }
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        
                        if (uiState.notaActual.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Nota actual: ${uiState.notaActual}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Nivel asignado: ${uiState.nivelAsignado}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        if (uiState.rutinaCompleta.isEmpty() && !uiState.isLoading) {
                            val faltan = uiState.pruebasFaltantes ?: 0
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(vertical = 24.dp, horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (faltan > 0) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                                            ),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(20.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.FactCheck,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(44.dp),
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Text(
                                                    text = "Marcas pendientes",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    textAlign = TextAlign.Center
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = if (faltan == 1) {
                                                        "Falta registrar una prueba oficial en tu perfil. Al guardarla, calcularemos tu nivel y te mostraremos la rutina recomendada para tu oposición."
                                                    } else {
                                                        "Faltan registrar $faltan pruebas oficiales en tu perfil. Cuando estén completas, podremos asignarte un nivel y personalizar tu entrenamiento."
                                                    },
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                        FilledTonalButton(
                                            onClick = onNavigateToEditarPerfil,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                        ) {
                                            Text("Ir a añadir marcas")
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedButton(
                                            onClick = { rutinasViewModel.cargarRutina(userId, oposicionId) },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Actualizar (si ya las guardé)")
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedButton(
                                            onClick = onNavigateToRutinasLibres,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Ver rutinas libres mientras tanto")
                                        }
                                    } else {
                                        Text(
                                            text = "Aún no podemos generar tu rutina",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Añade tus marcas en Perfil → Editar perfil para que podamos calcular tu nivel.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        FilledTonalButton(
                                            onClick = onNavigateToEditarPerfil,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Ir al perfil")
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedButton(
                                            onClick = onNavigateToRutinasLibres,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Ver rutinas libres (mientras tanto)")
                                        }
                                    }
                                }
                            }
                        } else if (uiState.rutinaCompleta.isNotEmpty()) {
                            val selectedEnfoque = enfoqueTabs[selectedTab].second
                            val filteredBlocks = uiState.rutinaCompleta.filter {
                                it.bloque.equals(selectedEnfoque, ignoreCase = true)
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                ScrollableTabRow(
                                    selectedTabIndex = selectedTab,
                                    edgePadding = 8.dp
                                ) {
                                    enfoqueTabs.forEachIndexed { index, (label, _) ->
                                        Tab(
                                            selected = selectedTab == index,
                                            onClick = { selectedTab = index },
                                            text = { Text(label) }
                                        )
                                    }
                                }
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    if (filteredBlocks.isEmpty()) {
                                        item {
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                                )
                                            ) {
                                                Box(
                                                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "No hay ejercicios de ${selectedEnfoque.lowercase()} disponibles para tu nivel.",
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    items(filteredBlocks) { bloque ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Text(
                                                    text = bloque.bloque,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                bloque.ejercicios.forEachIndexed { index, ejercicio ->
                                                    if (index > 0) Divider(modifier = Modifier.padding(vertical = 4.dp))
                                                    val nombreEj = Units.nombreConEquivalenciaDistancia(
                                                        ejercicio.nombre,
                                                        unitDist
                                                    )
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = nombreEj,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        Text(
                                                            text = "${ejercicio.series}x${ejercicio.repeticiones}",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.secondary
                                                        )
                                                    }
                                                    if (!ejercicio.video_url.isNullOrBlank()) {
                                                        TextButton(
                                                            onClick = {
                                                                UrlOpener.open(context, ejercicio.video_url)
                                                            }
                                                        ) {
                                                            Text("Ver vídeo")
                                                        }
                                                    }
                                                    Text(
                                                        text = "Descanso: ${ejercicio.descanso}s",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { onNavigateToEntrenamientos(selectedEnfoque) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("💪 Comenzar Entrenamiento")
                                    }
                                    OutlinedButton(
                                        onClick = onNavigateToRutinasLibres,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Ver rutinas libres")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
