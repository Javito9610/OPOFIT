package com.opofit.miapp.ui.screens.premium

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opofit.miapp.data.api.RetrofitClient
import com.opofit.miapp.data.local.TokenManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val scope = rememberCoroutineScope()

    var esPremium by remember { mutableStateOf(false) }
    var premiumHasta by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var msg by remember { mutableStateOf("") }

    fun cargarEstado() {
        scope.launch {
            loading = true
            try {
                val token = tokenManager.getToken().first() ?: ""
                val resp = RetrofitClient.premiumApi.estado("Bearer $token")
                if (resp.ok && resp.data != null) {
                    esPremium = resp.data.esPremium
                    premiumHasta = resp.data.premiumHasta
                }
            } catch (e: Exception) {
                msg = e.message ?: "Error"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) { cargarEstado() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OpoFit Premium") },
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
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(48.dp))
            Text("Desbloquea todo OpoFit", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            val beneficios = listOf(
                "Rutinas INTERMEDIO y AVANZADO (gratis: nivel BÁSICO en todas las oposiciones)",
                "Baremos completos en cada prueba",
                "Historial de simulacros guardado",
                "Ranking completo entre aspirantes",
                "Bomberos, Policía Local, Penitenciarias, Ejército y más"
            )
            Text(
                "Todas las oposiciones incluyen contenido gratuito: pruebas, simulacro, rutinas básicas y baremos parciales.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            beneficios.forEach { Text("✓ $it") }
            if (esPremium) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Text(
                        "Premium activo${premiumHasta?.let { " hasta $it" } ?: ""}",
                        Modifier.padding(16.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text(
                    "En producción conectarás Google Play Billing. En desarrollo puedes activar una prueba de 30 días.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val token = tokenManager.getToken().first() ?: ""
                                val resp = RetrofitClient.premiumApi.activarPrueba("Bearer $token")
                                msg = resp.msg ?: if (resp.ok) "¡Premium activado!" else "No disponible"
                                if (resp.ok) cargarEstado()
                            } catch (e: Exception) {
                                msg = e.message ?: "Error (activa PREMIUM_DEV_MODE en el servidor)"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Activar Premium (prueba 30 días)")
                }
            }
            if (msg.isNotBlank()) Text(msg, color = MaterialTheme.colorScheme.primary)
            if (loading) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        }
    }
}
