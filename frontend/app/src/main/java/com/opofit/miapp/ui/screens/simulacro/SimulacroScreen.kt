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
    var guardando by remember { mutableStateOf(false) }
    var notaFinal by remember { mutableStateOf<String?>(null) }
    var detalleNotas by remember { mutableStateOf<List<Pair<String, Int?>>>(emptyList()) }

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
                error.isNotBlank() -> Text(error, color = MaterialTheme.colorScheme.error)
                notaFinal != null -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Resultado del simulacro", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Nota media estimada: $notaFinal / 10", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                    detalleNotas.forEach { (nombre, nota) ->
                        Text("$nombre → ${nota?.toString() ?: "—"} pts")
                    }
                    Button(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth()) { Text("Volver") }
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
                                        onValueChange = { valores[pruebaActual.id_pruebas_oficiales] = it },
                                        label = { Text("Tiempo (segundos)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                    )
                                } else {
                                    OutlinedTextField(
                                        value = valores[pruebaActual.id_pruebas_oficiales] ?: "",
                                        onValueChange = { valores[pruebaActual.id_pruebas_oficiales] = it },
                                        label = { Text(if (pruebaActual.unidad == "s") "Tiempo (segundos)" else "Repeticiones conseguidas") },
                                        modifier = Modifier.fillMaxWidth(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
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
                                    if (v == null) {
                                        error = "Introduce un valor válido"
                                        return@Button
                                    }
                                    valores[id] = v.toString()
                                    error = ""
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
                                                    detalleNotas = resp.data.detalle.map { d ->
                                                        val nombre = pruebas.find { it.id_pruebas_oficiales == d.id_prueba }?.nombre_prueba ?: "Prueba"
                                                        nombre to d.nota
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
            if (guardando) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
    }
}
