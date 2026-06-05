package com.opofit.miapp.gps.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.opofit.miapp.data.api.RetrofitClient
import com.opofit.miapp.data.local.TokenManager
import com.opofit.miapp.data.responsemodels.LugarEntreno
import com.opofit.miapp.data.responsemodels.RutaEntreno
import com.opofit.miapp.data.responsemodels.RoutePointDto
import com.opofit.miapp.data.responsemodels.RutaPersonalizadaBody
import com.opofit.miapp.gps.service.PlannedRoute
import com.opofit.miapp.gps.service.RoutePoint
import com.opofit.miapp.gps.service.RoutePreferences
import com.opofit.miapp.gps.util.RouteGpxExport
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapaEntrenoScreen(
    onNavigateBack: () -> Unit,
    onUsarRutaEnGps: () -> Unit,
    distanciaObjetivoKm: Double? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val tokenManager = remember { TokenManager(context) }
    var tab by remember { mutableIntStateOf(0) }
    var lat by remember { mutableDoubleStateOf(40.4168) }
    var lng by remember { mutableDoubleStateOf(-3.7038) }
    var tipoLugar by remember { mutableStateOf("GYM") }
    var lugares by remember { mutableStateOf<List<LugarEntreno>>(emptyList()) }
    var ruta by remember { mutableStateOf<RutaEntreno?>(null) }
    var distKm by remember { mutableDoubleStateOf(distanciaObjetivoKm ?: 5.0) }
    var loading by remember { mutableStateOf(false) }
    var modoPersonalizado by remember { mutableStateOf(false) }
    val waypoints = remember { mutableStateListOf<LatLng>() }
    val camera = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(lat, lng), 14f)
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) {
            scope.launch { cargarUbicacion(context) { la, ln ->
                lat = la; lng = ln
                camera.position = CameraPosition.fromLatLngZoom(LatLng(la, ln), 14f)
            } }
        }
    }

    suspend fun auth(): String {
        val t = tokenManager.getToken().first() ?: ""
        return "Bearer $t"
    }

    fun cargarLugares() {
        scope.launch {
            loading = true
            try {
                val resp = RetrofitClient.mapasApi.lugares(auth(), lat, lng, tipoLugar)
                lugares = resp.data ?: emptyList()
            } catch (e: Exception) {
                snackbar.showSnackbar("No se pudieron cargar lugares: ${e.message}")
            } finally {
                loading = false
            }
        }
    }

    fun generarRuta() {
        scope.launch {
            loading = true
            try {
                val resp = if (modoPersonalizado && waypoints.size >= 2) {
                    RetrofitClient.mapasApi.rutaPersonalizada(
                        auth(),
                        RutaPersonalizadaBody(
                            waypoints = waypoints.map { RoutePointDto(it.latitude, it.longitude) },
                            nombre = "Mi ruta"
                        )
                    )
                } else {
                    RetrofitClient.mapasApi.rutaSugerida(auth(), lat, lng, distKm)
                }
                ruta = resp.data
                tab = 1
            } catch (e: Exception) {
                snackbar.showSnackbar("Error generando ruta: ${e.message}")
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED) {
            cargarUbicacion(context) { la, ln ->
                lat = la; lng = ln
                camera.position = CameraPosition.fromLatLngZoom(LatLng(la, ln), 14f)
            }
        } else {
            permLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
        cargarLugares()
    }

    LaunchedEffect(tipoLugar) { cargarLugares() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Mapa de entreno") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            cargarUbicacion(context) { la, ln ->
                                lat = la; lng = ln
                                camera.position = CameraPosition.fromLatLngZoom(LatLng(la, ln), 14f)
                            }
                        }
                    }) {
                        Icon(Icons.Filled.MyLocation, "Mi ubicación")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Lugares") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Rutas") })
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = camera,
                    properties = MapProperties(isMyLocationEnabled = true),
                    uiSettings = MapUiSettings(myLocationButtonEnabled = false),
                    onMapClick = { click ->
                        if (tab == 1 && modoPersonalizado) {
                            waypoints.add(click)
                        }
                    }
                ) {
                    Marker(state = MarkerState(LatLng(lat, lng)), title = "Tú")
                    if (tab == 0) {
                        lugares.forEach { l ->
                            Marker(
                                state = MarkerState(LatLng(l.lat, l.lng)),
                                title = l.nombre,
                                snippet = l.direccion
                            )
                        }
                    }
                    ruta?.puntos?.takeIf { it.size >= 2 }?.let { pts ->
                        val latLngs = pts.map { LatLng(it.lat, it.lng) }
                        Polyline(points = latLngs, color = Color(0xFF2E7D32), width = 10f)
                    }
                    waypoints.forEachIndexed { i, p ->
                        Marker(state = MarkerState(p), title = "Punto ${i + 1}")
                    }
                }
                if (loading) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
            }
            Card(Modifier.fillMaxWidth().padding(12.dp)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (tab == 0) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listOf("GYM", "CROSSFIT", "PISTA", "CALISTENIA", "PARQUE")) { t ->
                                FilterChip(
                                    selected = tipoLugar == t,
                                    onClick = { tipoLugar = t },
                                    label = { Text(t) }
                                )
                            }
                        }
                        if (lugares.isNotEmpty()) {
                            Text(
                                "${lugares.size} lugares cerca",
                                style = MaterialTheme.typography.labelMedium
                            )
                            lugares.take(3).forEach { l ->
                                Text(
                                    "• ${l.nombre} (${(l.distanciaM / 1000).let { "%.1f".format(it) }} km)",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    } else {
                        Text("Distancia objetivo: ${"%.1f".format(distKm)} km", fontWeight = FontWeight.Bold)
                        Slider(
                            value = distKm.toFloat(),
                            onValueChange = { distKm = it.toDouble() },
                            valueRange = 1f..15f,
                            steps = 13,
                            enabled = !modoPersonalizado
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = !modoPersonalizado,
                                onClick = { modoPersonalizado = false; waypoints.clear() },
                                label = { Text("Sugerida") }
                            )
                            FilterChip(
                                selected = modoPersonalizado,
                                onClick = { modoPersonalizado = true; ruta = null },
                                label = { Text("Elegir la mía") }
                            )
                        }
                        if (modoPersonalizado) {
                            Text(
                                "Toca el mapa para añadir puntos (${waypoints.size})",
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (waypoints.isNotEmpty()) {
                                OutlinedButton(onClick = { waypoints.clear() }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Limpiar puntos")
                                }
                            }
                        }
                        OutlinedButton(onClick = { generarRuta() }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.Route, null, Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text(if (modoPersonalizado) "Crear ruta" else "Generar ruta sugerida")
                        }
                        ruta?.let { r ->
                            Text(
                                "${r.nombre} · ${"%.2f".format(r.distanciaKm)} km",
                                fontWeight = FontWeight.Medium
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        val planned = PlannedRoute(
                                            id = r.id,
                                            nombre = r.nombre,
                                            distanciaKm = r.distanciaKm,
                                            puntos = r.puntos.map { RoutePoint(it.lat, it.lng) },
                                            origen = r.origen
                                        )
                                        val intent = RouteGpxExport.shareIntent(context, planned)
                                        if (intent != null) {
                                            context.startActivity(
                                                android.content.Intent.createChooser(intent, "Exportar GPX al reloj")
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.Download, null, Modifier.size(16.dp))
                                    Spacer(Modifier.size(4.dp))
                                    Text("GPX")
                                }
                                Button(
                                    onClick = {
                                        scope.launch {
                                            val planned = PlannedRoute(
                                                id = r.id,
                                                nombre = r.nombre,
                                                distanciaKm = r.distanciaKm,
                                                puntos = r.puntos.map { RoutePoint(it.lat, it.lng) },
                                                origen = r.origen
                                            )
                                            RoutePreferences.save(context, planned)
                                            snackbar.showSnackbar("Ruta lista para GPS")
                                            onUsarRutaEnGps()
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.PlayArrow, null, Modifier.size(16.dp))
                                    Spacer(Modifier.size(4.dp))
                                    Text("Usar en GPS")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private suspend fun cargarUbicacion(
    context: android.content.Context,
    onResult: (Double, Double) -> Unit
) {
    try {
        val fused = LocationServices.getFusedLocationProviderClient(context)
        val loc = fused.lastLocation.await()
        if (loc != null) onResult(loc.latitude, loc.longitude)
    } catch (_: Exception) { /* ubicación por defecto Madrid */ }
}
