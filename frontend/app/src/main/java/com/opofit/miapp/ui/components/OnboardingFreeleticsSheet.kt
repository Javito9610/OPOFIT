package com.opofit.miapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * OnboardingFreeleticsSheet — wizard de 4 pasos al estilo Freeletics Coach
 * para personalizar el plan del usuario en modo FITNESS.
 *
 * Pasos:
 *   1. OBJETIVO        (perder_grasa / ganar_musculo / resistencia / rendimiento)
 *   2. DÍAS POR SEMANA (3 / 4 / 5 / 6)
 *   3. TIEMPO/SESIÓN   (30 / 45 / 60 / 90 min)
 *   4. LESIONES        (ninguna / rodilla / hombro / lumbar / ...)
 *
 * El sheet es full-height, pasos con dot indicator arriba y bottom CTA fijo.
 */

data class OnboardingResult(
    val objetivo: String,         // perder_grasa | ganar_musculo | resistencia | rendimiento
    val diasSemana: Int,          // 3..6
    val tiempoMin: Int,           // 30..90
    val lesiones: List<String>    // rodilla | hombro | lumbar | tobillo | codo | muneca
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingFreeleticsSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (OnboardingResult) -> Unit
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var paso by remember { mutableIntStateOf(0) }
    var objetivo by remember { mutableStateOf<String?>(null) }
    var diasSemana by remember { mutableIntStateOf(4) }
    var tiempoMin by remember { mutableIntStateOf(45) }
    val lesionesSel = remember { mutableStateOf(setOf<String>()) }

    val totalPasos = 4
    val cs = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        containerColor = cs.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Indicador de pasos (Freeletics-style: barras horizontales)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(totalPasos) { i ->
                    Box(
                        modifier = Modifier
                            .height(4.dp)
                            .weight(1f)
                            .background(
                                if (i <= paso) cs.primary else cs.surfaceVariant.copy(alpha = 0.6f),
                                androidx.compose.foundation.shape.RoundedCornerShape(2.dp)
                            )
                    )
                }
            }

            Text(
                text = "PASO ${paso + 1} DE $totalPasos",
                style = MaterialTheme.typography.labelSmall,
                color = cs.primary,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            )

            // Contenido del paso
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (paso) {
                    0 -> PasoObjetivo(seleccionado = objetivo, onSelect = { objetivo = it })
                    1 -> PasoDias(seleccionado = diasSemana, onSelect = { diasSemana = it })
                    2 -> PasoTiempo(seleccionado = tiempoMin, onSelect = { tiempoMin = it })
                    else -> PasoLesiones(seleccionadas = lesionesSel.value, onToggle = { l ->
                        lesionesSel.value = if (l in lesionesSel.value) lesionesSel.value - l else lesionesSel.value + l
                    })
                }
            }

            // Botones inferiores
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (paso > 0) {
                    FreeleticsGhostButton(
                        text = "Atrás",
                        onClick = { paso-- },
                        modifier = Modifier.weight(1f)
                    )
                }
                val puedeAvanzar = when (paso) {
                    0 -> objetivo != null
                    else -> true
                }
                FreeleticsBigButton(
                    text = if (paso == totalPasos - 1) "Crear mi plan" else "Continuar",
                    onClick = {
                        if (paso < totalPasos - 1) {
                            paso++
                        } else {
                            onConfirm(
                                OnboardingResult(
                                    objetivo = objetivo ?: "rendimiento",
                                    diasSemana = diasSemana,
                                    tiempoMin = tiempoMin,
                                    lesiones = lesionesSel.value.toList()
                                )
                            )
                        }
                    },
                    enabled = puedeAvanzar,
                    modifier = if (paso > 0) Modifier.weight(2f) else Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PasoObjetivo(seleccionado: String?, onSelect: (String) -> Unit) {
    Text(
        "¿Qué quieres conseguir?",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Black,
        color = Color.White
    )
    Text(
        "Elige tu objetivo principal. Adaptaremos el plan para llegar ahí.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(4.dp))
    FreeleticsGoalCard(
        label = "Perder grasa",
        description = "Quemar calorías y definir",
        icon = Icons.Filled.LocalFireDepartment,
        selected = seleccionado == "perder_grasa",
        onClick = { onSelect("perder_grasa") }
    )
    FreeleticsGoalCard(
        label = "Ganar músculo",
        description = "Hipertrofia y fuerza máxima",
        icon = Icons.Filled.FitnessCenter,
        selected = seleccionado == "ganar_musculo",
        onClick = { onSelect("ganar_musculo") }
    )
    FreeleticsGoalCard(
        label = "Resistencia",
        description = "Correr más y mejor",
        icon = Icons.AutoMirrored.Filled.DirectionsRun,
        selected = seleccionado == "resistencia",
        onClick = { onSelect("resistencia") }
    )
    FreeleticsGoalCard(
        label = "Rendimiento",
        description = "Mejorar marcas en todo",
        icon = Icons.Filled.Whatshot,
        selected = seleccionado == "rendimiento",
        onClick = { onSelect("rendimiento") }
    )
}

@Composable
private fun PasoDias(seleccionado: Int, onSelect: (Int) -> Unit) {
    Text(
        "¿Cuántos días por semana?",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Black,
        color = Color.White
    )
    Text(
        "Sé honesto: con 3 días bien constantes ganas más que con 6 que fallas.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(4.dp))
    listOf(3, 4, 5, 6).forEach { n ->
        FreeleticsGoalCard(
            label = "$n días",
            description = when (n) {
                3 -> "Constante. Suficiente para progresar"
                4 -> "Equilibrio entre volumen y recuperación"
                5 -> "Volumen alto. Necesitas buena recuperación"
                else -> "Solo para avanzados o atletas"
            },
            icon = Icons.Filled.CalendarMonth,
            selected = seleccionado == n,
            onClick = { onSelect(n) }
        )
    }
}

@Composable
private fun PasoTiempo(seleccionado: Int, onSelect: (Int) -> Unit) {
    Text(
        "¿Cuánto tiempo tienes por sesión?",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Black,
        color = Color.White
    )
    Text(
        "Comprimimos la sesión a tu tiempo real. Calidad sobre cantidad.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(4.dp))
    listOf(30, 45, 60, 90).forEach { m ->
        FreeleticsGoalCard(
            label = "$m min",
            description = when (m) {
                30 -> "Sesión exprés. Solo lo esencial"
                45 -> "Recomendado. Cabe lo importante"
                60 -> "Sesión completa con accesorios"
                else -> "Sesión larga con técnica y carga"
            },
            icon = Icons.Filled.AccessTime,
            selected = seleccionado == m,
            onClick = { onSelect(m) }
        )
    }
}

@Composable
private fun PasoLesiones(seleccionadas: Set<String>, onToggle: (String) -> Unit) {
    Text(
        "¿Alguna lesión o molestia?",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Black,
        color = Color.White
    )
    Text(
        "Marca lo que tengas. Evitaremos ejercicios contraindicados.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(4.dp))
    listOf(
        "rodilla"  to "Rodilla",
        "hombro"   to "Hombro",
        "lumbar"   to "Espalda baja (lumbar)",
        "tobillo"  to "Tobillo",
        "codo"     to "Codo",
        "muneca"   to "Muñeca"
    ).forEach { (key, label) ->
        FreeleticsGoalCard(
            label = label,
            description = if (key in seleccionadas) "Activada — evitaremos cargas en esta zona" else "Sin molestias",
            icon = Icons.Filled.Favorite,
            selected = key in seleccionadas,
            onClick = { onToggle(key) }
        )
    }
}
