package com.opofit.miapp.ui.screens.entrenamientos

import com.opofit.miapp.ui.components.ElevatedCard
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.UploadFile
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
import com.opofit.miapp.gps.service.ShareActivityContext
import com.opofit.miapp.gps.service.buildPendingShareFromEntreno
import com.opofit.miapp.utils.MapaEntrenoNav
import com.opofit.miapp.ui.components.EntrenoLiveMetricsBar
import com.opofit.miapp.ui.components.ExerciseValueInput
import com.opofit.miapp.utils.EntrenoExerciseUtil
import com.opofit.miapp.utils.EntrenoRelojImport
import com.opofit.miapp.utils.EntrenoValidation
import com.opofit.miapp.utils.TimeFormatUtil
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
    onEntrenamientoFinalizado: (offerShare: Boolean) -> Unit,
    onNavigateToGps: (distKm: Double?) -> Unit = {},
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
    var elapsedMs by remember { mutableStateOf(0L) }
    var showObjetivoDialog by remember { mutableStateOf(false) }
    var objetivoEjercicioNombre by remember { mutableStateOf<String?>(null) }
    var avisoMsg by remember { mutableStateOf<String?>(null) }
    var sessionStartMs by remember { mutableStateOf<Long?>(null) }
    var importingReloj by remember { mutableStateOf(false) }
    var gpsActividadUuid by remember { mutableStateOf<String?>(null) }

    fun objetivoSegundosDesdeNombre(nombre: String): Int? {
        val regex = Regex("(\\d+)\\s*min\\b", RegexOption.IGNORE_CASE)
        val min = regex.find(nombre)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return null
        return min * 60
    }

    fun esEjercicioGps(nombre: String, tipo: String?): Boolean {
        val n = nombre.lowercase()
        if (n.contains("cinta") || n.contains("tapiz") || n.contains("treadmill")) return false
        if (tipo == "RUN") return true
        return Regex("\\b(bici|ciclismo|bicicleta|caminar|marcha|paseo)\\b").containsMatchIn(n)
    }

    fun ritmoVelocidadTexto(tipo: String?, distText: String, secs: Double): Pair<String, String> {
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
        val distancia: String = "",
        val pilar: String? = null
    )
    val ejerciciosUi = remember(rutinaId, rutina?.ejercicios) {
        mutableStateListOf<EjUi>().apply {
            rutina?.ejercicios?.forEach { ej ->
                val nombre = ej.nombre_ejercicio ?: "Ejercicio"
                val objetivo = objetivoSegundosDesdeNombre(nombre)
                val tipo = EntrenoExerciseUtil.tipoCardio(nombre, null, null)
                val unidad = EntrenoValidation.inferirUnidad(nombre, null, null, null)
                add(
                    EjUi(
                        id = ej.id_ejercicio,
                        nombre = nombre,
                        valor = if (tipo != null && unidad == "min") "" else (ej.repeticiones ?: 0).toString(),
                        checked = false,
                        objetivoSegundos = objetivo,
                        tipo = tipo,
                        pilar = null
                    )
                )
            }
        }
    }

    fun slotsParaImport(): List<EntrenoRelojImport.Slot> =
        ejerciciosUi.mapIndexed { idx, e ->
            EntrenoRelojImport.Slot(
                index = idx,
                nombre = e.nombre,
                tipo = e.tipo,
                pilar = e.pilar,
                objetivoSegundos = e.objetivoSegundos,
                hecho = e.checked,
                valor = e.valor,
                distancia = e.distancia
            )
        }

    fun aplicarImportReloj(result: EntrenoRelojImport.Result) {
        gpsActividadUuid = result.activityId
        if (result.elapsedMs > elapsedMs) {
            elapsedMs = result.elapsedMs
            cronometroIniciadoAlgunaVez = true
        }
        result.updates.forEach { u ->
            val e = ejerciciosUi.getOrNull(u.index) ?: return@forEach
            ejerciciosUi[u.index] = e.copy(
                valor = u.valor ?: e.valor,
                distancia = u.distancia ?: e.distancia,
                checked = u.hecho ?: e.checked
            )
        }
        avisoMsg = result.message
    }

    fun importarActividadReloj(activity: com.opofit.miapp.gps.model.ActivitySummary) {
        val result = EntrenoRelojImport.apply(
            activity = activity,
            slots = slotsParaImport(),
            unidadFor = { idx ->
                val ej = ejerciciosUi.getOrNull(idx)
                EntrenoValidation.inferirUnidad(ej?.nombre ?: "", null, ej?.pilar, null)
            },
            esGps = { slot -> esEjercicioGps(slot.nombre, slot.tipo) },
            unitDist = unitDist
        )
        aplicarImportReloj(result)
    }

    val importFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            importingReloj = true
            try {
                val activity = EntrenoRelojImport.importFromUri(context, uri)
                if (activity == null) {
                    avisoMsg = "No se pudo leer el fichero del reloj."
                } else {
                    importarActividadReloj(activity)
                }
            } catch (e: Exception) {
                avisoMsg = e.message ?: "Error al importar el fichero."
            } finally {
                importingReloj = false
            }
        }
    }

    fun aplicarCronometro(idx: Int) {
        val ej = ejerciciosUi.getOrNull(idx) ?: return
        if (elapsedMs <= 0L) {
            avisoMsg = "Inicia el cronómetro antes de aplicar el tiempo."
            return
        }
        val unidadEff = EntrenoValidation.inferirUnidad(ej.nombre, null, ej.pilar, null)
        val sec = TimeFormatUtil.secondsFromMs(elapsedMs)
        val nuevoValor = when {
            ej.tipo != null -> when {
                ej.objetivoSegundos != null || unidadEff == "min" -> "%.3f".format(sec / 60.0)
                unidadEff == "s" -> "%.3f".format(sec)
                else -> "%.3f".format(sec / 60.0)
            }
            unidadEff == "min" -> "%.3f".format(sec / 60.0)
            unidadEff == "s" -> "%.3f".format(sec)
            else -> ej.valor
        }
        ejerciciosUi[idx] = ej.copy(valor = nuevoValor, checked = true)
        avisoMsg = "Tiempo aplicado (${TimeFormatUtil.formatElapsedMs(elapsedMs)})."
    }

    LaunchedEffect(cronometroActivo, ejerciciosUi) {
        while (cronometroActivo) {
            delay(50L)
            elapsedMs += 50L
            val elapsedSec = (elapsedMs / 1000).toInt()
            val currentTimed = ejerciciosUi.firstOrNull { !it.checked && it.objetivoSegundos != null }
            val obj = currentTimed?.objetivoSegundos
            if (obj != null && elapsedSec >= obj && cronometroActivo) {
                cronometroActivo = false
                objetivoEjercicioNombre = currentTimed.nombre
                showObjetivoDialog = true
            }
        }
    }

    val gpsLast by GpsLastResult.value.collectAsState()
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
            val gpsMs = summary.durationSec * 1000L
            if (elapsedMs < gpsMs) {
                elapsedMs = gpsMs
                cronometroIniciadoAlgunaVez = true
            }
        }
        GpsLastResult.consume()
    }

    var guardando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    val alMenosUnoMarcado = ejerciciosUi.any { it.checked }
    val puedeFinalizar = cronometroIniciadoAlgunaVez && elapsedMs > 0L && alMenosUnoMarcado && !guardando


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
        avisoMsg?.let { msg ->
            AlertDialog(
                onDismissRequest = { avisoMsg = null },
                confirmButton = {
                    Button(onClick = { avisoMsg = null }) { Text("Entendido") }
                },
                title = { Text("Aviso") },
                text = { Text(msg) }
            )
        }

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
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Tiempo", color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Text(
                                        text = TimeFormatUtil.formatElapsedMs(elapsedMs),
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
                                        if (cronometroActivo) {
                                            cronometroIniciadoAlgunaVez = true
                                            if (sessionStartMs == null) sessionStartMs = System.currentTimeMillis()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = ejerciciosUi.isNotEmpty()
                                ) {
                                    Text(if (cronometroActivo) "Pausar" else "Iniciar")
                                }
                                Button(
                                    onClick = {
                                        cronometroActivo = false
                                        elapsedMs = 0L
                                        cronometroIniciadoAlgunaVez = false
                                        sessionStartMs = null
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = elapsedMs > 0L
                                ) {
                                    Text("Reiniciar")
                                }
                            }
                        }

                        if (cronometroActivo || cronometroIniciadoAlgunaVez) {
                            item {
                                val activoEj = ejerciciosUi.firstOrNull { !it.checked }
                                val distKm = activoEj?.distancia?.replace(",", ".")?.toDoubleOrNull()?.let { d ->
                                    if (unitDist == "mi" && activoEj.tipo == "RUN") Units.miToKm(d) else d
                                }
                                EntrenoLiveMetricsBar(
                                    elapsedMs = elapsedMs,
                                    cronometroActivo = cronometroActivo || cronometroIniciadoAlgunaVez,
                                    distanciaKm = distKm,
                                    tipoCardio = activoEj?.tipo
                                )
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            importingReloj = true
                                            try {
                                                val token = tokenManager.getToken().first().orEmpty()
                                                EntrenoRelojImport.syncFromWatch(context, token)
                                                val since = sessionStartMs?.minus(15 * 60_000L)
                                                val activity = EntrenoRelojImport.findRecentActivity(context, since)
                                                if (activity == null) {
                                                    avisoMsg = "No hay actividades recientes del reloj. Conecta Health Connect o importa un fichero TCX."
                                                } else {
                                                    importarActividadReloj(activity)
                                                }
                                            } catch (e: Exception) {
                                                avisoMsg = e.message ?: "Error al sincronizar."
                                            } finally {
                                                importingReloj = false
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = !importingReloj
                                ) {
                                    if (importingReloj) {
                                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Filled.Sync, null, Modifier.size(18.dp))
                                    }
                                    Spacer(Modifier.size(6.dp))
                                    Text("Importar del reloj")
                                }
                                OutlinedButton(
                                    onClick = { importFileLauncher.launch("*/*") },
                                    modifier = Modifier.weight(1f),
                                    enabled = !importingReloj
                                ) {
                                    Icon(Icons.Filled.UploadFile, null, Modifier.size(18.dp))
                                    Spacer(Modifier.size(6.dp))
                                    Text("TCX/GPX")
                                }
                            }
                            Text(
                                "Tras entrenar en el reloj, importa aquí los valores reales y luego finaliza para guardar el seguimiento.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        item {
                            Text(
                                text = "Ejercicios",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        itemsIndexed(ejerciciosUi) { idx, ej ->
                            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
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
                                                text = "Objetivo: ${TimeFormatUtil.formatElapsedMs(ej.objetivoSegundos * 1000L, showMs = false)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = { aplicarCronometro(idx) },
                                            modifier = Modifier.fillMaxWidth(),
                                            enabled = elapsedMs > 0L && !ej.checked
                                        ) {
                                            Icon(Icons.Filled.Timer, null, Modifier.size(18.dp))
                                            Spacer(Modifier.size(6.dp))
                                            Text("Usar tiempo del cronómetro (${TimeFormatUtil.formatElapsedMs(elapsedMs)})")
                                        }
                                        val secsCalc = ej.objetivoSegundos?.let {
                                            minOf(TimeFormatUtil.secondsFromMs(elapsedMs), it.toDouble())
                                        } ?: TimeFormatUtil.secondsFromMs(elapsedMs)
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
                                                if (elapsedMs <= 0L) {
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
                                                onClick = {
                                                    val kmPlan = MapaEntrenoNav.distanciaKmDesdeTexto(ej.nombre)
                                                        ?: ej.distancia.replace(",", ".").toDoubleOrNull()
                                                        ?: MapaEntrenoNav.distanciaKmDesdeTexto(ej.valor)
                                                    onNavigateToGps(kmPlan)
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(Icons.Filled.Explore, null, Modifier.size(18.dp))
                                                Spacer(Modifier.size(6.dp))
                                                Text("Ruta e iniciar carrera")
                                            }
                                        }
                                    } else {
                                        val unidadEff = EntrenoValidation.inferirUnidad(ej.nombre, null, ej.pilar, null)
                                        if ((unidadEff == "min" || unidadEff == "s") && elapsedMs > 0L) {
                                            OutlinedButton(
                                                onClick = { aplicarCronometro(idx) },
                                                modifier = Modifier.fillMaxWidth(),
                                                enabled = !ej.checked
                                            ) {
                                                Icon(Icons.Filled.Timer, null, Modifier.size(18.dp))
                                                Spacer(Modifier.size(6.dp))
                                                Text("Aplicar ${TimeFormatUtil.formatElapsedMs(elapsedMs)} del cronómetro")
                                            }
                                        }
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
                                            duracion = ((elapsedMs / 60_000L).toInt()).coerceAtLeast(1), // Backend espera minutos
                                            ejercicios = ejercicios,
                                            gpsActividadUuid = gpsActividadUuid
                                        )
                                        val resp = RetrofitClient.progresoApi.registrarEntrenamiento("Bearer $token", body)
                                        if (resp.ok) {
                                            val idHistorial = resp.id?.takeIf { it > 0 }
                                            idHistorial?.let { id ->
                                                ShareActivityContext.set(
                                                    buildPendingShareFromEntreno(
                                                        titulo = rutina?.nombre_personalizado ?: "Rutina personalizada",
                                                        idHistorial = id,
                                                        duracionMin = ((elapsedMs / 60_000L).toInt()).coerceAtLeast(1),
                                                        ejerciciosCount = ejercicios.size
                                                    )
                                                )
                                            }
                                            onEntrenamientoFinalizado(idHistorial != null)
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

