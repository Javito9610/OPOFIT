package com.opofit.miapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opofit.miapp.data.responsemodels.EjercicioPlan
import com.opofit.miapp.utils.EntrenoExerciseUtil

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExerciseDetailSheet(
    ejercicio: EjercicioPlan?,
    prescripcion: String,
    visible: Boolean,
    onDismiss: () -> Unit
) {
    if (!visible || ejercicio == null) return

    val grupo = inferGrupoMuscular(ejercicio.nombre, ejercicio.grupo_muscular, ejercicio.pilar)
    val tipoLabel = tipoEjercicioLabel(ejercicio.tipo_ilustracion, ejercicio.nombre, ejercicio.pilar)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // contentWindowInsets a 0 → el container del sheet llega al borde inferior
    // real (no deja franja blanca de la gesture bar). El Column de dentro usa
    // navigationBarsPadding() para que el contenido no quede tapado.
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // === HERO VISUAL DEL EJERCICIO ===
            // Si el backend tiene animacion_url o video_url con un GIF,
            // lo cargamos IN-LINE con Coil (soporte GIF activado en el
            // ImageLoader global). El usuario decía "yo quiero que salga
            // directamente, no que abra el navegador" — ahora se ve aquí.
            // Fallback: icono grande sobre gradient brand.
            val ctxLocal = androidx.compose.ui.platform.LocalContext.current
            val animUrl = ejercicio.animacion_url
                ?.takeIf { it.startsWith("http") }
                ?: ejercicio.video_url?.takeIf { it.startsWith("http") && (it.endsWith(".gif") || it.endsWith(".png") || it.endsWith(".jpg") || it.endsWith(".webp")) }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = if (animUrl != null) 220.dp else 140.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primary
            ) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        )
                        .padding(if (animUrl != null) 0.dp else 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (animUrl != null) {
                        // Animación / GIF in-line del banco de ejercicios.
                        coil.compose.AsyncImage(
                            model = animUrl,
                            contentDescription = ejercicio.nombre,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 220.dp),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                EnfoqueIcons.forEnfoque(ejercicio.pilar),
                                contentDescription = ejercicio.nombre,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(72.dp)
                            )
                            Text(
                                tipoLabel.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Nombre del ejercicio bajo el hero.
            Text(
                ejercicio.nombre,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // === BOTONES VISUALES: VÍDEO + ANIMACIÓN ===
            // El usuario pidió ver el ejercicio (vídeo o GIF). Aquí 2 CTAs
            // GRANDES, lado a lado, sin necesidad de scroll para encontrarlos.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Vídeo YouTube (rojo de marca YouTube).
                androidx.compose.material3.Button(
                    onClick = {
                        val q = java.net.URLEncoder.encode(
                            "${ejercicio.nombre} técnica correcta",
                            "UTF-8"
                        )
                        com.opofit.miapp.utils.UrlOpener.open(
                            ctxLocal,
                            "https://www.youtube.com/results?search_query=$q"
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFFCC0000),
                        contentColor = androidx.compose.ui.graphics.Color.White
                    )
                ) {
                    Icon(Icons.Outlined.Speed, null, Modifier.size(20.dp))
                    androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
                    Text(
                        "Vídeo",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                // Animación / GIF (azul Wger, banco gratuito de ejercicios
                // con animaciones de muñecos).
                androidx.compose.material3.OutlinedButton(
                    onClick = {
                        val q = java.net.URLEncoder.encode(ejercicio.nombre, "UTF-8")
                        com.opofit.miapp.utils.UrlOpener.open(
                            ctxLocal,
                            "https://wger.de/en/exercise/overview/?term=$q"
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Outlined.Timer, null, Modifier.size(20.dp))
                    androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
                    Text(
                        "Animación",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            ) {
                Column(
                    Modifier
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f)
                                )
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Prescripción",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        prescripcion,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    val proMeta = buildList {
                        if (ejercicio.descanso > 0) add("${ejercicio.descanso}s descanso")
                        ejercicio.tempo?.takeIf { it.isNotBlank() }?.let { add("Tempo $it") }
                        ejercicio.rpe_objetivo?.let { add("RPE objetivo $it") }
                        ejercicio.rango_rm?.takeIf { it.isNotBlank() }?.let { add(it) }
                        ejercicio.nota_carga?.takeIf { it.isNotBlank() }?.let { add(it) }
                    }
                    if (proMeta.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            proMeta.forEach { line ->
                                AssistChip(
                                    onClick = {},
                                    enabled = false,
                                    label = { Text(line, style = MaterialTheme.typography.labelSmall) },
                                    leadingIcon = when {
                                        line.contains("descanso", ignoreCase = true) -> {
                                            { Icon(Icons.Outlined.Timer, null, Modifier.size(16.dp)) }
                                        }
                                        line.contains("RPE", ignoreCase = true) -> {
                                            { Icon(Icons.Outlined.Speed, null, Modifier.size(16.dp)) }
                                        }
                                        line.contains("Tempo", ignoreCase = true) -> {
                                            { Icon(Icons.AutoMirrored.Outlined.TrendingUp, null, Modifier.size(16.dp)) }
                                        }
                                        else -> null
                                    }
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            // Tabs en lugar de scroll vertical infinito. El usuario decía
            // "es una mierda, tanto scroll". Ahora 3 pestañas: cada una cabe
            // en una pantalla.
            //   • TÉCNICA — setup + ejecución + cues + errores
            //   • POR QUÉ — porque + objetivo
            //   • DETALLES — chips + sustitución + ajuste + regresión/progresión
            val tabSelState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
            val tabSel = tabSelState.intValue
            val exp = ejercicio.explicacion
            androidx.compose.material3.TabRow(selectedTabIndex = tabSel) {
                androidx.compose.material3.Tab(
                    selected = tabSel == 0, onClick = { tabSelState.intValue = 0 },
                    text = { Text("Técnica", style = MaterialTheme.typography.labelLarge) }
                )
                androidx.compose.material3.Tab(
                    selected = tabSel == 1, onClick = { tabSelState.intValue = 1 },
                    text = { Text("Por qué", style = MaterialTheme.typography.labelLarge) }
                )
                androidx.compose.material3.Tab(
                    selected = tabSel == 2, onClick = { tabSelState.intValue = 2 },
                    text = { Text("Detalles", style = MaterialTheme.typography.labelLarge) }
                )
            }

            Column(
                Modifier
                    .heightIn(min = 200.dp, max = 360.dp)
                    .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (tabSel) {
                    0 -> {
                        if (exp != null) {
                            CoachSection("Setup", exp.setup, MaterialTheme.colorScheme.primary)
                            CoachSection("Ejecución", exp.ejecucion, MaterialTheme.colorScheme.tertiary)
                            if (exp.coaching_cues.isNotEmpty()) {
                                CoachSectionLista("Claves del entrenador", exp.coaching_cues, MaterialTheme.colorScheme.primary)
                            }
                            if (exp.errores_comunes.isNotEmpty()) {
                                CoachSectionLista("Errores a evitar", exp.errores_comunes, MaterialTheme.colorScheme.error)
                            }
                        } else {
                            Text(
                                EntrenoExerciseUtil.deduplicarInstrucciones(ejercicio.instrucciones_tecnicas)
                                    ?.takeIf { it.isNotBlank() }
                                    ?: "Ejecuta el movimiento con técnica controlada y progresión según tu nivel.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    1 -> {
                        exp?.porque?.takeIf { it.isNotBlank() }?.let { por ->
                            CoachSection("Por qué entrenas esto", por, MaterialTheme.colorScheme.secondary)
                        }
                        ejercicio.objetivo?.takeIf { it.isNotBlank() }?.let { obj ->
                            Text(
                                "Objetivo: $obj",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        ejercicio.regresion?.let { reg ->
                            Text(
                                "↘ Regresión: ${reg.nombre}${reg.motivo?.let { " — $it" } ?: ""}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        ejercicio.progresion?.let { prog ->
                            Text(
                                "↗ Progresión: ${prog.nombre}${prog.motivo?.let { " — $it" } ?: ""}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    else -> {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (grupo != "General") {
                                AssistChip(onClick = {}, label = { Text(grupo) }, enabled = false)
                            }
                            AssistChip(onClick = {}, label = { Text(tipoLabel) }, enabled = false)
                            ejercicio.pilar?.takeIf { it.isNotBlank() }?.let { pilar ->
                                AssistChip(onClick = {}, label = { Text(pilar) }, enabled = false)
                            }
                            ejercicio.equipamiento?.takeIf { it.isNotBlank() && it != "—" && it != "Variable" }?.let { eq ->
                                AssistChip(onClick = {}, label = { Text(eq) }, enabled = false)
                            }
                            ejercicio.patron_movimiento?.takeIf { it.isNotBlank() }?.let { patron ->
                                AssistChip(onClick = {}, label = { Text(patron) }, enabled = false)
                            }
                        }
                        val motivoSust = ejercicio.motivo_sustitucion?.takeIf { it.isNotBlank() }
                        if (motivoSust != null) {
                            Text(motivoSust, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                        } else if (ejercicio.sustituido && !ejercicio.nombre_original.isNullOrBlank()) {
                            val orig = ejercicio.nombre_original!!.trim()
                            if (!orig.equals(ejercicio.nombre.trim(), ignoreCase = true)) {
                                Text("Alternativa a: $orig", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                            }
                        }
                        ejercicio.motivo_ajuste?.takeIf { it.isNotBlank() && !esMotivoAjusteInterno(it) }?.let { aj ->
                            Text(aj, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // Los botones de vídeo y animación están AHORA en el hero
            // (arriba del sheet), donde se ven antes de cualquier texto.
        }
    }
}

private fun esMotivoAjusteInterno(motivo: String): Boolean {
    val m = motivo.lowercase()
    return m.contains("mantenimiento") && m.contains("recuperacion") && !m.contains("volumen")
}

/**
 * Sección de coach pro: header + párrafo con accent vertical en la izquierda.
 * Imita el estilo de Caliber/Hevy para los bloques de explicación.
 */
@Composable
private fun CoachSection(
    titulo: String,
    contenido: String,
    accent: androidx.compose.ui.graphics.Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            titulo,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = accent
        )
        Text(
            contenido,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Lista bullet de claves/errores. Cada item con bullet y texto.
 */
@Composable
private fun CoachSectionLista(
    titulo: String,
    items: List<String>,
    accent: androidx.compose.ui.graphics.Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            titulo,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = accent
        )
        items.forEach { item ->
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "•",
                    style = MaterialTheme.typography.bodyMedium,
                    color = accent,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    item,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
