package com.opofit.miapp.ui.screens.comunidad

import com.opofit.miapp.ui.components.ElevatedCard
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.opofit.miapp.data.responsemodels.UsuarioCerca
import com.opofit.miapp.ui.components.EmptyState
import com.opofit.miapp.ui.components.ProfileAvatar
import kotlinx.coroutines.tasks.await

@Composable
fun ComunidadCercaTab(
    usuarios: List<UsuarioCerca>,
    visible: Boolean,
    onToggleVisible: (Boolean) -> Unit,
    onBuscar: (Double, Double) -> Unit
) {
    val context = LocalContext.current
    var buscando by remember { mutableStateOf(false) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) {
            buscando = true
        }
    }

    suspend fun ubicacionYBuscar() {
        try {
            val fused = LocationServices.getFusedLocationProviderClient(context)
            val loc = fused.lastLocation.await()
            if (loc != null) {
                onBuscar(loc.latitude, loc.longitude)
            }
        } catch (_: Exception) { }
        buscando = false
    }

    LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Gente cerca", fontWeight = FontWeight.Bold)
            Text(
                "Encuentra opositores o personas que usan OpoFit para entrenar cerca de ti. Solo ves a quien activa la visibilidad.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(14.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Mostrarme a otros", fontWeight = FontWeight.Medium)
                        Text("Puedes desactivarlo cuando quieras", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = visible, onCheckedChange = onToggleVisible)
                }
            }
        }
        item {
            Button(
                onClick = {
                    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    if (fine == PackageManager.PERMISSION_GRANTED) {
                        buscando = true
                    } else {
                        permLauncher.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (buscando) "Buscando…" else "Buscar gente cerca") }
        }
        if (usuarios.isEmpty()) {
            item {
                EmptyState(
                    "📍",
                    "Nadie cerca por ahora",
                    "Activa tu visibilidad y vuelve a buscar. En zonas con más usuarios verás opositores y personas fitness."
                )
            }
        }
        items(usuarios) { u ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProfileAvatar(u.nombre, sizeDp = 44, avatarUrl = u.avatar_url)
                    Column(Modifier.weight(1f)) {
                        Text(u.nombre, fontWeight = FontWeight.SemiBold)
                        val tipo = when (u.modo_uso?.uppercase()) {
                            "FITNESS" -> "App de entrenamiento"
                            else -> u.oposicion_nombre ?: "Opositor"
                        }
                        Text(tipo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        Text(
                            "${"%.1f".format(u.distancia_m / 1000)} km",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }

    if (buscando) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            ubicacionYBuscar()
        }
    }
}
