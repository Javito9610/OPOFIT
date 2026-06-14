package com.opofit.miapp.ui.screens.entrenamientos

import com.opofit.miapp.gps.service.ChronoForegroundService
import com.opofit.miapp.gps.service.SessionTimerTracker
import com.opofit.miapp.ui.components.ElevatedCard
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opofit.miapp.data.api.RetrofitClient
import com.opofit.miapp.data.local.TokenManager
import com.opofit.miapp.data.responsemodels.Ejercicio
import com.opofit.miapp.data.responsemodels.EjercicioRealizado
import com.opofit.miapp.data.responsemodels.EjercicioPlan
import com.opofit.miapp.gps.service.GpsLastResult
import com.opofit.miapp.gps.service.ShareActivityContext
import com.opofit.miapp.gps.service.buildPendingShareFromEntreno
import com.opofit.miapp.gps.util.TcxExport
import com.opofit.miapp.ui.components.EntrenoActiveStepCard
import com.opofit.miapp.ui.components.EntrenoLiveMetricsBar
import com.opofit.miapp.ui.components.valoresPorSerieToCsv
import com.opofit.miapp.ui.components.resumenSeries
import com.opofit.miapp.ui.components.ExerciseDetailSheet
import com.opofit.miapp.ui.components.ExerciseInfoButton
import com.opofit.miapp.ui.components.ExerciseValueInput
import com.opofit.miapp.ui.components.RecordCelebrationDialog
import com.opofit.miapp.ui.components.RestTimerSheet
import com.opofit.miapp.utils.EntrenoExerciseUtil
import com.opofit.miapp.utils.EntrenoRelojImport
import com.opofit.miapp.utils.EntrenoValidation
import com.opofit.miapp.utils.TimeFormatUtil
import com.opofit.miapp.ui.viewmodels.AuthViewModel
import com.opofit.miapp.ui.viewmodels.HistorialViewModel
import com.opofit.miapp.ui.viewmodels.RutinasViewModel
import com.opofit.miapp.utils.MapaEntrenoNav
import com.opofit.miapp.utils.Units
import com.opofit.miapp.utils.rutinasUnicasPorEnfoque
import com.opofit.miapp.ui.components.PlanSesionActivaCard
import com.opofit.miapp.ui.components.enfoqueLabel
import com.opofit.miapp.utils.PrescripcionFormat
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val ENTRENO_TIMER_LABEL = "Entrenamiento"

private data class EjercicioEstado(
    val nombre: String,
    val idEjercicio: Int,
    var completado: Boolean = false,
    var valorConseguido: String = "",
    val objetivoSegundos: Int? = null,
    val tipo: String? = null,
    var distancia: String = "",
    val descansoSeg: Int = 90,
    val pilar: String? = null,
    val prescripcion: String = "",
    val instrucciones_tecnicas: String? = null,
    val grupo_muscular: String? = null,
    val equipamiento: String? = null,
    val tipo_ilustracion: String? = null,
    // Serie-por-serie (estilo Strong/Hevy): si la prescripción es "4×12",
    // el usuario rellena 4 entradas (una por serie). Se persiste como CSV
    // en valorConseguido para conservar compatibilidad con el historial.
    val valoresPorSerie: List<String> = emptyList(),
    // Modalidad WOD/CrossFit (wod, amrap, emom, for_time, tabata, death_by,
    // crossfit_lift). Cuando llega del banco, se renderiza con `WodInput`.
    val modalidad: String? = null,
    val scoreTipo: String? = null,
    val timeCapSeg: Int? = null
) {
    fun toEjercicioPlan(): EjercicioPlan {
        val parts = prescripcion.split("×", limit = 2)
        val series = parts.getOrNull(0)?.toIntOrNull() ?: 3
        val reps = parts.getOrNull(1)?.replace(Regex("[^0-9.]"), "")?.toDoubleOrNull() ?: 10.0
        return EjercicioPlan(
            id_ejercicio = idEjercicio,
            nombre = nombre,
            instrucciones_tecnicas = instrucciones_tecnicas,
            tipo_ilustracion = tipo_ilustracion,
            grupo_muscular = grupo_muscular,
            equipamiento = equipamiento,
            pilar = pilar,
            series = series,
            repeticiones = reps,
            descanso = descansoSeg
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntrenamientosScreen(
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onEntrenamientoFinalizado: (offerShare: Boolean) -> Unit,
    onNavigateToGps: (distKm: Double?) -> Unit = {},
    onNavigateToMisDispositivos: () -> Unit = {},
    initialEnfoque: String = "",
    initialPlanDiaId: Int? = null,
    initialRutinaOpoId: Int? = null,
    rutinasViewModel: RutinasViewModel = viewModel(),
    historialViewModel: HistorialViewModel = viewModel()
) {
    val authState by authViewModel.uiState.collectAsState()
    val rutinasState by rutinasViewModel.uiState.collectAsState()
    val historialState by historialViewModel.uiState.collectAsState()

    val userId = authState.userId ?: 0
    val oposicionId = authState.oposicionId ?: 1

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokenManager = remember { TokenManager(context) }
    var unitDist by remember { mutableStateOf("km") }
    var sessionStartMs by remember { mutableStateOf<Long?>(null) }
    var importingReloj by remember { mutableStateOf(false) }
    var mostrarAyudaReloj by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        tokenManager.getUnitDistancia().collectLatest { u ->
            if (!u.isNullOrBlank()) unitDist = u
        }
    }

    val timerState by SessionTimerTracker.state.collectAsState()
    val entrenoTimerActivo = timerState.label == ENTRENO_TIMER_LABEL
    var elapsedMs by remember { mutableStateOf(0L) }
    var cronometroActivo by remember { mutableStateOf(false) }
    var cronometroIniciadoAlgunaVez by remember { mutableStateOf(false) }

    LaunchedEffect(timerState.elapsedMs, timerState.active, timerState.paused, entrenoTimerActivo) {
        if (entrenoTimerActivo) {
            elapsedMs = timerState.elapsedMs
            cronometroActivo = timerState.active && !timerState.paused
            if (timerState.active) cronometroIniciadoAlgunaVez = true
        }
    }

    val ejerciciosEstado = remember { mutableStateListOf<EjercicioEstado>() }
    var selectedRutinaIndex by remember { mutableStateOf(0) }
    var initialApplied by remember { mutableStateOf(false) }
    var expandedRutinas by remember { mutableStateOf(false) }
    var showObjetivoDialog by remember { mutableStateOf(false) }
    var objetivoEjercicioNombre by remember { mutableStateOf<String?>(null) }
    var showRestTimer by remember { mutableStateOf(false) }
    var restTimerSecs by remember { mutableIntStateOf(90) }
    var nextEjercicioNombre by remember { mutableStateOf("") }
    var erroresValor by remember { mutableStateOf(mapOf<Int, String>()) }
    var avisoMsg by remember { mutableStateOf<String?>(null) }
    var gpsActividadUuid by remember { mutableStateOf<String?>(null) }
    var ejerciciosCatalogo by remember { mutableStateOf<Map<Int, Ejercicio>>(emptyMap()) }
    var ejercicioDetalle by remember { mutableStateOf<EjercicioPlan?>(null) }
    var prescripcionDetalle by remember { mutableStateOf("") }

    LaunchedEffect(userId) {
        if (userId <= 0) return@LaunchedEffect
        try {
            val token = tokenManager.getToken().first().orEmpty()
            if (token.isBlank()) return@LaunchedEffect
            val resp = RetrofitClient.ejerciciosApi.listarEjercicios("Bearer $token")
            if (resp.ok && resp.data != null) {
                ejerciciosCatalogo = resp.data.associateBy { it.id_ejercicio }
            }
        } catch (_: Exception) { }
    }

    fun mostrarDetalleEjercicio(estado: EjercicioEstado) {
        val cat = ejerciciosCatalogo[estado.idEjercicio]
        ejercicioDetalle = estado.toEjercicioPlan().copy(
            instrucciones_tecnicas = estado.instrucciones_tecnicas ?: cat?.instrucciones_tecnicas,
            grupo_muscular = estado.grupo_muscular ?: cat?.grupo_muscular,
            equipamiento = estado.equipamiento ?: cat?.equipamiento,
            tipo_ilustracion = estado.tipo_ilustracion ?: cat?.tipo_ilustracion,
            pilar = estado.pilar ?: cat?.pilar
        )
        prescripcionDetalle = estado.prescripcion
    }

    val rutinasSelector = remember(rutinasState.rutinaCompleta) {
        rutinasUnicasPorEnfoque(rutinasState.rutinaCompleta)
    }
    val enModoPlan = (initialPlanDiaId ?: 0) > 0
    val diaPlanSesion = remember(initialPlanDiaId, rutinasState.planSemanal) {
        initialPlanDiaId?.let { id ->
            rutinasState.planSemanal?.semana?.find { it.id_plan_dia == id }
        }
    }

    fun objetivoSegundosDesdeNombre(nombre: String): Int? {
        val regex = Regex("(\\d+)\\s*min\\b", RegexOption.IGNORE_CASE)
        val min = regex.find(nombre)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return null
        return min * 60
    }

    fun enfoqueActual(): String? =
        diaPlanSesion?.enfoque ?: rutinasSelector.getOrNull(selectedRutinaIndex)?.bloque

    fun esEjercicioGps(nombre: String, tipo: String?): Boolean {
        val n = nombre.lowercase()
        if (n.contains("cinta") || n.contains("tapiz") || n.contains("treadmill")) return false
        if (tipo == "RUN") return true
        return Regex("\\b(bici|ciclismo|bicicleta|caminar|marcha|paseo)\\b").containsMatchIn(n)
    }

    fun slotsParaImport(): List<EntrenoRelojImport.Slot> =
        ejerciciosEstado.mapIndexed { idx, e ->
            EntrenoRelojImport.Slot(
                index = idx,
                nombre = e.nombre,
                tipo = e.tipo,
                pilar = e.pilar,
                objetivoSegundos = e.objetivoSegundos,
                hecho = e.completado,
                valor = e.valorConseguido,
                distancia = e.distancia
            )
        }

    fun unidadEjercicioGlobal(idx: Int): String {
        val e = ejerciciosEstado.getOrNull(idx)
        val planUnidad = if (enModoPlan) {
            diaPlanSesion?.ejercicios?.getOrNull(idx)?.unidad
        } else null
        val apiUnidad = planUnidad?.ifBlank { null }
            ?: rutinasSelector.getOrNull(selectedRutinaIndex)?.ejercicios?.getOrNull(idx)?.unidad?.ifBlank { null }
        return EntrenoValidation.inferirUnidad(
            e?.nombre ?: "",
            apiUnidad,
            e?.pilar,
            enfoqueActual()
        )
    }

    fun aplicarImportReloj(result: EntrenoRelojImport.Result) {
        gpsActividadUuid = result.activityId
        if (result.elapsedMs > elapsedMs) {
            elapsedMs = result.elapsedMs
            cronometroIniciadoAlgunaVez = true
        }
        result.updates.forEach { u ->
            val e = ejerciciosEstado.getOrNull(u.index) ?: return@forEach
            ejerciciosEstado[u.index] = e.copy(
                valorConseguido = u.valor ?: e.valorConseguido,
                distancia = u.distancia ?: e.distancia,
                completado = u.hecho ?: e.completado
            )
        }
        avisoMsg = result.message
    }

    fun importarActividadReloj(activity: com.opofit.miapp.gps.model.ActivitySummary) {
        val result = EntrenoRelojImport.apply(
            activity = activity,
            slots = slotsParaImport(),
            unidadFor = { idx -> unidadEjercicioGlobal(idx) },
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

    LaunchedEffect(userId, oposicionId) {
        if (userId > 0 && rutinasState.rutinaCompleta.isEmpty()) {
            rutinasViewModel.cargarRutina(userId, oposicionId)
        }
    }

    LaunchedEffect(rutinasSelector, initialRutinaOpoId, initialEnfoque) {
        if (rutinasSelector.isEmpty()) return@LaunchedEffect
        when {
            initialRutinaOpoId != null && initialRutinaOpoId > 0 -> {
                val idx = rutinasSelector.indexOfFirst { it.id_rutina_opo == initialRutinaOpoId }
                if (idx >= 0) selectedRutinaIndex = idx
                initialApplied = true
            }
            !initialApplied && initialEnfoque.isNotBlank() -> {
                val idx = rutinasSelector.indexOfFirst { it.bloque.equals(initialEnfoque, ignoreCase = true) }
                if (idx >= 0) selectedRutinaIndex = idx
                initialApplied = true
            }
        }
        if (selectedRutinaIndex !in rutinasSelector.indices) selectedRutinaIndex = 0
    }

    LaunchedEffect(rutinasSelector) {
        if (rutinasSelector.isEmpty()) {
            ejerciciosEstado.clear()
            cronometroActivo = false
            elapsedMs = 0L
        }
    }

    LaunchedEffect(selectedRutinaIndex, rutinasSelector, initialPlanDiaId, rutinasState.planSemanal, enModoPlan) {
        ejerciciosEstado.clear()
        val diaPlan = if (enModoPlan) diaPlanSesion else null
        val enfoqueBloque = diaPlan?.enfoque ?: rutinasSelector.getOrNull(selectedRutinaIndex)?.bloque
        fun agregarEjercicio(
            nombre: String,
            idEjercicio: Int?,
            descanso: Int,
            idx: Int,
            pilar: String? = null,
            unidadApi: String? = null,
            series: Int? = null,
            repeticiones: Double? = null,
            instrucciones: String? = null,
            grupoMuscular: String? = null,
            equipamiento: String? = null,
            tipoIlustracion: String? = null,
            modalidad: String? = null,
            scoreTipo: String? = null,
            timeCapSeg: Int? = null
        ) {
            val pil = pilar ?: enfoqueBloque
            val objetivo = objetivoSegundosDesdeNombre(nombre)
            val tipo = EntrenoExerciseUtil.tipoCardio(nombre, pil, enfoqueBloque)
            val desc = if (descanso > 0) descanso else 90
            val unidadEff = EntrenoValidation.inferirUnidad(nombre, unidadApi, pil, enfoqueBloque)
            val valorInicial = when {
                tipo != null && unidadEff == "min" -> objetivo?.let { (it / 60.0).toString() } ?: ""
                else -> ""
            }
            val prescripcion = if (series != null && repeticiones != null) {
                "${series}×${PrescripcionFormat.formatRepeticiones(repeticiones, unidadApi, nombre)}"
            } else ""
            ejerciciosEstado.add(
                EjercicioEstado(
                    nombre = nombre,
                    idEjercicio = idEjercicio ?: (selectedRutinaIndex * 100 + idx + 1),
                    objetivoSegundos = objetivo,
                    tipo = tipo,
                    descansoSeg = desc,
                    pilar = pil,
                    valorConseguido = valorInicial,
                    prescripcion = prescripcion,
                    instrucciones_tecnicas = instrucciones,
                    grupo_muscular = grupoMuscular,
                    equipamiento = equipamiento,
                    tipo_ilustracion = tipoIlustracion,
                    modalidad = modalidad,
                    scoreTipo = scoreTipo,
                    timeCapSeg = timeCapSeg
                )
            )
        }
        val planEj = diaPlan?.ejercicios?.takeIf { it.isNotEmpty() }
        if (planEj != null) {
            planEj.forEachIndexed { idx, ej ->
                agregarEjercicio(
                    ej.nombre, ej.id_ejercicio, ej.descanso, idx, ej.pilar, ej.unidad,
                    series = ej.series, repeticiones = ej.repeticiones,
                    instrucciones = ej.instrucciones_tecnicas,
                    grupoMuscular = ej.grupo_muscular,
                    equipamiento = ej.equipamiento,
                    tipoIlustracion = ej.tipo_ilustracion,
                    modalidad = ej.modalidad,
                    scoreTipo = ej.score_tipo,
                    timeCapSeg = ej.time_cap_seg
                )
            }
        } else {
            rutinasSelector.getOrNull(selectedRutinaIndex)?.ejercicios?.forEachIndexed { idx, ej ->
                agregarEjercicio(
                    ej.nombre, ej.id_ejercicio, ej.descanso, idx, unidadApi = ej.unidad,
                    series = ej.series, repeticiones = ej.repeticiones
                )
            }
        }
        cronometroActivo = false
        elapsedMs = 0L
        cronometroIniciadoAlgunaVez = false
        showObjetivoDialog = false
        objetivoEjercicioNombre = null
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

    LaunchedEffect(cronometroActivo, cronometroIniciadoAlgunaVez) {
        if (cronometroActivo || cronometroIniciadoAlgunaVez) {
            com.opofit.miapp.gps.service.HrBleManager.get(context).autoConnectSavedDevice()
        }
    }

    LaunchedEffect(elapsedMs, cronometroActivo) {
        if (!cronometroActivo) return@LaunchedEffect
        val elapsedSec = (elapsedMs / 1000).toInt()
        val currentTimed = ejerciciosEstado.firstOrNull { !it.completado && it.objetivoSegundos != null }
        val obj = currentTimed?.objetivoSegundos
        if (obj != null && elapsedSec >= obj) {
            ChronoForegroundService.pause(context)
            objetivoEjercicioNombre = currentTimed.nombre
            showObjetivoDialog = true
        }
    }

    val gpsLast by GpsLastResult.value.collectAsState()
    LaunchedEffect(gpsLast?.id) {
        val summary = gpsLast ?: return@LaunchedEffect
        gpsActividadUuid = summary.id
        val idx = ejerciciosEstado.indexOfFirst { !it.completado && esEjercicioGps(it.nombre, it.tipo) }
        if (idx >= 0) {
            val km = summary.distanceM / 1000.0
            val mostrado = if (unitDist == "mi") "%.2f".format(km / 1.609344) else "%.2f".format(km)
            ejerciciosEstado[idx] = ejerciciosEstado[idx].copy(
                distancia = mostrado,
                valorConseguido = km.toString()
            )
            val gpsMs = summary.durationSec * 1000L
            if (elapsedMs < gpsMs) {
                elapsedMs = gpsMs
                cronometroIniciadoAlgunaVez = true
            }
        }
        GpsLastResult.consume()
    }

    fun prepararCompartirYFinalizar() {
        cronometroActivo = false
        val offerShare = historialState.ultimoEntreno?.let { entreno ->
            ShareActivityContext.set(
                buildPendingShareFromEntreno(
                    titulo = entreno.titulo,
                    idHistorial = entreno.idHistorial,
                    duracionMin = entreno.duracionMin,
                    ejerciciosCount = entreno.ejerciciosCount
                )
            )
            true
        } ?: false
        historialViewModel.resetRegistrado()
        onEntrenamientoFinalizado(offerShare)
    }

    LaunchedEffect(historialState.registradoExitoso, historialState.recordsRotos) {
        if (historialState.registradoExitoso && historialState.recordsRotos.isEmpty()) {
            prepararCompartirYFinalizar()
        }
    }

    RecordCelebrationDialog(
        records = historialState.recordsRotos,
        onDismiss = {
            if (historialState.registradoExitoso) {
                historialViewModel.clearRecordsCelebration()
                prepararCompartirYFinalizar()
            }
        },
        onShare = {
            // Botón "Compartir" en el diálogo de PR → empuja al editor de
            // share-card (Insta / WhatsApp / perfil) tras cerrar el diálogo.
            if (historialState.registradoExitoso) {
                historialViewModel.clearRecordsCelebration()
                prepararCompartirYFinalizar()
            }
        }
    )

    ExerciseDetailSheet(
        ejercicio = ejercicioDetalle,
        prescripcion = prescripcionDetalle,
        visible = ejercicioDetalle != null,
        onDismiss = { ejercicioDetalle = null }
    )

    RestTimerSheet(
        visible = showRestTimer,
        ejercicioNombre = nextEjercicioNombre,
        initialSeconds = restTimerSecs,
        onDismiss = { showRestTimer = false },
        onSkip = { showRestTimer = false }
    )

    val tiempoFormateado = TimeFormatUtil.formatElapsedMs(elapsedMs)

    val hayEjerciciosCompletadosGlobal by remember {
        derivedStateOf { ejerciciosEstado.any { it.completado } }
    }
    val valoresInvalidosGlobal by remember {
        derivedStateOf {
            ejerciciosEstado.withIndex().any { (i, e) ->
                if (!e.completado) return@any false
                val unidad = unidadEjercicioGlobal(i)
                // Validación básica (rango + número)
                if (EntrenoValidation.validarValor(e.valorConseguido, unidad) != null) return@any true
                // Validación lógica: tiempo no puede exceder duración del entreno
                if (EntrenoValidation.validarValorContraTiempoTotal(
                        e.valorConseguido, unidad, elapsedMs
                    ) != null) return@any true
                false
            }
        }
    }
    val puedaFinalizarGlobal by remember {
        derivedStateOf {
            hayEjerciciosCompletadosGlobal && cronometroIniciadoAlgunaVez && elapsedMs > 0L && !valoresInvalidosGlobal
        }
    }

    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
    val hrLive by com.opofit.miapp.gps.service.HrBleManager.get(context).heartRate.collectAsState()
    val hrState by com.opofit.miapp.gps.service.HrBleManager.get(context).state.collectAsState()
    val hrConnected = hrState is com.opofit.miapp.gps.service.HrBleManager.State.Connected

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Entrenamiento") },
                navigationIcon = {
                    IconButton(onClick = {
                        cronometroActivo = false
                        onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    // HR chip: muestra pulso en vivo si conectado, abre Mis Dispositivos si no.
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .background(
                                color = if (hrConnected) MaterialTheme.colorScheme.tertiaryContainer
                                else MaterialTheme.colorScheme.surface.copy(alpha = 0.25f),
                                shape = MaterialTheme.shapes.medium
                            )
                            .clickable { onNavigateToMisDispositivos() }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Filled.Favorite,
                            null,
                            tint = if (hrConnected) androidx.compose.ui.graphics.Color(0xFFE53935)
                            else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            hrLive?.let { "$it" } ?: if (hrConnected) "—" else "Conectar",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (hrConnected) MaterialTheme.colorScheme.onTertiaryContainer
                            else MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            if (ejerciciosEstado.isNotEmpty()) {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 12.dp,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (!hayEjerciciosCompletadosGlobal) {
                            Text(
                                "Marca al menos un ejercicio como completado para finalizar.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!cronometroIniciadoAlgunaVez || elapsedMs == 0L) {
                            Text(
                                "Inicia el cronómetro para poder finalizar.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (valoresInvalidosGlobal) {
                            Text(
                                "Corrige los valores en rojo antes de finalizar.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        if (historialState.isLoading) {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else {
                            Button(
                                onClick = {
                                    ChronoForegroundService.stop(context)
                                    cronometroActivo = false
                                    val realizados = ejerciciosEstado
                                        .filter { it.completado }
                                        .map { EjercicioRealizado(it.idEjercicio, it.valorConseguido.toDoubleOrNull() ?: 0.0) }
                                    val rutinaOpoId = diaPlanSesion?.id_rutina_opo
                                        ?: initialRutinaOpoId
                                        ?: rutinasSelector.getOrNull(selectedRutinaIndex)?.id_rutina_opo
                                        ?: 1
                                    val tituloRutina = diaPlanSesion?.titulo
                                        ?: initialEnfoque.ifBlank {
                                            rutinasSelector.getOrNull(selectedRutinaIndex)?.bloque?.let { enfoqueLabel(it) }
                                                ?: "Entrenamiento"
                                        }
                                    historialViewModel.registrarEntrenamiento(
                                        userId = userId,
                                        tipoRutina = "OPO",
                                        idRutina = rutinaOpoId,
                                        // ⚠ duracion EN SEGUNDOS (backend lo guarda así
                                        // y luego lo divide entre 60 para mostrar minutos).
                                        // Antes mandábamos minutos → backend lo dividía
                                        // otra vez y aparecía "0 min".
                                        duracion = ((elapsedMs / 1000L).toInt()).coerceAtLeast(1),
                                        ejercicios = realizados,
                                        gpsActividadUuid = gpsActividadUuid,
                                        tituloRutina = tituloRutina
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = puedaFinalizarGlobal
                            ) {
                                Text("🏁 Finalizar Entrenamiento")
                            }
                        }
                    }
                }
            }
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
                    Button(onClick = { showObjetivoDialog = false }) {
                        Text("¡Genial!")
                    }
                },
                title = { Text("Objetivo completado") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Has completado el objetivo de tiempo${objetivoEjercicioNombre?.let { ": $it" } ?: ""}.")
                        Text("Marca el ejercicio como completado e introduce la distancia para ver ritmo/velocidad.")
                    }
                }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Timer,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Tiempo",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Text(
                                text = tiempoFormateado,
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            if (enModoPlan && diaPlanSesion != null) {
                item {
                    val completados = ejerciciosEstado.count { it.completado }
                    PlanSesionActivaCard(
                        titulo = diaPlanSesion.titulo ?: enfoqueLabel(diaPlanSesion.enfoque),
                        nombreDia = diaPlanSesion.nombre_dia,
                        enfoque = diaPlanSesion.enfoque,
                        completados = completados,
                        total = ejerciciosEstado.size
                    )
                }
            } else if (rutinasSelector.isNotEmpty()) {
                item {
                    ExposedDropdownMenuBox(
                        expanded = expandedRutinas,
                        onExpandedChange = { expandedRutinas = it }
                    ) {
                        val bloqueSeleccionado = rutinasSelector.getOrNull(selectedRutinaIndex)
                        val label = bloqueSeleccionado?.bloque?.let { enfoqueLabel(it) } ?: "Selecciona rutina"
                        OutlinedTextField(
                            value = label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Rutina") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRutinas) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = expandedRutinas,
                            onDismissRequest = { expandedRutinas = false }
                        ) {
                            rutinasSelector.forEachIndexed { idx, b ->
                                DropdownMenuItem(
                                    text = { Text(enfoqueLabel(b.bloque)) },
                                    onClick = {
                                        selectedRutinaIndex = idx
                                        expandedRutinas = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (rutinasSelector.isNotEmpty() || enModoPlan) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                if (cronometroActivo) {
                                    ChronoForegroundService.pause(context)
                                } else {
                                    if (!entrenoTimerActivo) {
                                        SessionTimerTracker.start(ENTRENO_TIMER_LABEL, elapsedMs)
                                        ChronoForegroundService.start(context)
                                    } else {
                                        ChronoForegroundService.resume(context)
                                    }
                                    cronometroIniciadoAlgunaVez = true
                                    if (sessionStartMs == null) sessionStartMs = System.currentTimeMillis()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = ejerciciosEstado.isNotEmpty()
                        ) {
                            Text(if (cronometroActivo) "Pausar" else "Iniciar")
                        }
                        Button(
                            onClick = {
                                ChronoForegroundService.stop(context)
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

                item {
                    val planEjercicios = if (enModoPlan) {
                        diaPlanSesion?.ejercicios
                    } else {
                        rutinasSelector.getOrNull(selectedRutinaIndex)?.ejercicios?.map { ej ->
                            EjercicioPlan(
                                id_ejercicio = ej.id_ejercicio,
                                nombre = ej.nombre,
                                video_url = ej.video_url,
                                series = ej.series,
                                repeticiones = ej.repeticiones,
                                descanso = ej.descanso,
                                unidad = ej.unidad
                            )
                        }
                    }
                    if (!planEjercicios.isNullOrEmpty()) {
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Reloj", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    com.opofit.miapp.ui.components.InfoTip(
                                        title = "¿Cómo funciona con el reloj?",
                                        text = "Tres pasos para que tu reloj y OpoFit hablen entre sí:\n\n" +
                                            "1️⃣ ENVIAR PLAN: genera un archivo TCX/FIT con los ejercicios y lo envía a tu app del reloj (Garmin/Polar/Zepp). Tu reloj lo guarda como entreno guiado.\n\n" +
                                            "2️⃣ SINCRONIZAR: tras entrenar, baja los datos del reloj automáticamente vía Health Connect (distancia, tiempo, pulso medio).\n\n" +
                                            "3️⃣ ARCHIVO: si la sincro automática falla, exporta el archivo desde la app del reloj y súbelo aquí. Sirve para Garmin/Polar/Wikiloc."
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val steps = TcxExport.stepsFromEjercicios(planEjercicios)
                                            val titulo = diaPlanSesion?.titulo
                                                ?: rutinasSelector.getOrNull(selectedRutinaIndex)?.bloque?.let { enfoqueLabel(it) }
                                                ?: "Entreno OpoFit"
                                            val intent = TcxExport.shareIntent(context, titulo, steps)
                                            if (intent != null) {
                                                context.startActivity(
                                                    android.content.Intent.createChooser(intent, "Enviar al reloj")
                                                )
                                            } else {
                                                avisoMsg = "No se pudo generar el archivo para el reloj."
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                    ) {
                                        Icon(Icons.Filled.UploadFile, null, Modifier.size(16.dp))
                                        Spacer(Modifier.size(4.dp))
                                        Text(
                                            "Enviar",
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1
                                        )
                                    }
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
                                                        avisoMsg = "No hay actividades recientes. Conecta tu reloj en Menú → Conexiones y reloj."
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
                                        enabled = !importingReloj,
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                    ) {
                                        if (importingReloj) {
                                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                        } else {
                                            Icon(Icons.Filled.Sync, null, Modifier.size(16.dp))
                                        }
                                        Spacer(Modifier.size(4.dp))
                                        Text(
                                            "Sincro",
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = { importFileLauncher.launch("*/*") },
                                        enabled = !importingReloj,
                                        modifier = Modifier.weight(1f),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                    ) {
                                        // Icono de import (flecha hacia abajo / dentro), no export.
                                        Icon(Icons.Filled.FileDownload, null, Modifier.size(16.dp))
                                        Spacer(Modifier.size(4.dp))
                                        Text(
                                            "Subir",
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (ejerciciosEstado.isEmpty() && rutinasState.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Cargando rutina...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else if (ejerciciosEstado.isEmpty()) {
                item {
                    Text(
                        text = "No hay ejercicios en la rutina actual. Ve a 'Mis Rutinas' para ver tu rutina.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            }

            val pasoActualIdx = ejerciciosEstado.indexOfFirst { !it.completado }
            val activo = ejerciciosEstado.getOrNull(pasoActualIdx)

            fun unidadEjercicio(idx: Int): String {
                val e = ejerciciosEstado.getOrNull(idx)
                val planUnidad = if (enModoPlan) {
                    diaPlanSesion?.ejercicios?.getOrNull(idx)?.unidad
                } else null
                val apiUnidad = planUnidad?.ifBlank { null }
                    ?: rutinasSelector.getOrNull(selectedRutinaIndex)?.ejercicios?.getOrNull(idx)?.unidad?.ifBlank { null }
                return EntrenoValidation.inferirUnidad(
                    e?.nombre ?: "",
                    apiUnidad,
                    e?.pilar,
                    enfoqueActual()
                )
            }

            fun aplicarCronometro(idx: Int) {
                val e = ejerciciosEstado.getOrNull(idx) ?: return
                if (elapsedMs <= 0L) {
                    avisoMsg = "Inicia el cronómetro antes de aplicar el tiempo."
                    return
                }
                val unidadEff = unidadEjercicio(idx)
                val sec = TimeFormatUtil.secondsFromMs(elapsedMs)
                val nuevoValor = when {
                    e.tipo != null -> when {
                        e.objetivoSegundos != null || unidadEff == "min" -> "%.3f".format(sec / 60.0)
                        unidadEff == "s" -> "%.3f".format(sec)
                        else -> "%.3f".format(sec / 60.0)
                    }
                    unidadEff == "min" -> "%.3f".format(sec / 60.0)
                    unidadEff == "s" -> "%.3f".format(sec)
                    else -> e.valorConseguido
                }
                ejerciciosEstado[idx] = e.copy(valorConseguido = nuevoValor)
                val err = EntrenoValidation.validarValor(nuevoValor, unidadEff)
                erroresValor = if (err == null) erroresValor - idx else erroresValor + (idx to err)
                if (err == null) {
                    avisoMsg = "Tiempo del cronómetro aplicado (${TimeFormatUtil.formatElapsedMs(elapsedMs)})."
                }
            }

            fun marcarCompletado(idx: Int) {
                var estado = ejerciciosEstado.getOrNull(idx) ?: return
                if (estado.completado) return
                if (elapsedMs > 0L && estado.valorConseguido.isBlank()) {
                    val unidadEff = unidadEjercicio(idx)
                    if (estado.tipo != null || unidadEff == "min" || unidadEff == "s") {
                        aplicarCronometro(idx)
                        estado = ejerciciosEstado.getOrNull(idx) ?: return
                    }
                }
                // Validación lógica (estilo Strong/Hevy): si tiene N series
                // declaradas, exigimos que TODAS estén rellenas antes de
                // saltar al siguiente ejercicio. Permite ahorrar un click
                // con "Igualar todas las series" del SeriesInput.
                val prescParts = estado.prescripcion.split("×", limit = 2)
                val seriesObj = prescParts.getOrNull(0)?.toIntOrNull() ?: 1
                if (seriesObj > 1 && estado.tipo == null) {
                    val seriesRellenas = estado.valoresPorSerie.count { it.isNotBlank() }
                    if (seriesRellenas < seriesObj) {
                        avisoMsg = "Anota las $seriesObj series antes de completar el ejercicio (llevas $seriesRellenas)."
                        return
                    }
                }
                ejerciciosEstado[idx] = estado.copy(completado = true)
                val nextIdx = idx + 1
                if (nextIdx < ejerciciosEstado.size) {
                    restTimerSecs = estado.descansoSeg.coerceIn(30, 300)
                    nextEjercicioNombre = ejerciciosEstado[nextIdx].nombre
                    showRestTimer = true
                }
            }

            if (cronometroActivo || cronometroIniciadoAlgunaVez) {
                item {
                    val distKm = activo?.distancia?.replace(",", ".")?.toDoubleOrNull()?.let { d ->
                        if (unitDist == "mi" && activo?.tipo == "RUN") Units.miToKm(d) else d
                    }
                    EntrenoLiveMetricsBar(
                        elapsedMs = elapsedMs,
                        cronometroActivo = cronometroActivo || cronometroIniciadoAlgunaVez,
                        distanciaKm = distKm,
                        tipoCardio = activo?.tipo
                    )
                }
            }

            if (activo != null) {
                item {
                    val unidadEff = unidadEjercicio(pasoActualIdx)
                    val secsCalc = activo.objetivoSegundos?.let { minOf(TimeFormatUtil.secondsFromMs(elapsedMs), it.toDouble()) }
                        ?: TimeFormatUtil.secondsFromMs(elapsedMs)
                    val (ritmoTxt, velTxt) = ritmoVelocidadTexto(activo.tipo, activo.distancia, secsCalc)
                    val labelDist = when (activo.tipo) {
                        "SWIM" -> if (unitDist == "mi") "Distancia (yd)" else "Distancia (m)"
                        "RUN" -> if (unitDist == "mi") "Distancia (mi)" else "Distancia (km)"
                        else -> "Distancia"
                    }
                    // Parseamos "S×R" de la prescripción para mostrar input por serie.
                    val prescParts = activo.prescripcion.split("×", limit = 2)
                    val seriesObjetivo = prescParts.getOrNull(0)?.toIntOrNull() ?: 1
                    val repsObjetivo = prescParts.getOrNull(1)
                        ?.replace(Regex("[^0-9]"), "")
                        ?.toIntOrNull()
                    EntrenoActiveStepCard(
                        paso = pasoActualIdx + 1,
                        total = ejerciciosEstado.size,
                        nombre = activo.nombre,
                        objetivoSegundos = activo.objetivoSegundos,
                        tipoCardio = activo.tipo,
                        unidad = unidadEff,
                        valor = activo.valorConseguido,
                        distancia = activo.distancia,
                        elapsedMsCronometro = elapsedMs,
                        seriesObjetivo = seriesObjetivo,
                        repsObjetivo = repsObjetivo,
                        valoresPorSerie = activo.valoresPorSerie,
                        onValoresPorSerieChange = { nuevos ->
                            val csv = valoresPorSerieToCsv(nuevos)
                            ejerciciosEstado[pasoActualIdx] = activo.copy(
                                valoresPorSerie = nuevos,
                                valorConseguido = csv
                            )
                            // Validamos cada serie con las reglas habituales + reglas
                            // lógicas (tiempo de un set no puede superar el cronómetro).
                            val errs = nuevos.mapNotNull { v ->
                                if (v.isBlank()) null
                                else EntrenoValidation.validarValor(v, unidadEff)
                                    ?: EntrenoValidation.validarValorContraTiempoTotal(v, unidadEff, elapsedMs)
                            }
                            val err = errs.firstOrNull()
                            erroresValor = if (err == null) erroresValor - pasoActualIdx
                                else erroresValor + (pasoActualIdx to err)
                        },
                        onValorChange = { v ->
                            ejerciciosEstado[pasoActualIdx] = activo.copy(valorConseguido = v)
                            val err = EntrenoValidation.validarValor(v, unidadEff)
                                ?: EntrenoValidation.validarValorContraTiempoTotal(v, unidadEff, elapsedMs)
                            erroresValor = if (err == null) erroresValor - pasoActualIdx else erroresValor + (pasoActualIdx to err)
                        },
                        onDistanciaChange = { v ->
                            val baseValue = when (activo.tipo) {
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
                            ejerciciosEstado[pasoActualIdx] = activo.copy(distancia = v, valorConseguido = baseValue)
                        },
                        onUsarCronometro = { aplicarCronometro(pasoActualIdx) },
                        onCompletar = { marcarCompletado(pasoActualIdx) },
                        onGps = if (esEjercicioGps(activo.nombre, activo.tipo)) {
                            {
                                val kmPlan = MapaEntrenoNav.distanciaKmDesdeTexto(activo.nombre)
                                    ?: activo.distancia.replace(",", ".").toDoubleOrNull()
                                    ?: MapaEntrenoNav.distanciaKmDesdeTexto(activo.valorConseguido)
                                onNavigateToGps(kmPlan)
                            }
                        } else null,
                        errorValor = erroresValor[pasoActualIdx],
                        ritmoTexto = ritmoTxt,
                        velocidadTexto = velTxt,
                        labelDistancia = labelDist,
                        onInfoClick = { mostrarDetalleEjercicio(activo) },
                        modalidad = activo.modalidad,
                        scoreTipo = activo.scoreTipo,
                        timeCapSeg = activo.timeCapSeg
                    )
                }
            }

            val completados = ejerciciosEstado.withIndex().filter { it.value.completado }
            if (completados.isNotEmpty()) {
                item {
                    Text(
                        "Completados (${completados.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            itemsIndexed(ejerciciosEstado) { index, estado ->
                if (!estado.completado && index == pasoActualIdx) return@itemsIndexed
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = estado.completado,
                            onCheckedChange = { checked ->
                                if (checked) marcarCompletado(index)
                                else ejerciciosEstado[index] = estado.copy(completado = false)
                            }
                        )
                        Column(Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (estado.prescripcion.isNotBlank()) {
                                    androidx.compose.material3.Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                    ) {
                                        Text(
                                            estado.prescripcion,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Text(
                                    estado.nombre,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (estado.completado) FontWeight.Normal else FontWeight.Medium,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }
                            if (estado.completado && estado.valorConseguido.isNotBlank()) {
                                // Mostrar el valor CON UNIDAD legible (ej: "49 seg" en vez de
                                // solo "49" para plancha). El usuario reportaba confusión.
                                // Si hay valores por serie (estilo Strong/Hevy), resumimos:
                                //   - todas iguales → "4×12 reps"
                                //   - distintas → "media 10 reps (8–12)"
                                val unidadEj = unidadEjercicio(index)
                                val resumen = if (estado.valoresPorSerie.size > 1) {
                                    resumenSeries(estado.valorConseguido, unidadEj)
                                } else {
                                    "${estado.valorConseguido} ${EntrenoValidation.unidadLegible(unidadEj)}"
                                }
                                Text(
                                    "Resultado: $resumen",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        ExerciseInfoButton(
                            onClick = { mostrarDetalleEjercicio(estado) },
                            size = 36.dp
                        )
                    }
                }
            }

            if (historialState.error.isNotEmpty()) {
                item {
                    Text(
                        text = historialState.error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            item {
                val hintsVisibles = listOf(
                    !hayEjerciciosCompletadosGlobal,
                    !cronometroIniciadoAlgunaVez || elapsedMs == 0L,
                    valoresInvalidosGlobal
                ).count { it }
                Spacer(modifier = Modifier.height((160 + hintsVisibles * 22).dp))
            }
        }
    }
    com.opofit.miapp.ui.components.CoachMarkOverlay(
        screenKey = "entrenamiento_v1",
        steps = listOf(
            com.opofit.miapp.ui.components.CoachStep(
                title = "Tu entreno paso a paso",
                text = "Cada ejercicio aparece como una tarjeta. Pon series, reps y peso, luego dale al check para marcarlo como hecho."
            ),
            com.opofit.miapp.ui.components.CoachStep(
                title = "Cronómetro general arriba",
                text = "Mide cuánto tiempo llevas entrenando. Si registras peso en kg/reps, no necesita estar activo, pero ayuda para llevar la cuenta."
            ),
            com.opofit.miapp.ui.components.CoachStep(
                title = "Botón finalizar abajo",
                text = "Cuando termines, pulsa «Finalizar entrenamiento». Si dejas valores vacíos, la app te avisa."
            )
        )
    )
    }
}
