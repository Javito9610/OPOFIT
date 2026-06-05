package com.opofit.miapp.gps.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.TouchApp
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
import androidx.compose.runtime.collectAsState
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
import com.opofit.miapp.gps.service.EntrenoFlowContext
import com.opofit.miapp.gps.service.PlannedRoute
import com.opofit.miapp.gps.service.RoutePoint
import com.opofit.miapp.gps.service.RoutePreferences
import com.opofit.miapp.gps.util.RouteGpxExport
import com.opofit.miapp.utils.MapaEntrenoNav
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapaEntrenoScreen(
    onNavigateBack: () -> Unit,
    onUsarRutaEnGps: () -> Unit,
    distanciaObjetivoKm: Double? = null,
    modoInicial: String = MapaEntrenoNav.MODO_RUTAS,
    tipoLugarInicial: String = "GYM"
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val tokenManager = remember { TokenManager(context) }
    val esModoLugares = modoInicial == MapaEntrenoNav.MODO_LUGARES
    var tab by remember { mutableIntStateOf(if (esModoLugares) 0 else 1) }
    var lat by remember { mutableDoubleStateOf(40.4168) }
    var lng by remember { mutableDoubleStateOf(-3.7038) }
    var ubicacionLista by remember { mutableStateOf(false) }
    var tipoLugar by remember { mutableStateOf(tipoLugarInicial) }
    var lugares by remember { mutableStateOf<List<LugarEntreno>>(emptyList()) }
    var ruta by remember { mutableStateOf<RutaEntreno?>(null) }
    var distKm by remember { mutableDoubleStateOf(distanciaObjetivoKm ?: 5.0) }
    var variacion by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    val flowCtx by EntrenoFlowContext.state.collectAsState()
    var modoPersonalizado by remember { mutableStateOf(false) }
    val waypoints = remember { mutableStateListOf<LatLng>() }
    val camera = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(lat, lng), 14f)
    }

    val titulo = if (esModoLugares) "Dónde entrenar" else "Ruta de carrera"

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) {
            scope.launch {
                cargarUbicacion(context) { la, ln ->
                    lat = la; lng = ln; ubicacionLista = true
                    camera.position = CameraPosition.fromLatLngZoom(LatLng(la, ln), 14f)
                }
            }
        }
    }

    suspend fun auth(): String {
        val t = tokenManager.getToken().first() ?: ""
        return "Bearer $t"
    }

    fun cargarLugares() {
        if (!ubicacionLista) return
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

    fun generarRuta(onListo: (() -> Unit)? = null) {
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
                    RetrofitClient.mapasApi.rutaSugerida(auth(), lat, lng, distKm, variacion)
                }
                ruta = resp.data
                tab = 1
                onListo?.invoke()
            } catch (e: Exception) {
                snackbar.showSnackbar("Error generando ruta: ${e.message}")
            } finally {
                loading = false
            }
        }
    }

    suspend fun guardarEIniciar(r: RutaEntreno) {
        val planned = PlannedRoute(
            id = r.id,
            nombre = r.nombre,
            distanciaKm = r.distanciaKm,
            puntos = r.puntos.map { RoutePoint(it.lat, it.lng) },
            origen = r.origen
        )
        RoutePreferences.save(context, planned)
        onUsarRutaEnGps()
    }

    fun iniciarCarreraConRuta() {
        scope.launch {
            val actual = ruta
            if (actual != null && actual.puntos.size >= 2) {
                guardarEIniciar(actual)
            } else {
                loading = true
                try {
                    val resp = RetrofitClient.mapasApi.rutaSugerida(auth(), lat, lng, distKm, variacion)
                    val generada = resp.data
                    if (generada != null && generada.puntos.size >= 2) {
                        ruta = generada
                        tab = 1
                        guardarEIniciar(generada)
                    }
                } catch (e: Exception) {
                    snackbar.showSnackbar("Error: ${e.message}")
                } finally {
                    loading = false
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED) {
            cargarUbicacion(context) { la, ln ->
                lat = la; lng = ln; ubicacionLista = true
                camera.position = CameraPosition.fromLatLngZoom(LatLng(la, ln), 14f)
            }
        } else {
            permLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    LaunchedEffect(flowCtx?.distKmObjetivo, distanciaObjetivoKm) {
        val km = distanciaObjetivoKm ?: flowCtx?.distKmObjetivo
        if (km != null && km > 0) distKm = km
    }

    LaunchedEffect(ubicacionLista, esModoLugares) {
        if (ubicacionLista && esModoLugares) cargarLugares()
    }

    LaunchedEffect(ubicacionLista, tipoLugar) {
        if (ubicacionLista && tab == 0) cargarLugares()
    }

    /** Al cambiar km, regenera la ruta sugerida y la dibuja en el mapa. */
    LaunchedEffect(ubicacionLista, distKm, tab, modoPersonalizado, esModoLugares) {
        if (!ubicacionLista || esModoLugares || tab != 1 || modoPersonalizado) return@LaunchedEffect
        variacion = 0
        delay(350)
        generarRuta()
    }

    fun otraPropuesta() {
        variacion++
        generarRuta()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(titulo) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            cargarUbicacion(context) { la, ln ->
                                lat = la; lng = ln; ubicacionLista = true
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
            if (!esModoLugares) {
                TabRow(selectedTabIndex = tab) {
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Ruta sugerida") })
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Lugares") })
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = camera,
                    properties = MapProperties(isMyLocationEnabled = true),
                    uiSettings = MapUiSettings(myLocationButtonEnabled = false),
                    onMapClick = { click ->
                        if (tab == 1 && modoPersonalizado) waypoints.add(click)
                    }
                ) {
                    Marker(state = MarkerState(LatLng(lat, lng)), title = "Tú")
                    if (tab == 0 || esModoLugares) {
                        lugares.forEach { l ->
                            Marker(
                                state = MarkerState(LatLng(l.lat, l.lng)),
                                title = l.nombre,
                                snippet = l.direccion
                            )
                        }
                    }
                    if (tab == 1 || !esModoLugares) {
                        ruta?.puntos?.takeIf { it.size >= 2 }?.let { pts ->
                            val latLngs = pts.map { LatLng(it.lat, it.lng) }
                            Polyline(points = latLngs, color = Color(0xFF2E7D32), width = 10f)
                        }
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
                    if (tab == 0 || esModoLugares) {
                        Text(
                            "Gyms, CrossFit y parques cerca de ti",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                            Text("${lugares.size} lugares cerca", style = MaterialTheme.typography.labelMedium)
                            lugares.take(3).forEach { l ->
                                Text(
                                    "• ${l.nombre} (${"%.1f".format(l.distanciaM / 1000)} km)",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    } else {
                        flowCtx?.tituloSesion?.let { t ->
                            Text(
                                "Sesión: $t",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (distanciaObjetivoKm != null) {
                            Text(
                                "Del plan de hoy: ${"%.1f".format(distanciaObjetivoKm)} km",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text("Distancia: ${"%.1f".format(distKm)} km", fontWeight = FontWeight.Medium)
                        Text(
                            "La ruta en verde se actualiza al mover la distancia",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = distKm.toFloat(),
                            onValueChange = { distKm = it.toDouble() },
                            valueRange = 1f..15f,
                            steps = 13,
                            enabled = !modoPersonalizado && !loading
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
                                "Toca el mapa para trazar tu ruta (${waypoints.size} puntos)",
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (waypoints.size >= 2) {
                                OutlinedButton(
                                    onClick = { generarRuta() },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !loading
                                ) {
                                    Icon(Icons.Filled.TouchApp, null, Modifier.size(16.dp))
                                    Spacer(Modifier.size(4.dp))
                                    Text("Crear mi ruta")
                                }
                            }
                            if (waypoints.isNotEmpty()) {
                                OutlinedButton(onClick = { waypoints.clear(); ruta = null }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Limpiar puntos")
                                }
                            }
                        }
                        ruta?.let { r ->
                            Text(
                                "${r.nombre} · ${"%.2f".format(r.distanciaKm)} km",
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Button(
                            onClick = { iniciarCarreraConRuta() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !loading && (ubicacionLista || ruta != null)
                        ) {
                            Icon(Icons.Filled.PlayArrow, null, Modifier.size(20.dp))
                            Spacer(Modifier.size(8.dp))
                            Text(
                                if (ruta != null) "Iniciar carrera con esta ruta"
                                else "Generar ruta e iniciar carrera"
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { otraPropuesta() },
                                modifier = Modifier.weight(1f),
                                enabled = !loading && !modoPersonalizado
                            ) {
                                Icon(Icons.Filled.Refresh, null, Modifier.size(16.dp))
                                Spacer(Modifier.size(4.dp))
                                Text("Otra propuesta")
                            }
                            OutlinedButton(
                                onClick = {
                                    val r = ruta ?: return@OutlinedButton
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
                                            android.content.Intent.createChooser(intent, "Exportar GPX")
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = ruta != null
                            ) {
                                Icon(Icons.Filled.Download, null, Modifier.size(16.dp))
                                Spacer(Modifier.size(4.dp))
                                Text("GPX")
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
    } catch (_: Exception) { /* ubicación por defecto */ }
}
