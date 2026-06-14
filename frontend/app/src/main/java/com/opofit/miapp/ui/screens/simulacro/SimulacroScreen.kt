package com.opofit.miapp.ui.screens.simulacro

import com.opofit.miapp.ui.components.ElevatedCard
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.opofit.miapp.data.api.RetrofitClient
import com.opofit.miapp.data.local.TokenManager
import com.opofit.miapp.data.responsemodels.GuardarSimulacroRequest
import com.opofit.miapp.data.responsemodels.PerfilTrasSimulacro
import com.opofit.miapp.data.responsemodels.PruebaOficialSimulacro
import com.opofit.miapp.data.responsemodels.ResultadoSimulacroItem
import com.opofit.miapp.data.responsemodels.SimulacroHistorialItem
import com.opofit.miapp.ui.viewmodels.AuthViewModel
import com.opofit.miapp.ui.viewmodels.SimulacroViewModel
import com.opofit.miapp.utils.ApiErrorParser
import com.opofit.miapp.utils.AppEvents
import com.opofit.miapp.utils.TimeFormatUtil
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulacroScreen(
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onNavigatePremium: () -> Unit,
    simulacroViewModel: SimulacroViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val authState by authViewModel.uiState.collectAsState()
    val oposicionId = authState.oposicionId ?: 1
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val scope = rememberCoroutineScope()

    // ViewModel holds timer + values so the simulacro survives navigation away and back.
    val vmState by simulacroViewModel.state.collectAsState()
    val pruebas = vmState.pruebas
    val paso = vmState.paso
    val loading = vmState.loading
    val cronometroActivo = vmState.cronometroActivo
    val elapsedMs = vmState.elapsedMs
    val valores = vmState.valores
    val marcasPreCargadas = vmState.marcasPreCargadas

    // Local state: validation, result display, dialogs (post-completion only).
    var error by remember { mutableStateOf("") }
    var errorCampo by remember { mutableStateOf("") }
    var guardando by remember { mutableStateOf(false) }
    var notaFinal by remember { mutableStateOf<String?>(null) }
    var detalleNotas by remember { mutableStateOf<List<Pair<String, Int?>>>(emptyList()) }
    var perfilTrasSimulacro by remember { mutableStateOf<PerfilTrasSimulacro?>(null) }
    var ultimosResultados by remember { mutableStateOf<List<ResultadoSimulacroItem>>(emptyList()) }
    var mostrarDialogoPerfil by remember { mutableStateOf(false) }
    var aplicandoMarcas by remember { mutableStateOf(false) }
    var mensajeExito by remember { mutableStateOf<String?>(null) }
    var historialSimulacros by remember { mutableStateOf<List<SimulacroHistorialItem>>(emptyList()) }
    var mostrarHistorial by remember { mutableStateOf(false) }

    val userId = authState.userId ?: 0

    LaunchedEffect(oposicionId) {
        simulacroViewModel.cargarPruebas(oposicionId)
        // Load history separately (premium-gated).
        try {
            val token = tokenManager.getToken().first() ?: ""
            val histResp = RetrofitClient.simulacroApi.historial("Bearer $token", oposicionId)
            if (histResp.ok && histResp.data != null) historialSimulacros = histResp.data
        } catch (_: Exception) {}
    }

    // Pre-fill values from the user's profile marks once pruebas are loaded.
    LaunchedEffect(pruebas.size, userId) {
        if (pruebas.isNotEmpty() && userId > 0) {
            simulacroViewModel.precargarMarcas(userId, oposicionId)
        }
    }

    // Propagate ViewModel load errors to local error state.
    LaunchedEffect(vmState.error) {
        if (vmState.error.isNotBlank()) error = vmState.error
    }

    // Reset per-step timer and validation when step changes.
    LaunchedEffect(paso, pruebas.size) {
        errorCampo = ""
        val p = pruebas.getOrNull(paso) ?: return@LaunchedEffect
        if (p.unidad == "s") {
            val savedMs = valores[p.id_pruebas_oficiales]?.toDoubleOrNull()
                ?.let { TimeFormatUtil.msFromSeconds(it) } ?: 0L
            simulacroViewModel.setElapsedMs(savedMs)
        } else {
            simulacroViewModel.resetTimer()
        }
    }

    val pruebaActual = pruebas.getOrNull(paso)
    val esTiempo = pruebaActual?.unidad == "s"
    fun etiquetaEntrada(p: PruebaOficialSimulacro?) =
        p?.unidadEtiqueta?.let { if (it == "segundos") "Tiempo (segundos)" else "Repeticiones" }
            ?: if (p?.unidad == "s") "Tiempo (segundos)" else "Repeticiones conseguidas"
    val hayAptoNoApto = pruebas.any { it.tipo_baremo == "APTO_NO_APTO" }

    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Simulacro oficial") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    com.opofit.miapp.ui.components.InfoTip(
                        title = "¿Qué es el simulacro?",
                        text = "Reproduce las pruebas físicas oficiales de tu oposición tal cual te las harán el día del examen.\n\n" +
                            "Por cada prueba, registras tu marca (tiempo o reps). La app calcula tu nota con el baremo oficial.\n\n" +
                            "Al terminar puedes:\n" +
                            "• Guardar el simulacro en historial.\n" +
                            "• Aplicar las marcas a tu PERFIL si mejoraste (actualiza ranking).\n" +
                            "• Compartir el resultado.\n\n" +
                            "El cronómetro funciona en 2º plano: puedes salir de la app sin que se pare."
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            when {
                loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                error.isNotBlank() -> Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = onNavigateBack) { Text("Volver") }
                }
                notaFinal != null -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Resultado del simulacro", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Nota del simulacro: $notaFinal / 10", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                    Text(
                        "Esta nota es de esta sesión. La nota oficial del perfil solo cambia si actualizas tus marcas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    perfilTrasSimulacro?.let { p ->
                        if (p.subirNivel) {
                            ElevatedCard() {
                                Text(
                                    "¡Has mejorado! Nivel proyectado: ${p.nivelTrasSimulacro} (antes ${p.nivelActual})",
                                    modifier = Modifier.padding(12.dp),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    detalleNotas.forEach { (nombre, nota) ->
                        Text("$nombre → ${nota?.toString() ?: "—"} pts")
                    }
                    mensajeExito?.let { msg ->
                        Text(msg, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                    }
                    Button(
                        onClick = { mostrarDialogoPerfil = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (perfilTrasSimulacro?.hayMejoras == true) {
                                "Actualizar marcas en mi perfil"
                            } else {
                                "Revisar marcas del simulacro"
                            }
                        )
                    }
                    OutlinedButton(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth()) { Text("Volver") }
                }
                pruebas.isEmpty() -> Text(
                    "Tu oposición no tiene pruebas físicas deportivas en convocatoria (p. ej. Ayudante de Instituciones Penitenciarias). Consulta la ficha de tu oposición."
                )
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Historial de simulacros anteriores (solo cuando no ha empezado aún)
                    if (paso == 0 && historialSimulacros.isNotEmpty()) {
                        item {
                            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Simulacros anteriores",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        TextButton(onClick = { mostrarHistorial = !mostrarHistorial }) {
                                            Text(if (mostrarHistorial) "Ocultar" else "Ver todos")
                                        }
                                    }
                                    val itemsAMostrar = if (mostrarHistorial) historialSimulacros else historialSimulacros.take(3)
                                    itemsAMostrar.forEach { sim ->
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                sim.fecha.take(10),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                sim.nota_media?.let { "$it / 10" } ?: "—",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (hayAptoNoApto) {
                        item {
                            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    "Oficialmente estas pruebas se califican Apto / No Apto (BOE). La nota 0-10 es orientativa para entrenar.",
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                    if (paso == 0 && marcasPreCargadas && valores.isNotEmpty()) {
                        item {
                            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    "Los campos se han rellenado con tus mejores marcas del perfil. Actualiza los que hayan cambiado.",
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    item {
                        LinearProgressIndicator(
                            progress = { (paso + 1f) / pruebas.size },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "Prueba ${paso + 1} de ${pruebas.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    item {
                        // Guard: si por race condition pruebas cambia y paso queda
                        // fuera de rango, no crasheamos — simplemente no pintamos.
                        val actual = pruebaActual ?: return@item
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(actual.nombre_prueba, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                actual.descripcion?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                                actual.convocatoria_ref?.let {
                                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (esTiempo) {
                                    val idPrueba = actual.id_pruebas_oficiales
                                    Text("Modo cronómetro", fontWeight = FontWeight.SemiBold)
                                    Text(TimeFormatUtil.formatElapsedMs(elapsedMs), style = MaterialTheme.typography.displayMedium)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = {
                                            if (cronometroActivo) {
                                                simulacroViewModel.stopTimer()
                                                simulacroViewModel.setValor(idPrueba, "%.3f".format(TimeFormatUtil.secondsFromMs(elapsedMs)))
                                                errorCampo = ""
                                            } else {
                                                simulacroViewModel.startTimer()
                                            }
                                        }) {
                                            Text(if (cronometroActivo) "Parar" else "Iniciar")
                                        }
                                        OutlinedButton(onClick = {
                                            simulacroViewModel.stopTimer()
                                            simulacroViewModel.resetTimer()
                                            simulacroViewModel.removeValor(idPrueba)
                                            errorCampo = ""
                                        }) {
                                            Text("Reset")
                                        }
                                    }
                                    Text(
                                        "Al pulsar Siguiente se usará el tiempo del cronómetro (${TimeFormatUtil.formatSecondsValue(TimeFormatUtil.secondsFromMs(elapsedMs))}).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (errorCampo.isNotEmpty()) {
                                        Text(
                                            errorCampo,
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                } else {
                                    OutlinedTextField(
                                        value = valores[actual.id_pruebas_oficiales] ?: "",
                                        onValueChange = { v ->
                                            simulacroViewModel.setValor(actual.id_pruebas_oficiales, v)
                                            errorCampo = ""
                                        },
                                        label = { Text(etiquetaEntrada(actual)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        isError = errorCampo.isNotEmpty(),
                                        supportingText = if (errorCampo.isNotEmpty()) {
                                            { Text(errorCampo, color = MaterialTheme.colorScheme.error) }
                                        } else null
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (paso > 0) {
                                OutlinedButton(
                                    onClick = {
                                        simulacroViewModel.setPaso(paso - 1)
                                        errorCampo = ""
                                    },
                                    modifier = Modifier.weight(1f)
                                ) { Text("Anterior") }
                            }
                            Button(
                                onClick = {
                                    val id = pruebaActual?.id_pruebas_oficiales ?: return@Button
                                    val v = if (esTiempo) {
                                        simulacroViewModel.stopTimer()
                                        if (elapsedMs > 0L) {
                                            val sec = TimeFormatUtil.secondsFromMs(elapsedMs)
                                            simulacroViewModel.setValor(id, "%.3f".format(sec))
                                            sec
                                        } else null
                                    } else {
                                        valores[id]?.toDoubleOrNull()
                                    }
                                    if (v == null || v < 0) {
                                        errorCampo = if (esTiempo) {
                                            "Usa el cronómetro (Iniciar → Parar) y luego pulsa Siguiente"
                                        } else {
                                            "Indica cuántas repeticiones has conseguido"
                                        }
                                        return@Button
                                    }
                                    simulacroViewModel.setValor(id, if (esTiempo) "%.3f".format(v) else v.toString())
                                    errorCampo = ""
                                    simulacroViewModel.stopTimer()
                                    simulacroViewModel.resetTimer()
                                    if (paso < pruebas.size - 1) {
                                        simulacroViewModel.setPaso(paso + 1)
                                    } else {
                                        guardando = true
                                        scope.launch {
                                            try {
                                                val token = tokenManager.getToken().first() ?: ""
                                                val resultados = pruebas.mapNotNull { p ->
                                                    val valStr = valores[p.id_pruebas_oficiales] ?: return@mapNotNull null
                                                    val num = valStr.toDoubleOrNull() ?: return@mapNotNull null
                                                    ResultadoSimulacroItem(p.id_pruebas_oficiales, num)
                                                }
                                                val resp = RetrofitClient.simulacroApi.guardar(
                                                    "Bearer $token",
                                                    GuardarSimulacroRequest(oposicionId, resultados)
                                                )
                                                if (resp.ok && resp.data != null) {
                                                    notaFinal = resp.data.notaMedia
                                                    detalleNotas = (resp.data.detalle ?: emptyList()).map { d ->
                                                        val nombre = pruebas.find { it.id_pruebas_oficiales == d.id_prueba }?.nombre_prueba ?: "Prueba"
                                                        nombre to d.nota
                                                    }
                                                    perfilTrasSimulacro = resp.data.perfil
                                                    ultimosResultados = resultados
                                                    historialSimulacros = listOf(
                                                        SimulacroHistorialItem(
                                                            id_simulacro = resp.data.idSimulacro ?: 0,
                                                            nota_media = resp.data.notaMedia,
                                                            fecha = java.time.Instant.now().toString()
                                                        )
                                                    ) + historialSimulacros
                                                    mostrarDialogoPerfil = true
                                                } else {
                                                    error = resp.msg ?: "Error al guardar"
                                                }
                                            } catch (e: Exception) {
                                                error = ApiErrorParser.message(e)
                                            } finally {
                                                guardando = false
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !guardando
                            ) {
                                Text(if (paso < pruebas.size - 1) "Siguiente" else "Ver nota final")
                            }
                        }
                    }
                }
            }
            if (guardando || aplicandoMarcas) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
    }

    if (mostrarDialogoPerfil && perfilTrasSimulacro != null) {
        val p = perfilTrasSimulacro!!
        AlertDialog(
            onDismissRequest = { mostrarDialogoPerfil = false },
            title = { Text("¿Actualizar tu perfil?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (p.hayMejoras) {
                            "Has superado marcas del simulacro respecto a tu perfil:"
                        } else {
                            "Puedes guardar las marcas de este simulacro en tu perfil (solo se aplican mejoras):"
                        }
                    )
                    p.mejoras?.forEach { m ->
                        val unidad = if (m.unidad == "s") "s" else "rep"
                        val ant = m.valorAnterior?.let { String.format("%.2f", it) } ?: "—"
                        Text("• ${m.nombrePrueba}: $ant → ${String.format("%.2f", m.valorNuevo)} $unidad (nota ${m.notaNueva})")
                    }
                    if (p.subirNivel) {
                        Text(
                            "Tu nivel pasaría a ${p.nivelTrasSimulacro} (nota media ${p.notaMediaTrasSimulacro}).",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        aplicandoMarcas = true
                        scope.launch {
                            try {
                                val token = tokenManager.getToken().first() ?: ""
                                val resp = RetrofitClient.simulacroApi.aplicarMarcas(
                                    "Bearer $token",
                                    GuardarSimulacroRequest(oposicionId, ultimosResultados)
                                )
                                if (resp.ok) {
                                    resp.data?.perfil?.let { perfilTrasSimulacro = it }
                                    mensajeExito = (resp.msg ?: "Perfil actualizado correctamente") +
                                        " El ranking se actualizará con tus nuevas marcas."
                                    authViewModel.refreshSessionFromBackend()
                                    AppEvents.signalHomeRefresh()
                                    mostrarDialogoPerfil = false
                                } else {
                                    error = resp.msg ?: "No se pudieron actualizar las marcas"
                                }
                            } catch (e: Exception) {
                                error = ApiErrorParser.message(e)
                            } finally {
                                aplicandoMarcas = false
                            }
                        }
                    }
                ) { Text("Sí, actualizar perfil") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoPerfil = false }) { Text("Ahora no") }
            }
        )
    }
    com.opofit.miapp.ui.components.CoachMarkOverlay(
        screenKey = "simulacro_v1",
        steps = listOf(
            com.opofit.miapp.ui.components.CoachStep(
                title = "Simulacro oficial",
                text = "Vas a hacer las pruebas físicas de tu oposición tal cual te las harán. Una a una, con cronómetro real."
            ),
            com.opofit.miapp.ui.components.CoachStep(
                title = "Cronómetro automático",
                text = "Para pruebas de tiempo (carrera, plancha), pulsa Iniciar y el cronómetro corre solo en segundo plano. Puedes salir de la app sin que se pare."
            ),
            com.opofit.miapp.ui.components.CoachStep(
                title = "Tu nota se calcula sola",
                text = "Al terminar verás tu nota media. Si supera la del perfil, te ofrece actualizar tus marcas. Queda guardado en historial."
            )
        )
    )
    }
}
