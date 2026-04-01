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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opofit.miapp.data.local.TokenManager
import com.opofit.miapp.data.responsemodels.EjercicioRealizado
import com.opofit.miapp.ui.viewmodels.AuthViewModel
import com.opofit.miapp.ui.viewmodels.HistorialViewModel
import com.opofit.miapp.ui.viewmodels.RutinasViewModel
import com.opofit.miapp.utils.Units
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.roundToInt

private data class EjercicioEstado(
    val nombre: String,
    val idEjercicio: Int,
    var completado: Boolean = false,
    var valorConseguido: String = "",
    val objetivoSegundos: Int? = null,
    val tipo: String? = null, 
    var distancia: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntrenamientosScreen(
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onEntrenamientoFinalizado: () -> Unit,
    initialEnfoque: String = "",
    rutinasViewModel: RutinasViewModel = viewModel(),
    historialViewModel: HistorialViewModel = viewModel()
) {
    val authState by authViewModel.uiState.collectAsState()
    val rutinasState by rutinasViewModel.uiState.collectAsState()
    val historialState by historialViewModel.uiState.collectAsState()

    val userId = authState.userId ?: 0
    val oposicionId = authState.oposicionId ?: 1

    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    var unitDist by remember { mutableStateOf("km") }
    LaunchedEffect(Unit) {
        tokenManager.getUnitDistancia().collectLatest { u ->
            if (!u.isNullOrBlank()) unitDist = u
        }
    }

    var segundos by remember { mutableStateOf(0) }
    var cronometroActivo by remember { mutableStateOf(false) }
    var cronometroIniciadoAlgunaVez by remember { mutableStateOf(false) }

    val ejerciciosEstado = remember { mutableStateListOf<EjercicioEstado>() }
    var selectedRutinaIndex by remember { mutableStateOf(0) }
    var initialApplied by remember { mutableStateOf(false) }
    var expandedRutinas by remember { mutableStateOf(false) }
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

    LaunchedEffect(userId, oposicionId) {
        if (userId > 0 && rutinasState.rutinaCompleta.isEmpty()) {
            rutinasViewModel.cargarRutina(userId, oposicionId)
        }
    }

    LaunchedEffect(rutinasState.rutinaCompleta) {
        if (!initialApplied && initialEnfoque.isNotBlank() && rutinasState.rutinaCompleta.isNotEmpty()) {
            val idx = rutinasState.rutinaCompleta.indexOfFirst { it.bloque.equals(initialEnfoque, ignoreCase = true) }
            if (idx >= 0) {
                selectedRutinaIndex = idx
            }
            initialApplied = true
        }
        if (rutinasState.rutinaCompleta.isNotEmpty() && selectedRutinaIndex !in rutinasState.rutinaCompleta.indices) {
            selectedRutinaIndex = 0
        }
        if (rutinasState.rutinaCompleta.isEmpty()) {
            ejerciciosEstado.clear()
            cronometroActivo = false
            segundos = 0
        }
    }

    LaunchedEffect(selectedRutinaIndex, rutinasState.rutinaCompleta) {
        ejerciciosEstado.clear()
        val bloque = rutinasState.rutinaCompleta.getOrNull(selectedRutinaIndex)
        if (bloque != null) {
            bloque.ejercicios.forEachIndexed { idx, ejercicio ->
                val objetivo = objetivoSegundosDesdeNombre(ejercicio.nombre)
                val tipo = tipoCardio(ejercicio.nombre)
                ejerciciosEstado.add(
                    EjercicioEstado(
                        nombre = ejercicio.nombre,
                        idEjercicio = ejercicio.id_ejercicio ?: (selectedRutinaIndex * 100 + idx + 1),
                        objetivoSegundos = objetivo,
                        tipo = tipo
                    )
                )
            }
        }
        cronometroActivo = false
        segundos = 0
        cronometroIniciadoAlgunaVez = false
        showObjetivoDialog = false
        objetivoEjercicioNombre = null
    }

    fun formatTime(totalSeconds: Int): String {
        val mm = totalSeconds / 60
        val ss = totalSeconds % 60
        return "%02d:%02d".format(mm, ss)
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

    LaunchedEffect(cronometroActivo) {
        while (cronometroActivo) {
            delay(1000L)
            segundos++
            val currentTimed = ejerciciosEstado.firstOrNull { !it.completado && it.objetivoSegundos != null }
            val obj = currentTimed?.objetivoSegundos
            if (obj != null && segundos >= obj && cronometroActivo) {
                cronometroActivo = false
                objetivoEjercicioNombre = currentTimed.nombre
                showObjetivoDialog = true
            }
        }
    }

    LaunchedEffect(historialState.registradoExitoso) {
        if (historialState.registradoExitoso) {
            cronometroActivo = false
            historialViewModel.resetRegistrado()
            onEntrenamientoFinalizado()
        }
    }

    val minutos = segundos / 60
    val segs = segundos % 60
    val tiempoFormateado = "%02d:%02d".format(minutos, segs)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Entrenamiento") },
                navigationIcon = {
                    IconButton(onClick = {
                        cronometroActivo = false
                        onNavigateBack()
                    }) {
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
                    Button(onClick = { showObjetivoDialog = false }) {
                        Text("¡Genial!")
                    }
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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "⏱ Tiempo",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
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

            if (rutinasState.rutinaCompleta.isNotEmpty()) {
                item {
                    ExposedDropdownMenuBox(
                        expanded = expandedRutinas,
                        onExpandedChange = { expandedRutinas = it }
                    ) {
                        val bloqueSeleccionado = rutinasState.rutinaCompleta.getOrNull(selectedRutinaIndex)
                        val label = bloqueSeleccionado?.bloque ?: "Selecciona rutina"
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
                            rutinasState.rutinaCompleta.forEachIndexed { idx, b ->
                                val name = b.bloque
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        selectedRutinaIndex = idx
                                        expandedRutinas = false
                                    }
                                )
                            }
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
                            enabled = ejerciciosEstado.isNotEmpty()
                        ) {
                            Text(if (cronometroActivo) "Pausar" else "Iniciar")
                        }
                        Button(
                            onClick = { cronometroActivo = false; segundos = 0; cronometroIniciadoAlgunaVez = false },
                            modifier = Modifier.weight(1f),
                            enabled = segundos > 0
                        ) {
                            Text("Reiniciar")
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

            itemsIndexed(ejerciciosEstado) { index, estado ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = estado.completado,
                                onCheckedChange = { checked ->
                                    ejerciciosEstado[index] = estado.copy(completado = checked)
                                }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = estado.nombre,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            if (estado.objetivoSegundos != null && estado.tipo != null) {
                                    Text(
                                        text = "Objetivo: ${formatTime(estado.objetivoSegundos)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                    if (estado.tipo != null) {
                        val secsCalc = estado.objetivoSegundos?.let { minOf(segundos, it) } ?: segundos
                        val (ritmoTxt, velTxt) = ritmoVelocidadTexto(estado.tipo, estado.distancia, secsCalc)
                        val labelDist = when (estado.tipo) {
                            "SWIM" -> if (unitDist == "mi") "Distancia (yd)" else "Distancia (m)"
                            "RUN" -> if (unitDist == "mi") "Distancia (mi)" else "Distancia (km)"
                            else -> "Distancia"
                        }
                            OutlinedTextField(
                                value = estado.distancia,
                                onValueChange = { v ->
                                val baseValue = when (estado.tipo) {
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
                                val next = estado.copy(distancia = v, valorConseguido = baseValue)
                                ejerciciosEstado[index] = next
                                },
                                label = { Text(labelDist) },
                                placeholder = { Text(if (estado.tipo == "SWIM") "Ej: 1500" else "Ej: 7.20") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            supportingText = {
                                if (segundos <= 0) {
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
                        } else {
                            val unidad = rutinasState.rutinaCompleta
                                .getOrNull(selectedRutinaIndex)
                                ?.ejercicios
                                ?.getOrNull(index)
                                ?.unidad
                                ?.ifBlank { null }

                            OutlinedTextField(
                                value = estado.valorConseguido,
                                onValueChange = { v ->
                                    ejerciciosEstado[index] = estado.copy(valorConseguido = v)
                                },
                                label = { Text(if (unidad != null) "Valor ($unidad)" else "Valor") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true
                            )
                        }
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
                Spacer(modifier = Modifier.height(8.dp))
                if (historialState.isLoading) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    val hayEjerciciosCompletados = ejerciciosEstado.any { it.completado }
                    val puedeFinalizar = hayEjerciciosCompletados && cronometroIniciadoAlgunaVez && segundos > 0
                    if (ejerciciosEstado.isNotEmpty() && !hayEjerciciosCompletados) {
                        Text(
                            text = "Marca al menos un ejercicio como completado para finalizar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    if (!cronometroIniciadoAlgunaVez || segundos == 0) {
                        Text(
                            text = "Inicia el cronómetro para poder registrar el entrenamiento.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    Button(
                        onClick = {
                            cronometroActivo = false
                            val realizados = ejerciciosEstado
                                .filter { it.completado }
                                .map { EjercicioRealizado(it.idEjercicio, it.valorConseguido.toDoubleOrNull() ?: 0.0) }
                            val rutinaOpoId = rutinasState.rutinaCompleta.getOrNull(selectedRutinaIndex)?.id_rutina_opo ?: 1
                            historialViewModel.registrarEntrenamiento(
                                userId = userId,
                                tipoRutina = "OPO",
                                idRutina = rutinaOpoId,
                                duracion = segundos,
                                ejercicios = realizados
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = puedeFinalizar
                    ) {
                        Text("🏁 Finalizar Entrenamiento")
                    }
                }
            }
        }
    }
}
