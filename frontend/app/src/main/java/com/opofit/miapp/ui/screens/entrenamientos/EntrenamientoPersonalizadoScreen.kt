package com.opofit.miapp.ui.screens.entrenamientos

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.size
import com.opofit.miapp.data.api.RetrofitClient
import com.opofit.miapp.data.local.TokenManager
import com.opofit.miapp.data.responsemodels.EjercicioRealizado
import com.opofit.miapp.data.responsemodels.RegistrarHistorialRequest
import com.opofit.miapp.gps.service.GpsLastResult
import com.opofit.miapp.ui.components.ExerciseValueInput
import com.opofit.miapp.utils.EntrenoValidation
import com.opofit.miapp.ui.viewmodels.AuthViewModel
import com.opofit.miapp.ui.viewmodels.RutinasLibresViewModel
import com.opofit.miapp.utils.Units
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntrenamientoPersonalizadoScreen(
    rutinaId: Int,
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onEntrenamientoFinalizado: () -> Unit,
    onNavigateToGps: () -> Unit = {},
    rutinasLibresViewModel: RutinasLibresViewModel = viewModel()
) {
    val authState by authViewModel.uiState.collectAsState()
    val uiState by rutinasLibresViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    var unitDist by remember { mutableStateOf("km") }
    LaunchedEffect(Unit) {
        tokenManager.getUnitDistancia().collectLatest { u ->
            if (!u.isNullOrBlank()) unitDist = u
        }
    }

    val userId = authState.userId ?: 0

    LaunchedEffect(userId) {
        if (userId > 0 && uiState.rutinas.isEmpty()) {
            rutinasLibresViewModel.cargarRutinas(userId)
        }
    }

    val rutina = uiState.rutinas.firstOrNull { it.id_rutina_pers == rutinaId }
    val scope = rememberCoroutineScope()

    var cronometroActivo by remember { mutableStateOf(false) }
    var cronometroIniciadoAlgunaVez by remember { mutableStateOf(false) }
    var tiempoSegundos by remember { mutableIntStateOf(0) }
    var showObjetivoDialog by remember { mutableStateOf(false) }
    var objetivoEjercicioNombre by remember { mutableStateOf<String?>(null) }

    fun objetivoSegundosDesdeNombre(nombre: String): Int? {
        val regex = Regex("(\\d+)\\s*min\\b", RegexOption.IGNORE_CASE)
        val min = regex.find(nombre)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return null
        return min * 60
    }

    fun tipoCardio(nombre: String): String? {
        val n = nombre.lowercase()
        return when {
            n.contains("natación") || n.contains("natacion") -> "SWIM"
            n.contains("carrera") || n.contains("trote") || n.contains("rodaje") || n.contains("fartlek") -> "RUN"
            else -> null
        }
    }

    fun esEjercicioGps(nombre: String, tipo: String?): Boolean {
        val n = nombre.lowercase()
        if (n.contains("cinta") || n.contains("tapiz") || n.contains("treadmill")) return false
        if (tipo == "RUN") return true
        return Regex("\\b(bici|ciclismo|bicicleta|caminar|marcha|paseo)\\b").containsMatchIn(n)
    }

    fun ritmoVelocidadTexto(tipo: String?, distText: String, secs: Int): Pair<String, String> {
        val dist = distText.replace(",", ".").toDoubleOrNull() ?: return "-" to "-"
        if (dist <= 0.0 || secs <= 0) return "-" to "-"
        return when (tipo) {
            "RUN" -> {
                val distKm = if (unitDist == "mi") Units.miToKm(dist) else dist
                val paceSecPerKm = secs / distKm
                val hours = secs / 3600.0
                val speedKmh = distKm / hours
                if (unitDist == "mi") {
                    val paceSecPerMi = paceSecPerKm * 1.609344
                    Units.formatPace(paceSecPerMi) + " min/mi" to "%.2f mph".format(Units.kmhToMph(speedKmh))
                } else {
                    Units.formatPace(paceSecPerKm) + " min/km" to "%.2f km/h".format(speedKmh)
                }
            }
            "SWIM" -> {
                if (unitDist == "mi") {
                    val distYd = dist
                    val per100 = secs / (distYd / 100.0)
                    val speed = distYd / secs
                    Units.formatPace(per100) + " min/100yd" to "%.2f yd/s".format(speed)
                } else {
                    val distM = dist
                    val per100 = secs / (distM / 100.0)
                    val speed = distM / secs
                    Units.formatPace(per100) + " min/100m" to "%.2f m/s".format(speed)
                }
            }
            else -> "-" to "-"
        }
    }

    data class EjUi(
        val id: Int,
        val nombre: String,
        val valor: String,
        val checked: Boolean,
        val objetivoSegundos: Int? = null,
        val tipo: String? = null,
        val distancia: String = ""
    )
    val ejerciciosUi = remember(rutinaId, rutina?.ejercicios) {
        mutableStateListOf<EjUi>().apply {
            rutina?.ejercicios?.forEach { ej ->
                val nombre = ej.nombre_ejercicio ?: "Ejercicio"
                val objetivo = objetivoSegundosDesdeNombre(nombre)
                val tipo = tipoCardio(nombre)
                add(
                    EjUi(
                        id = ej.id_ejercicio,
                        nombre = nombre,
                        valor = (ej.repeticiones ?: 0).toString(),
                        checked = false,
                        objetivoSegundos = objetivo,
                        tipo = tipo
                    )
                )
            }
        }
    }

    LaunchedEffect(cronometroActivo, ejerciciosUi) {
        while (cronometroActivo) {
            delay(1000)
            tiempoSegundos += 1
            val currentTimed = ejerciciosUi.firstOrNull { !it.checked && it.objetivoSegundos != null }
            val obj = currentTimed?.objetivoSegundos
            if (obj != null && tiempoSegundos >= obj && cronometroActivo) {
                cronometroActivo = false
                objetivoEjercicioNombre = currentTimed.nombre
                showObjetivoDialog = true
            }
        }
    }

    val gpsLast by GpsLastResult.value.collectAsState()
    var gpsActividadUuid by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(gpsLast?.id) {
        val summary = gpsLast ?: return@LaunchedEffect
        gpsActividadUuid = summary.id
        val idx = ejerciciosUi.indexOfFirst { !it.checked && esEjercicioGps(it.nombre, it.tipo) }
        if (idx >= 0) {
            val km = summary.distanceM / 1000.0
            val mostrado = if (unitDist == "mi") "%.2f".format(km / 1.609344) else "%.2f".format(km)
            ejerciciosUi[idx] = ejerciciosUi[idx].copy(
                distancia = mostrado,
                valor = km.toString()
            )
            if (tiempoSegundos < summary.durationSec) {
                tiempoSegundos = summary.durationSec
                cronometroIniciadoAlgunaVez = true
            }
        }
        GpsLastResult.consume()
    }

    var guardando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    val alMenosUnoMarcado = ejerciciosUi.any { it.checked }
    val puedeFinalizar = cronometroIniciadoAlgunaVez && tiempoSegundos > 0 && alMenosUnoMarcado && !guardando

    fun formatTime(s: Int): String {
        val mm = s / 60
        val ss = s % 60
        return "%02d:%02d".format(mm, ss)
    }
    

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(rutina?.nombre_personalizado ?: "Entrenamiento") },
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
        if (showObjetivoDialog) {
            AlertDialog(
                onDismissRequest = { showObjetivoDialog = false },
                confirmButton = {
                    Button(onClick = { showObjetivoDialog = false }) { Text("¡Genial!") }
                },
                title = { Text("🎉 Objetivo completado") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Has completado el objetivo de tiempo${objetivoEjercicioNombre?.let { ": $it" } ?: ""}.")
                        Text("Marca el ejercicio como completado e introduce la distancia para ver ritmo/velocidad.")
                    }
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                rutina == null -> Text(
                    text = "No se encontró la rutina personalizada.",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Tiempo", color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Text(
                                        text = formatTime(tiempoSegundos),
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    
                                }
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = {
                                        cronometroActivo = !cronometroActivo
                                        if (cronometroActivo) cronometroIniciadoAlgunaVez = true
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = ejerciciosUi.isNotEmpty()
                                ) {
                                    Text(if (cronometroActivo) "Pausar" else "Iniciar")
                                }
                                Button(
                                    onClick = {
                                        cronometroActivo = false
                                        tiempoSegundos = 0
                                        cronometroIniciadoAlgunaVez = false
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Reiniciar")
                                }
                            }
                        }

                        item {
                            Text(
                                text = "Ejercicios",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        itemsIndexed(ejerciciosUi) { idx, ej ->
                            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(1.dp)) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = ej.checked,
                                            onCheckedChange = { checked ->
                                                ejerciciosUi[idx] = ej.copy(checked = checked)
                                            }
                                        )
                                        Spacer(modifier = Modifier.height(0.dp))
                                        Text(ej.nombre, fontWeight = FontWeight.SemiBold)
                                    }
                                    if (ej.tipo != null) {
                                        if (ej.objetivoSegundos != null) {
                                            Text(
                                                text = "Objetivo: ${formatTime(ej.objetivoSegundos)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        val secsCalc = ej.objetivoSegundos?.let { minOf(tiempoSegundos, it) } ?: tiempoSegundos
                                        val (ritmoTxt, velTxt) = ritmoVelocidadTexto(ej.tipo, ej.distancia, secsCalc)
                                        val labelDist = when (ej.tipo) {
                                            "SWIM" -> if (unitDist == "mi") "Distancia (yd)" else "Distancia (m)"
                                            "RUN" -> if (unitDist == "mi") "Distancia (mi)" else "Distancia (km)"
                                            else -> "Distancia"
                                        }
                                        OutlinedTextField(
                                            value = ej.distancia,
                                            onValueChange = { v ->
                                                val baseValue = when (ej.tipo) {
                                                    "RUN" -> {
                                                        val d = v.replace(",", ".").toDoubleOrNull()
                                                        if (d != null && unitDist == "mi") Units.miToKm(d).toString() else v
                                                    }
                                                    "SWIM" -> {
                                                        val d = v.replace(",", ".").toDoubleOrNull()
                                                        if (d != null && unitDist == "mi") Units.ydToM(d).toString() else v
                                                    }
                                                    else -> v
                                                }
                                                ejerciciosUi[idx] = ej.copy(distancia = v, valor = baseValue)
                                            },
                                            label = { Text(labelDist) },
                                            placeholder = { Text(if (ej.tipo == "SWIM") "Ej: 1500" else "Ej: 7.20") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            supportingText = {
                                                if (tiempoSegundos <= 0) {
                                                    Text("Inicia el cronómetro para calcular ritmo y velocidad.")
                                                }
                                            }
                                        )
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Column {
                                                Text("Ritmo medio", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text(ritmoTxt, fontWeight = FontWeight.Bold)
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("Velocidad media", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text(velTxt, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        if (esEjercicioGps(ej.nombre, ej.tipo)) {
                                            OutlinedButton(
                                                onClick = onNavigateToGps,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(Icons.Filled.Explore, null, Modifier.size(18.dp))
                                                Spacer(Modifier.size(6.dp))
                                                Text("Registrar con GPS")
                                            }
                                        }
                                    } else {
                                        val unidadEff = EntrenoValidation.inferirUnidad(ej.nombre, null)
                                        ExerciseValueInput(
                                            value = ej.valor,
                                            onValueChange = { v -> ejerciciosUi[idx] = ej.copy(valor = v) },
                                            unidad = unidadEff,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }

                        if (error.isNotBlank()) {
                            item {
                                Text(error, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    
                    Column(
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        if (guardando) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Button(
                            onClick = {
                                guardando = true
                                error = ""
                                val ejercicios = ejerciciosUi
                                    .filter { it.checked }
                                    .mapNotNull {
                                        val v = it.valor.toDoubleOrNull() ?: return@mapNotNull null
                                        EjercicioRealizado(id_ejercicio = it.id, valor = v)
                                    }
                                if (ejercicios.isEmpty()) {
                                    guardando = false
                                    error = "Marca al menos un ejercicio y añade un valor numérico."
                                    return@Button
                                }
                                scope.launch {
                                    try {
                                        val token = tokenManager.getToken().first() ?: ""
                                        val body = RegistrarHistorialRequest(
                                            userId = userId,
                                            tipoRutina = "PERS",
                                            idRutina = rutinaId,
                                            duracion = (tiempoSegundos / 60).coerceAtLeast(1), // Backend espera minutos
                                            ejercicios = ejercicios,
                                            gpsActividadUuid = gpsActividadUuid
                                        )
                                        val resp = RetrofitClient.progresoApi.registrarEntrenamiento("Bearer $token", body)
                                        if (resp.ok) {
                                            onEntrenamientoFinalizado()
                                        } else {
                                            error = resp.msg ?: resp.message ?: "No se pudo guardar el entrenamiento"
                                            guardando = false
                                        }
                                    } catch (e: Exception) {
                                        error = e.message ?: "Error de conexión"
                                        guardando = false
                                    }
                                }
                            },
                            enabled = puedeFinalizar,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Finalizar Entrenamiento")
                        }
                    }
                }
            }
        }
    }
}

