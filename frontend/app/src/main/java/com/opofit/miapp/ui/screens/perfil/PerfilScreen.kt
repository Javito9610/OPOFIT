package com.opofit.miapp.ui.screens.perfil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.opofit.miapp.ui.viewmodels.AuthViewModel
import com.opofit.miapp.ui.viewmodels.PerfilViewModel
import com.opofit.miapp.ui.viewmodels.RutinasViewModel
import com.opofit.miapp.data.local.TokenManager
import androidx.compose.ui.platform.LocalContext
import com.opofit.miapp.utils.Units
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.pow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEditarPerfil: () -> Unit,
    perfilViewModel: PerfilViewModel = viewModel(),
    rutinasViewModel: RutinasViewModel = viewModel()
) {
    val authState by authViewModel.uiState.collectAsState()
    val perfilState by perfilViewModel.uiState.collectAsState()
    val rutinasState by rutinasViewModel.uiState.collectAsState()

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
    val oposicionId = authState.oposicionId ?: 1
    val genero = authState.genero ?: "HOMBRE"

    LaunchedEffect(userId) {
        if (userId > 0) {
            perfilViewModel.cargarMarcasUsuario(userId, oposicionId)
            rutinasViewModel.cargarRutina(userId, oposicionId)
        }
    }

    Scaffold(
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = authState.userName ?: "Usuario",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = authState.userEmail ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )

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
                                Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Peso", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                                        Text(pesoShown?.let { String.format("%.1f %s", it, unitPeso) } ?: "-", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Altura", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                                        Text(altura?.let { String.format("%.0f cm", it) } ?: "-", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("IMC", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                                        Text(imc?.let { String.format("%.2f", it) } ?: "-", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                }
                            }
                        }
                    }
                }

                if (rutinasState.notaActual.isNotEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Nota actual", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(rutinasState.notaActual, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Nivel", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${rutinasState.nivelAsignado}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }

                if (perfilState.marcasUsuario.isNotEmpty()) {
                    item {
                        Text(
                            text = "Mis Marcas",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(perfilState.marcasUsuario) { marca ->
                        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(1.dp)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = Units.nombreConEquivalenciaDistancia(marca.nombre_prueba, unitDist),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(
                                        text = "Marca: ${marca.valord_record ?: "-"}${if (!marca.unidad.isNullOrBlank()) " ${marca.unidad}" else ""}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Fecha: ${marca.fecha_logro?.take(10) ?: "-"}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.secondary
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

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onNavigateToEditarPerfil,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("✏️ Editar Perfil")
                    }
                }
            }
        }
    }
}
