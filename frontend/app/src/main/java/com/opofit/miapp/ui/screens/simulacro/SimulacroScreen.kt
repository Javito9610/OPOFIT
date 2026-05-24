package com.opofit.miapp.ui.screens.simulacro

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.opofit.miapp.ui.viewmodels.AuthViewModel
import com.opofit.miapp.utils.ApiErrorParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulacroScreen(
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onNavigatePremium: () -> Unit
) {
    val authState by authViewModel.uiState.collectAsState()
    val oposicionId = authState.oposicionId ?: 1
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val scope = rememberCoroutineScope()

    var pruebas by remember { mutableStateOf<List<PruebaOficialSimulacro>>(emptyList()) }
    var paso by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
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

    var cronometroActivo by remember { mutableStateOf(false) }
    var segundos by remember { mutableIntStateOf(0) }
    val valores = remember { mutableStateMapOf<Int, String>() }

    LaunchedEffect(oposicionId) {
        loading = true
        error = ""
        try {
            val token = tokenManager.getToken().first() ?: ""
            val resp = RetrofitClient.simulacroApi.listarPruebas("Bearer $token", oposicionId)
            if (resp.ok && resp.data != null) {
                pruebas = resp.data
                paso = 0
            } else {
                error = resp.msg ?: "No se pudieron cargar las pruebas"
            }
        } catch (e: Exception) {
            error = ApiErrorParser.message(e)
        } finally {
            loading = false
        }
    }

    LaunchedEffect(cronometroActivo) {
        while (cronometroActivo) {
            delay(1000)
            segundos++
        }
    }

    fun formatTime(s: Int) = "%02d:%02d".format(s / 60, s % 60)

    val pruebaActual = pruebas.getOrNull(paso)
    val esTiempo = pruebaActual?.unidad == "s"
    fun etiquetaEntrada(p: PruebaOficialSimulacro?) =
        p?.unidadEtiqueta?.let { if (it == "segundos") "Tiempo (segundos)" else "Repeticiones" }
            ?: if (p?.unidad == "s") "Tiempo (segundos)" else "Repeticiones conseguidas"
    val hayAptoNoApto = pruebas.any { it.tipo_baremo == "APTO_NO_APTO" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Simulacro oficial") },
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
                    Text("Nota media estimada: $notaFinal / 10", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                    perfilTrasSimulacro?.let { p ->
                        if (p.subirNivel) {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
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
                    if (perfilTrasSimulacro?.hayMejoras == true) {
                        Button(
                            onClick = { mostrarDialogoPerfil = true },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Actualizar marcas en mi perfil") }
                    }
                    OutlinedButton(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth()) { Text("Volver") }
                }
                pruebas.isEmpty() -> Text(
                    "Tu oposición no tiene pruebas físicas deportivas en convocatoria (p. ej. Ayudante de Instituciones Penitenciarias). Consulta la ficha de tu oposición."
                )
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (hayAptoNoApto) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Text(
                                    "Oficialmente estas pruebas se califican Apto / No Apto (BOE). La nota 0-10 es orientativa para entrenar.",
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodySmall
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
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(pruebaActual!!.nombre_prueba, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                pruebaActual.descripcion?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                                pruebaActual.convocatoria_ref?.let {
                                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (esTiempo) {
                                    Text("Modo cronómetro", fontWeight = FontWeight.SemiBold)
                                    Text(formatTime(segundos), style = MaterialTheme.typography.displayMedium)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { cronometroActivo = !cronometroActivo }) {
                                            Text(if (cronometroActivo) "Parar" else "Iniciar")
                                        }
                                        OutlinedButton(onClick = { cronometroActivo = false; segundos = 0 }) {
                                            Text("Reset")
                                        }
                                    }
                                    OutlinedTextField(
                                        value = valores[pruebaActual.id_pruebas_oficiales] ?: segundos.toString(),
                                        onValueChange = {
                                            valores[pruebaActual.id_pruebas_oficiales] = it
                                            errorCampo = ""
                                        },
                                        label = { Text(etiquetaEntrada(pruebaActual)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        isError = errorCampo.isNotEmpty(),
                                        supportingText = if (errorCampo.isNotEmpty()) {
                                            { Text(errorCampo, color = MaterialTheme.colorScheme.error) }
                                        } else null
                                    )
                                } else {
                                    OutlinedTextField(
                                        value = valores[pruebaActual.id_pruebas_oficiales] ?: "",
                                        onValueChange = {
                                            valores[pruebaActual.id_pruebas_oficiales] = it
                                            errorCampo = ""
                                        },
                                        label = { Text(etiquetaEntrada(pruebaActual)) },
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
                                        paso--
                                        cronometroActivo = false
                                        segundos = 0
                                        errorCampo = ""
                                    },
                                    modifier = Modifier.weight(1f)
                                ) { Text("Anterior") }
                            }
                            Button(
                                onClick = {
                                    val id = pruebaActual!!.id_pruebas_oficiales
                                    val v = if (esTiempo) {
                                        (valores[id]?.toDoubleOrNull() ?: segundos.toDouble())
                                    } else {
                                        valores[id]?.toDoubleOrNull()
                                    }
                                    if (v == null || v < 0) {
                                        errorCampo = if (esTiempo) {
                                            "Indica el tiempo en segundos (usa el cronómetro o escribe el valor)"
                                        } else {
                                            "Indica cuántas repeticiones has conseguido"
                                        }
                                        return@Button
                                    }
                                    valores[id] = v.toString()
                                    errorCampo = ""
                                    cronometroActivo = false
                                    segundos = 0
                                    if (paso < pruebas.size - 1) {
                                        paso++
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
                                                    if (resp.data.perfil?.hayMejoras == true) {
                                                        mostrarDialogoPerfil = true
                                                    }
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
                    Text("Has superado marcas del simulacro respecto a tu perfil:")
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
                                    mensajeExito = resp.msg ?: "Perfil actualizado correctamente"
                                    authViewModel.refreshSessionFromBackend()
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
}
