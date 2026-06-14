package com.opofit.miapp.gps.ui

import com.opofit.miapp.ui.components.ElevatedCard
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.opofit.miapp.ui.utils.isCompactScreen
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
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
import com.opofit.miapp.data.responsemodels.CrearSegmentoGeoRequest
import com.opofit.miapp.data.responsemodels.RutaPersonalizadaBody
import com.opofit.miapp.gps.service.EntrenoFlowContext
import com.opofit.miapp.gps.service.GpsRecordingContext
import com.opofit.miapp.gps.service.HrBleManager
import com.opofit.miapp.gps.service.PlannedRoute
import com.opofit.miapp.gps.service.RoutePoint
import com.opofit.miapp.gps.service.RoutePreferences
import com.opofit.miapp.gps.util.RouteGpxExport
import com.opofit.miapp.utils.MapaEntrenoNav
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAB_RUTAS = 0
private const val TAB_LUGARES = 1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapaEntrenoScreen(
    onNavigateBack: () -> Unit,
    onUsarRutaEnGps: () -> Unit,
    onActividadLibre: () -> Unit,
    distanciaObjetivoKm: Double? = null,
    modoInicial: String = MapaEntrenoNav.MODO_RUTAS,
    tipoLugarInicial: String = "GYM",
    actividadInicial: String = "CARRERA",
    terrenoInicial: String = "CIUDAD"
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val tokenManager = remember { TokenManager(context) }
    val esModoLugares = modoInicial == MapaEntrenoNav.MODO_LUGARES
    var tab by remember { mutableIntStateOf(if (esModoLugares) TAB_LUGARES else TAB_RUTAS) }
    var lat by remember { mutableDoubleStateOf(40.4168) }
    var lng by remember { mutableDoubleStateOf(-3.7038) }
    var ubicacionLista by remember { mutableStateOf(false) }
    fun tienePermisoUbicacion(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }
    var locationOk by remember { mutableStateOf(tienePermisoUbicacion()) }
    var permisoDenegado by remember { mutableStateOf(false) }
    var tipoLugar by remember { mutableStateOf(tipoLugarInicial) }
    var lugares by remember { mutableStateOf<List<LugarEntreno>>(emptyList()) }
    var ruta by remember { mutableStateOf<RutaEntreno?>(null) }
    var actividad by remember { mutableStateOf(actividadInicial.uppercase()) }
    var terreno by remember { mutableStateOf(terrenoInicial.uppercase()) }
    val limites = remember(actividad, terreno) { MapaEntrenoNav.limitesRuta(actividad, terreno) }
    var distKm by remember(actividad, terreno) {
        mutableDoubleStateOf(
            (distanciaObjetivoKm ?: 5.0).coerceIn(limites.minKm.toDouble(), limites.maxKm.toDouble())
        )
    }
    var variacion by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    val flowCtx by EntrenoFlowContext.state.collectAsState()
    var modoPersonalizado by remember { mutableStateOf(false) }
    val waypoints = remember { mutableStateListOf<LatLng>() }
    val camera = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(lat, lng), 14f)
    }

    val hrManager = remember { HrBleManager.get(context) }
    val hrState by hrManager.state.collectAsState()
    val liveHr by hrManager.heartRate.collectAsState()
    val hrConnected = hrState is HrBleManager.State.Connected
    val hrLabel = when {
        liveHr != null -> "♥ $liveHr bpm"
        hrConnected -> "♥ Conectado"
        hrManager.savedDeviceAddress() != null -> "♥ Reconectando…"
        else -> "♥ Sin reloj"
    }

    LaunchedEffect(Unit) {
        hrManager.autoConnectSavedDevice()
    }

    val titulo = if (esModoLugares) {
        "Dónde entrenar"
    } else {
        when {
            actividad == "BICI" || actividad == "BIKE" -> "Ruta de bici"
            actividad == "CAMINAR" || actividad == "WALK" -> "Ruta a pie"
            else -> "Ruta de carrera"
        }
    }
    val esBici = actividad == "BICI" || actividad == "BIKE"
    val esCaminar = actividad == "CAMINAR" || actividad == "WALK"

    fun cargarMiUbicacion() {
        scope.launch {
            try {
                val obtenida = cargarUbicacion(context) { la, ln ->
                    lat = la
                    lng = ln
                    permisoDenegado = false
                    camera.position = CameraPosition.fromLatLngZoom(LatLng(la, ln), 14f)
                }
                ubicacionLista = true
                if (!obtenida) {
                    snackbar.showSnackbar("Usando ubicación aproximada. Activa el GPS para mayor precisión.")
                }
            } catch (e: Exception) {
                ubicacionLista = true
                snackbar.showSnackbar("No se pudo obtener tu ubicación: ${e.message}")
            }
        }
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        locationOk = granted[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (locationOk) {
            cargarMiUbicacion()
        } else {
            permisoDenegado = true
            ubicacionLista = false
            scope.launch {
                snackbar.showSnackbar("Necesitamos tu ubicación para buscar parques y lugares cerca de ti.")
            }
        }
    }

    fun solicitarUbicacion() {
        permLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }

    suspend fun auth(): String {
        val t = tokenManager.getToken().first() ?: ""
        return "Bearer $t"
    }

    fun cargarLugares() {
        if (!ubicacionLista) return
        scope.launch {
            loading = true
            lugares = emptyList()
            try {
                val radio = when (tipoLugar) {
                    "CALISTENIA" -> 20000
                    "PISTA" -> 18000
                    "PARQUE" -> 15000
                    "CROSSFIT" -> 15000
                    "PISCINA" -> 15000
                    else -> 12000
                }
                suspend fun buscar(tipo: String, r: Int) =
                    RetrofitClient.mapasApi.lugares(auth(), lat, lng, tipo, r).data.orEmpty()

                lugares = buscar(tipoLugar, radio)
                if (lugares.isEmpty() && tipoLugar == "CALISTENIA") {
                    lugares = buscar("PARQUE", radio)
                }
                if (lugares.isEmpty() && tipoLugar != "GYM") {
                    lugares = buscar("GYM", radio)
                }
                if (lugares.isEmpty()) {
                    val msg = when (tipoLugar) {
                        "CALISTENIA" -> "No hay parques de calistenia cerca. Prueba Calistenia → Parque o activa el GPS."
                        "PISTA" -> "No hay pistas cerca. Prueba otro filtro o desplázate a una zona deportiva."
                        else -> "No hay lugares de este tipo cerca. Comprueba que el GPS esté activo."
                    }
                    snackbar.showSnackbar(msg)
                }
            } catch (e: Exception) {
                lugares = emptyList()
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
                            nombre = "Mi ruta",
                            actividad = actividad
                        )
                    )
                } else {
                    RetrofitClient.mapasApi.rutaSugerida(auth(), lat, lng, distKm, variacion, actividad, terreno)
                }
                ruta = resp.data
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
        GpsRecordingContext.prepare(MapaEntrenoNav.tipoDesdeActividad(actividad), conRuta = true)
        onUsarRutaEnGps()
    }

    fun iniciarLibre() {
        GpsRecordingContext.prepare(MapaEntrenoNav.tipoDesdeActividad(actividad), conRuta = false)
        scope.launch {
            RoutePreferences.clear(context)
            onActividadLibre()
        }
    }

    fun iniciarCarreraConRuta() {
        scope.launch {
            val actual = ruta
            if (actual != null && actual.puntos.size >= 2) {
                guardarEIniciar(actual)
            } else {
                loading = true
                try {
                    val resp = RetrofitClient.mapasApi.rutaSugerida(auth(), lat, lng, distKm, variacion, actividad, terreno)
                    val generada = resp.data
                    if (generada != null && generada.puntos.size >= 2) {
                        ruta = generada
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
        locationOk = tienePermisoUbicacion()
        if (locationOk) cargarMiUbicacion()
    }

    LaunchedEffect(flowCtx?.distKmObjetivo, distanciaObjetivoKm) {
        val km = distanciaObjetivoKm ?: flowCtx?.distKmObjetivo
        if (km != null && km > 0) distKm = km
    }

    LaunchedEffect(ubicacionLista, tipoLugar, tab) {
        if (ubicacionLista && (tab == TAB_LUGARES || esModoLugares)) cargarLugares()
    }

    LaunchedEffect(lat, lng, ubicacionLista) {
        if (ubicacionLista) {
            camera.position = CameraPosition.fromLatLngZoom(LatLng(lat, lng), 14f)
        }
    }

    LaunchedEffect(lugares, tab, esModoLugares, ubicacionLista) {
        if (!ubicacionLista || lugares.isEmpty()) return@LaunchedEffect
        if (tab != TAB_LUGARES && !esModoLugares) return@LaunchedEffect
        val builder = LatLngBounds.builder()
        builder.include(LatLng(lat, lng))
        lugares.forEach { builder.include(LatLng(it.lat, it.lng)) }
        try {
            camera.move(CameraUpdateFactory.newLatLngBounds(builder.build(), 120))
        } catch (_: Exception) {
            camera.position = CameraPosition.fromLatLngZoom(LatLng(lat, lng), 13f)
        }
    }

    LaunchedEffect(ruta, tab, esModoLugares) {
        if (esModoLugares || tab != TAB_RUTAS) return@LaunchedEffect
        val pts = ruta?.puntos?.takeIf { it.size >= 2 } ?: return@LaunchedEffect
        val builder = LatLngBounds.builder()
        pts.forEach { builder.include(LatLng(it.lat, it.lng)) }
        try {
            camera.move(CameraUpdateFactory.newLatLngBounds(builder.build(), 100))
        } catch (_: Exception) { }
    }

    /** Al cambiar km, regenera la ruta sugerida y la dibuja en el mapa. */
    LaunchedEffect(ubicacionLista, distKm, tab, modoPersonalizado, esModoLugares, actividad, terreno) {
        if (!ubicacionLista || esModoLugares || tab != TAB_RUTAS || modoPersonalizado) return@LaunchedEffect
        variacion = 0
        delay(350)
        generarRuta()
    }

    LaunchedEffect(actividad, terreno, limites) {
        distKm = distKm.coerceIn(limites.minKm.toDouble(), limites.maxKm.toDouble())
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
                        if (!locationOk) solicitarUbicacion() else cargarMiUbicacion()
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
        val compact = isCompactScreen()
        val panelScroll = rememberScrollState()
        val mapFraction = if (compact) 0.38f else 0.42f
        Column(Modifier.fillMaxSize().padding(pad)) {
            if (!esModoLugares) {
                TabRow(selectedTabIndex = tab) {
                    Tab(
                        selected = tab == TAB_RUTAS,
                        onClick = { tab = TAB_RUTAS },
                        text = { Text(if (compact) "Ruta" else "Ruta sugerida", maxLines = 1) }
                    )
                    Tab(
                        selected = tab == TAB_LUGARES,
                        onClick = {
                            tab = TAB_LUGARES
                            if (ubicacionLista) cargarLugares()
                        },
                        text = { Text("Lugares", maxLines = 1) }
                    )
                }
            }
            if (!locationOk) {
                ElevatedCard(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = if (compact) 4.dp else 8.dp)
                ) {
                    Column(
                        Modifier.padding(if (compact) 10.dp else 14.dp),
                        verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)
                    ) {
                        Text(
                            "Ubicación necesaria",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (!compact) {
                            Text(
                                if (permisoDenegado) {
                                    "Sin permiso de ubicación no podemos mostrar parques de calistenia ni lugares cerca de ti. Actívalo en Ajustes o pulsa el botón."
                                } else {
                                    "Para buscar parques, pistas y gimnasios cerca necesitamos acceder a tu ubicación."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(onClick = { solicitarUbicacion() }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.MyLocation, null, Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text(if (compact) "Activar ubicación" else "Permitir ubicación")
                        }
                    }
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(mapFraction)
                    .heightIn(min = if (compact) 120.dp else 140.dp)
            ) {
                val mostrarLugares = tab == TAB_LUGARES || esModoLugares
                val mostrarRuta = tab == TAB_RUTAS && !esModoLugares
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = camera,
                    properties = MapProperties(
                        isMyLocationEnabled = locationOk,
                        mapType = MapType.NORMAL
                    ),
                    uiSettings = MapUiSettings(
                        myLocationButtonEnabled = false,
                        zoomControlsEnabled = true
                    ),
                    onMapClick = { click ->
                        if (mostrarRuta && modoPersonalizado) {
                            waypoints.add(click)
                            // Feedback inmediato: el usuario veía que tocaba
                            // y "no pasaba nada" porque el contador estaba en
                            // texto pequeño. Snackbar le confirma el tap y le
                            // recuerda el mínimo de 2 puntos para crear la ruta.
                            scope.launch {
                                val tot = waypoints.size
                                val msg = if (tot < 2) {
                                    "Punto $tot añadido. Añade al menos 1 más para crear la ruta."
                                } else {
                                    "Punto $tot añadido. Pulsa \"Crear mi ruta\" cuando hayas terminado."
                                }
                                snackbar.showSnackbar(msg)
                            }
                        }
                    }
                ) {
                    // La ubicación del usuario YA se muestra con el punto azul
                    // nativo (isMyLocationEnabled). El Marker rojo "Tú" duplicaba
                    // la información y confundía: parecía un lugar más del mapa.
                    // Las chinchetas rojas quedan SOLO para lugares de entreno.
                    if (mostrarLugares) {
                        lugares.forEach { l ->
                            Marker(
                                state = MarkerState(LatLng(l.lat, l.lng)),
                                title = l.nombre,
                                snippet = l.direccion
                            )
                        }
                    }
                    if (mostrarRuta) {
                        ruta?.puntos?.takeIf { it.size >= 2 }?.let { pts ->
                            val latLngs = pts.map { LatLng(it.lat, it.lng) }
                            Polyline(points = latLngs, color = Color(0xFF2E7D32), width = 10f)
                        }
                        waypoints.forEachIndexed { i, p ->
                            Marker(state = MarkerState(p), title = "Punto ${i + 1}")
                        }
                    }
                }
                if (loading) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
            }
            Column(
                Modifier
                    .weight(1f - mapFraction)
                    .fillMaxWidth()
                    .verticalScroll(panelScroll)
            ) {
            ElevatedCard(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                Column(
                    Modifier.padding(if (compact) 10.dp else 12.dp),
                    verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)
                ) {
                    if (tab == TAB_LUGARES || esModoLugares) {
                        Text(
                            when (tipoLugar) {
                                "CALISTENIA" -> "Parques y zonas de barras / street workout"
                                "PISTA" -> "Pistas de atletismo y estadios"
                                "CROSSFIT" -> "Boxes y centros CrossFit"
                                "PARQUE" -> "Parques para entrenar al aire libre"
                                "PISCINA" -> "Piscinas municipales y centros acuáticos"
                                else -> "Gimnasios y centros deportivos"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listOf(
                                "GYM" to "Gym",
                                "CROSSFIT" to "CrossFit",
                                "PISTA" to "Pista",
                                "CALISTENIA" to "Calistenia",
                                "PARQUE" to "Parque",
                                "PISCINA" to "Piscina"
                            )) { (id, label) ->
                                FilterChip(
                                    selected = tipoLugar == id,
                                    onClick = {
                                        if (tipoLugar != id) {
                                            tipoLugar = id
                                            lugares = emptyList()
                                        }
                                    },
                                    label = { Text(label, maxLines = 1) }
                                )
                            }
                        }
                        if (!ubicacionLista && !loading && lugares.isEmpty()) {
                            Text(
                                "Activa la ubicación para buscar lugares cerca de ti.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (ubicacionLista && !loading && lugares.isEmpty()) {
                            Text(
                                when (tipoLugar) {
                                    "CALISTENIA" ->
                                        "No hay parques de calistenia cerca. Prueba Parque o mueve el mapa a otra zona."
                                    "PISTA" ->
                                        "No hay pistas de atletismo cerca en esta zona."
                                    "CROSSFIT" ->
                                        "No hay boxes de CrossFit cerca. Prueba Gym."
                                    "PISCINA" ->
                                        "No hay piscinas cerca. Prueba ampliar la búsqueda o comprueba que el GPS esté activo."
                                    else ->
                                        "No hay lugares de este tipo cerca. Prueba otro filtro."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (lugares.isNotEmpty()) {
                            val hayDemo = lugares.any { it.demo == true }
                            Text(
                                if (hayDemo) "${lugares.size} lugares (aproximados)" else "${lugares.size} lugares cerca",
                                style = MaterialTheme.typography.labelMedium
                            )
                            lugares.take(5).forEach { l ->
                                Text(
                                    "• ${l.nombre} (${"%.1f".format(l.distanciaM / 1000)} km)",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        if (ubicacionLista) {
                            OutlinedButton(
                                onClick = { cargarLugares() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !loading
                            ) {
                                Icon(Icons.Filled.Refresh, null, Modifier.size(16.dp))
                                Spacer(Modifier.size(6.dp))
                                Text("Actualizar lugares")
                            }
                        }
                    } else if (tab == TAB_RUTAS) {
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
                        AssistChip(
                            onClick = { onNavigateBack() },
                            label = { Text(hrLabel, maxLines = 1) },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Favorite,
                                    null,
                                    Modifier.size(18.dp),
                                    tint = if (hrConnected || liveHr != null) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                        if (!hrConnected && liveHr == null) {
                            Text(
                                "Pulso en vivo: conéctalo en Rutas GPS → Conectar pulso (opcional).",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listOf("CARRERA" to "Correr", "CAMINAR" to "Caminar", "BICI" to "Bici")) { (id, label) ->
                                FilterChip(
                                    selected = actividad == id,
                                    onClick = {
                                        if (actividad != id) {
                                            actividad = id
                                            waypoints.clear()
                                            ruta = null
                                            variacion = 0
                                        }
                                    },
                                    label = { Text(label, maxLines = 1) }
                                )
                            }
                        }
                        Text(
                            if (compact) {
                                "Objetivo: ${"%.1f".format(distKm)} km"
                            } else {
                                val verbo = when {
                                    esBici -> "pedalear"
                                    esCaminar -> "caminar"
                                    else -> "correr"
                                }
                                "¿Cuántos km quieres $verbo? ${"%.1f".format(distKm)} km (máx. ${limites.maxKm.toInt()})"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = terreno == "CIUDAD",
                                onClick = { terreno = "CIUDAD" },
                                label = { Text("Ciudad") }
                            )
                            FilterChip(
                                selected = terreno == "MONTANA",
                                onClick = { terreno = "MONTANA" },
                                label = { Text("Montaña") }
                            )
                        }
                        if (!compact) {
                            Text(
                                if (terreno == "MONTANA") {
                                    "Montaña: caminos y senderos (menos calles)"
                                } else {
                                    "Ciudad: calles y urbanización (sigue el mapa real)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Slider(
                            value = distKm.toFloat().coerceIn(limites.minKm, limites.maxKm),
                            onValueChange = { distKm = it.toDouble() },
                            valueRange = limites.minKm..limites.maxKm,
                            steps = limites.steps,
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
                            // Panel de creación manual de ruta estilo Garmin/Komoot
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                color = if (waypoints.isEmpty())
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                            ) {
                                Column(
                                    Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Edit,
                                            null,
                                            Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            "Traza tu ruta tocando el mapa",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    if (waypoints.isEmpty()) {
                                        Text(
                                            "Toca cualquier punto del mapa para añadir el inicio de tu ruta. Puedes añadir tantos puntos como quieras.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            // Chips de puntos
                                            waypoints.takeLast(4).forEachIndexed { i, _ ->
                                                val idx = if (waypoints.size > 4) waypoints.size - 4 + i else i
                                                Box(
                                                    Modifier
                                                        .size(24.dp)
                                                        .background(
                                                            if (idx == waypoints.lastIndex)
                                                                MaterialTheme.colorScheme.primary
                                                            else MaterialTheme.colorScheme.surfaceVariant,
                                                            CircleShape
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        "${idx + 1}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (idx == waypoints.lastIndex)
                                                            MaterialTheme.colorScheme.onPrimary
                                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            if (waypoints.size > 4) {
                                                Text("…", style = MaterialTheme.typography.labelMedium)
                                            }
                                            Spacer(Modifier.weight(1f))
                                            Text(
                                                "${waypoints.size} puntos",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                            // Instrucción dinámica según estado
                            if (waypoints.size in 1..1) {
                                Text(
                                    "Buen inicio. Añade al menos 1 punto más para trazar la ruta.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { generarRuta() },
                                    modifier = Modifier.weight(1f),
                                    enabled = !loading && waypoints.size >= 2,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Icon(Icons.Filled.Route, null, Modifier.size(16.dp))
                                    Spacer(Modifier.size(4.dp))
                                    Text(
                                        if (waypoints.size >= 2) "Crear ruta" else "Añade puntos",
                                        maxLines = 1
                                    )
                                }
                                if (waypoints.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            if (waypoints.isNotEmpty()) {
                                                waypoints.removeAt(waypoints.lastIndex)
                                            }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            "Deshacer último punto",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                            if (waypoints.size >= 2) {
                                OutlinedButton(
                                    onClick = { waypoints.clear(); ruta = null },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Limpiar todos los puntos")
                                }
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            try {
                                                val inicio = waypoints.first()
                                                val fin = waypoints.last()
                                                val resp = RetrofitClient.segmentosApi.crearGeografico(
                                                    auth(),
                                                    CrearSegmentoGeoRequest(
                                                        nombre = "Mi tramo personalizado",
                                                        latInicio = inicio.latitude,
                                                        lngInicio = inicio.longitude,
                                                        latFin = fin.latitude,
                                                        lngFin = fin.longitude,
                                                        categoria = when {
                                                            esBici -> "BICI"
                                                            esCaminar -> "CAMINATA"
                                                            else -> "CARRERA"
                                                        }
                                                    )
                                                )
                                                snackbar.showSnackbar(
                                                    if (resp.ok) "Segmento KOM guardado (inicio → fin)"
                                                    else resp.msg ?: "No se pudo guardar el segmento"
                                                )
                                            } catch (e: Exception) {
                                                snackbar.showSnackbar("Error: ${e.message}")
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !loading
                                ) {
                                    Text("Guardar inicio→fin como segmento KOM")
                                }
                            }
                        }
                        ruta?.let { r ->
                            val objetivoKm = r.distanciaObjetivoKm ?: distKm
                            val realKm = r.distanciaKm
                            val diffKm = kotlin.math.abs(realKm - objetivoKm)
                            val diffPct = if (objetivoKm > 0) diffKm / objetivoKm else 0.0
                            // Antes la alarma saltaba con +15% de desvío, lo que en rutas
                            // por calles es CONSTANTE (calles reales rara vez dan justo X km).
                            // Ahora: tolerancia ±10% silenciosa, aviso amable hasta 30%,
                            // solo warning fuerte si >30%.
                            Text(
                                "Recorrido real: ${"%.2f".format(realKm)} km",
                                fontWeight = FontWeight.Medium
                            )
                            val pctTxt = if (objetivoKm > 0) {
                                val signo = if (realKm > objetivoKm) "+" else "−"
                                " ($signo${"%.0f".format(diffPct * 100)}%)"
                            } else ""
                            Text(
                                "Objetivo del entreno: ${"%.1f".format(objetivoKm)} km$pctTxt",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            // ±10% silencioso; 10-30% aviso informativo; >30% aviso fuerte.
                            if (diffPct > 0.10 && diffPct <= 0.30) {
                                Text(
                                    "Ruta razonablemente cerca del objetivo. Si quieres exactitud, prueba «Otra propuesta».",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else if (diffPct > 0.30) {
                                Text(
                                    "La ruta calculada está bastante lejos del objetivo. Prueba «Otra propuesta» o ajusta el slider.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            val porCalles = r.origen.contains("calles") || r.origen.contains("osrm")
                            Text(
                                if (porCalles) "Trazado por calles reales" else "Ruta aproximada (sin datos de calles)",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (porCalles) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error
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
                                when {
                                    compact && ruta != null -> "Iniciar con esta ruta"
                                    compact -> "Generar e iniciar"
                                    ruta != null && esBici -> "Iniciar bici con esta ruta"
                                    ruta != null && esCaminar -> "Iniciar caminata con esta ruta"
                                    ruta != null -> "Iniciar carrera con esta ruta"
                                    esBici -> "Generar ruta e iniciar bici"
                                    esCaminar -> "Generar ruta e iniciar caminata"
                                    else -> "Generar ruta e iniciar carrera"
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        OutlinedButton(
                            onClick = { iniciarLibre() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !loading && ubicacionLista
                        ) {
                            Icon(Icons.Filled.Route, null, Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text(
                                if (compact) "Actividad libre" else "Actividad libre (sin seguir ruta)",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (compact) {
                            OutlinedButton(
                                onClick = { otraPropuesta() },
                                modifier = Modifier.fillMaxWidth(),
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
                                            android.content.Intent.createChooser(intent, "Compartir ruta")
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = ruta != null
                            ) {
                                Icon(Icons.Filled.Download, null, Modifier.size(16.dp))
                                Spacer(Modifier.size(4.dp))
                                Text("Compartir ruta con el reloj")
                            }
                        } else {
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
                                                android.content.Intent.createChooser(intent, "Compartir ruta")
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = ruta != null
                                ) {
                                    Icon(Icons.Filled.Download, null, Modifier.size(16.dp))
                                    Spacer(Modifier.size(4.dp))
                                    Text("Compartir")
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.size(8.dp))
            }
        }
    }
}

private suspend fun cargarUbicacion(
    context: android.content.Context,
    onResult: (Double, Double) -> Unit
): Boolean {
    return try {
        val fused = LocationServices.getFusedLocationProviderClient(context)
        val loc = fused.lastLocation.await()
        if (loc != null) {
            onResult(loc.latitude, loc.longitude)
            true
        } else {
            false
        }
    } catch (_: Exception) {
        false
    }
}
