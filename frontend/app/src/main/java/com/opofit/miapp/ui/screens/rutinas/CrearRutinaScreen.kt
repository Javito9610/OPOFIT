package com.opofit.miapp.ui.screens.rutinas

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.opofit.miapp.data.api.RetrofitClient
import com.opofit.miapp.data.local.TokenManager
import com.opofit.miapp.data.responsemodels.Ejercicio
import com.opofit.miapp.data.responsemodels.EjercicioLibreItem
import com.opofit.miapp.data.responsemodels.EjercicioPlan
import com.opofit.miapp.data.responsemodels.EntornoEntrenoOpcion
import com.opofit.miapp.data.responsemodels.toEjercicioPlan
import com.opofit.miapp.ui.components.ElevatedCard
import com.opofit.miapp.ui.components.ExerciseDetailSheet
import com.opofit.miapp.ui.utils.isCompactScreen
import com.opofit.miapp.ui.viewmodels.AuthViewModel
import com.opofit.miapp.ui.viewmodels.RutinasLibresViewModel
import kotlinx.coroutines.flow.first

private data class EjercicioFormRow(
    val idEjercicio: Int? = null,
    val nombreEjercicio: String = "",
    val grupoMuscular: String = "",
    val pilar: String? = null,
    val series: String = "",
    val repeticiones: String = "",
    val descansoSeg: Int? = null
)

private fun esCardioContinuo(nombre: String): Boolean {
    val n = nombre.lowercase()
    val esCardio = n.contains("carrera") || n.contains("trote") || n.contains("rodaje") || n.contains("fartlek") ||
        n.contains("natación") || n.contains("natacion") || n.contains("nadar")
    if (!esCardio) return false
    val tieneMin = Regex("(\\d+)\\s*min\\b", RegexOption.IGNORE_CASE).containsMatchIn(nombre)
    val tieneContinuo = n.contains("continu")
    val mencionaSeries = n.contains("series") || n.contains("x ")
    return (tieneMin || tieneContinuo) && !mencionaSeries
}

private fun tipoEntradaEjercicio(nombre: String): Pair<String, String> {
    val n = nombre.lowercase()
    return when {
        n.contains("carrera") || n.contains("rodaje") || n.contains("fartlek") -> "Duración" to "min"
        n.contains("series") && (n.contains("m") || n.contains("400") || n.contains("200")) -> "Distancia" to "m"
        n.contains("natación") || n.contains("natacion") -> "Distancia" to "m"
        n.contains("plancha") || n.contains("suspensión") || n.contains("suspension") -> "Tiempo" to "s"
        else -> "Repeticiones" to "reps"
    }
}

private fun sugerirSeriesReps(nombre: String, pilar: String?): Pair<String, String> {
    if (esCardioContinuo(nombre)) {
        val m = Regex("(\\d+)\\s*min", RegexOption.IGNORE_CASE).find(nombre)?.groupValues?.get(1) ?: "20"
        return "1" to m
    }
    if (pilar == "RESISTENCIA" || pilar == "VELOCIDAD") return "1" to "20"
    return "3" to "10"
}

private fun descansoSugerido(nombre: String, pilar: String?, entorno: String?): Int {
    if (esCardioContinuo(nombre) || pilar == "RESISTENCIA" || pilar == "VELOCIDAD") return 0
    val n = nombre.lowercase()
    val ent = entorno?.uppercase()
    if (ent == "CROSSFIT") return 60
    if (n.contains("dominada") || n.contains("sentadilla") || n.contains("peso muerto") || n.contains("press banca")) return 120
    if (n.contains("curl") || n.contains("extensión") || n.contains("extension")) return 60
    return 90
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CrearRutinaScreen(
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    rutinasLibresViewModel: RutinasLibresViewModel = viewModel()
) {
    val authState by authViewModel.uiState.collectAsState()
    val uiState by rutinasLibresViewModel.uiState.collectAsState()
    val compact = isCompactScreen()
    val padH = if (compact) 12.dp else 16.dp

    val userId = authState.userId ?: 0

    var nombreRutina by remember { mutableStateOf("") }
    val ejercicios = remember { mutableStateListOf<EjercicioFormRow>() }

    var entornoSeleccionado by remember { mutableStateOf<String?>(null) }
    var entornosOpciones by remember { mutableStateOf<List<EntornoEntrenoOpcion>>(emptyList()) }
    var pasoEnfoque by remember { mutableStateOf(true) }

    var ejerciciosDisponibles by remember { mutableStateOf<List<Ejercicio>>(emptyList()) }
    var cargandoEjercicios by remember { mutableStateOf(false) }
    var errorCargaEjercicios by remember { mutableStateOf("") }
    var filtroPilar by remember { mutableStateOf<String?>(null) }
    var errorIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var ejercicioDetalleBanco by remember { mutableStateOf<EjercicioPlan?>(null) }
    var prescripcionDetalleBanco by remember { mutableStateOf("") }
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }

    ExerciseDetailSheet(
        ejercicio = ejercicioDetalleBanco,
        prescripcion = prescripcionDetalleBanco,
        visible = ejercicioDetalleBanco != null,
        onDismiss = { ejercicioDetalleBanco = null }
    )

    LaunchedEffect(Unit) {
        try {
            val token = tokenManager.getToken().first() ?: ""
            val entornos = RetrofitClient.planesApi.getEntornos("Bearer $token")
            entornosOpciones = entornos.data.orEmpty().filter { it.id != "MIXTO" && it.id != "PISTA" }
            val usuario = RetrofitClient.planesApi.getEntornoUsuario("Bearer $token")
            entornoSeleccionado = usuario.data?.entorno?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            entornosOpciones = listOf(
                EntornoEntrenoOpcion("GYM", "Gimnasio", "🏋️", "Máquinas, barras y mancuernas"),
                EntornoEntrenoOpcion("CASA", "En casa", "🏠", "Mínimo material"),
                EntornoEntrenoOpcion("CROSSFIT", "CrossFit / Box", "🏋️‍♀️", "Cajones y kettlebells"),
                EntornoEntrenoOpcion("CALISTENIA", "Parque calistenia", "🤸", "Barras y peso corporal")
            )
        }
    }

    LaunchedEffect(entornoSeleccionado, filtroPilar, pasoEnfoque) {
        val ent = entornoSeleccionado ?: return@LaunchedEffect
        if (pasoEnfoque) return@LaunchedEffect
        cargandoEjercicios = true
        try {
            val token = tokenManager.getToken().first() ?: ""
            val response = RetrofitClient.ejerciciosApi.listarEjercicios(
                "Bearer $token",
                pilar = filtroPilar,
                entorno = ent
            )
            if (response.ok && response.data != null) {
                ejerciciosDisponibles = response.data
                errorCargaEjercicios = ""
            } else {
                errorCargaEjercicios = "No se pudieron cargar los ejercicios"
            }
        } catch (e: Exception) {
            errorCargaEjercicios = "Error al cargar ejercicios: ${e.message ?: "Error de conexión"}"
        } finally {
            cargandoEjercicios = false
        }
    }

    LaunchedEffect(uiState.guardadoExitoso) {
        if (uiState.guardadoExitoso) {
            rutinasLibresViewModel.resetGuardado()
            onNavigateBack()
        }
    }

    fun anadirEjercicio(ej: Ejercicio) {
        if (ejercicios.any { it.idEjercicio == ej.id_ejercicio }) return
        val (s, r) = sugerirSeriesReps(ej.nombre, ej.pilar)
        val desc = descansoSugerido(ej.nombre, ej.pilar, entornoSeleccionado)
        ejercicios.add(
            EjercicioFormRow(
                idEjercicio = ej.id_ejercicio,
                nombreEjercicio = ej.nombre,
                grupoMuscular = ej.grupo_muscular.orEmpty(),
                pilar = ej.pilar,
                series = s,
                repeticiones = r,
                descansoSeg = desc
            )
        )
    }

    val entornoLabel = entornosOpciones.find { it.id == entornoSeleccionado }?.etiqueta
        ?: entornoSeleccionado.orEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (pasoEnfoque) "¿Dónde entrenas?" else "Nueva rutina libre") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!pasoEnfoque) pasoEnfoque = true else onNavigateBack()
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
        if (pasoEnfoque) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(padH),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Elige el entorno para adaptar el banco de ejercicios a tu material.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    entornosOpciones.forEach { op ->
                        FilterChip(
                            selected = entornoSeleccionado == op.id,
                            onClick = { entornoSeleccionado = op.id },
                            label = { Text("${op.emoji ?: ""} ${op.etiqueta}") }
                        )
                    }
                }
                entornoSeleccionado?.let { id ->
                    entornosOpciones.find { it.id == id }?.descripcion?.let { desc ->
                        Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { if (entornoSeleccionado != null) pasoEnfoque = false },
                    enabled = entornoSeleccionado != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Continuar")
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = padH),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Entorno", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text(entornoLabel, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(onClick = { pasoEnfoque = true }) { Text("Cambiar") }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = nombreRutina,
                    onValueChange = { nombreRutina = it },
                    label = { Text("Nombre de la rutina") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                Text(
                    text = "Banco de ejercicios (${ejerciciosDisponibles.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "Abre un grupo muscular y pulsa + para añadir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        null to "Todos",
                        "FUERZA" to "Fuerza",
                        "RESISTENCIA" to "Resistencia",
                        "VELOCIDAD" to "Velocidad"
                    ).forEach { (pilar, label) ->
                        FilterChip(
                            selected = filtroPilar == pilar,
                            onClick = { filtroPilar = pilar },
                            label = { Text(label, maxLines = 1, softWrap = false) }
                        )
                    }
                }
                if (cargandoEjercicios) {
                    CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                }
                if (errorCargaEjercicios.isNotEmpty()) {
                    Text(errorCargaEjercicios, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }

            item {
                BancoEjerciciosPorGrupo(
                    ejercicios = ejerciciosDisponibles,
                    idsYaAnadidos = ejercicios.mapNotNull { it.idEjercicio }.toSet(),
                    onSeleccionar = { anadirEjercicio(it) },
                    onVerDetalle = { ej ->
                        ejercicioDetalleBanco = ej.toEjercicioPlan()
                        prescripcionDetalleBanco = ""
                    }
                )
            }

            if (ejercicios.isEmpty()) {
                item {
                    Text(
                        "Aún no has añadido ejercicios. Expande un grupo muscular arriba.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            itemsIndexed(ejercicios) { index, row ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Ejercicio ${index + 1}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(row.nombreEjercicio, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                if (row.grupoMuscular.isNotBlank()) {
                                    Text(row.grupoMuscular, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            IconButton(onClick = { ejercicios.removeAt(index) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        val (labelReps, unit) = tipoEntradaEjercicio(row.nombreEjercicio)
                        if (compact) {
                            if (!esCardioContinuo(row.nombreEjercicio)) {
                                OutlinedTextField(
                                    value = row.series,
                                    onValueChange = { ejercicios[index] = row.copy(series = it) },
                                    label = { Text("Series") },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                            }
                            OutlinedTextField(
                                value = row.repeticiones,
                                onValueChange = {
                                    ejercicios[index] = row.copy(repeticiones = it)
                                    if (index in errorIndices) errorIndices = errorIndices - index
                                },
                                label = { Text("$labelReps ($unit)") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                isError = index in errorIndices,
                                supportingText = if (index in errorIndices) {
                                    { Text("Introduce un valor para este ejercicio") }
                                } else null
                            )
                        } else {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (!esCardioContinuo(row.nombreEjercicio)) {
                                    OutlinedTextField(
                                        value = row.series,
                                        onValueChange = { ejercicios[index] = row.copy(series = it) },
                                        label = { Text("Series") },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true
                                    )
                                }
                                OutlinedTextField(
                                    value = row.repeticiones,
                                    onValueChange = {
                                        ejercicios[index] = row.copy(repeticiones = it)
                                        if (index in errorIndices) errorIndices = errorIndices - index
                                    },
                                    label = { Text("$labelReps ($unit)") },
                                    modifier = if (esCardioContinuo(row.nombreEjercicio)) Modifier.fillMaxWidth() else Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    isError = index in errorIndices,
                                    supportingText = if (index in errorIndices) {
                                        { Text("Introduce un valor para este ejercicio") }
                                    } else null
                                )
                            }
                        }
                        row.descansoSeg?.takeIf { it > 0 }?.let { d ->
                            Text(
                                "Descanso sugerido: ${d}s (se guardará al crear la rutina)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            if (uiState.error.isNotEmpty()) {
                item {
                    Text(uiState.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    val itemsValidos = ejercicios.mapNotNull { row ->
                        val id = row.idEjercicio ?: return@mapNotNull null
                        val s = row.series.toIntOrNull()?.takeIf { it > 0 } ?: 1
                        val r = row.repeticiones.toIntOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
                        EjercicioLibreItem(id, s, r, row.descansoSeg)
                    }
                    val tieneNombre = nombreRutina.trim().isNotEmpty()
                    Button(
                        enabled = tieneNombre && itemsValidos.isNotEmpty(),
                        onClick = {
                            val invalidos = ejercicios.indices.filter { i ->
                                val r = ejercicios[i]
                                r.idEjercicio != null &&
                                    (r.repeticiones.toIntOrNull()?.takeIf { it > 0 } == null)
                            }.toSet()
                            errorIndices = invalidos
                            if (invalidos.isNotEmpty() || itemsValidos.isEmpty()) return@Button
                            rutinasLibresViewModel.crearRutina(
                                userId,
                                nombreRutina.trim(),
                                itemsValidos,
                                entornoSeleccionado
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Guardar rutina")
                    }
                    val msgValidacion = when {
                        !tieneNombre -> "Ponle un nombre a tu rutina."
                        errorIndices.isNotEmpty() -> "Corrige los ejercicios marcados en rojo."
                        itemsValidos.isEmpty() -> "Añade al menos un ejercicio desde el banco."
                        else -> ""
                    }
                    if (msgValidacion.isNotEmpty()) {
                        Text(
                            msgValidacion,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (errorIndices.isNotEmpty()) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth()) {
                        Text("Cancelar")
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
