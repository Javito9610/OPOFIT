package com.opofit.miapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal fun inferGrupoMuscular(nombre: String, grupo: String?, pilar: String?): String {
    if (!grupo.isNullOrBlank()) return grupo
    val n = nombre.lowercase()
    val pil = pilar?.uppercase().orEmpty()
    // OJO con el ORDEN: "press militar" y "leg press" contienen "press" pero NO
    // son de pecho. Por eso hombros y pierna se evalúan ANTES que pecho.
    return when {
        pil == "RESISTENCIA" || pil == "VELOCIDAD" -> "Cardio"
        pil == "CORE" || n.contains("plancha") || n.contains("plank") || n.contains("core") ||
            n.contains("abdominal") || n.contains("crunch") || n.contains("hollow") -> "Core"
        n.contains("dominada") || n.contains("remo") || n.contains("jalón") || n.contains("jalon") ||
            n.contains("pull") || n.contains("pulldown") -> "Espalda"
        // Hombros ANTES que pecho (press militar/overhead/elevaciones no son pecho).
        n.contains("militar") || n.contains("hombro") || n.contains("elevacion") || n.contains("elevación") ||
            n.contains("arnold") || n.contains("overhead") || n.contains("push press") ||
            n.contains("pájaro") || n.contains("pajaro") || n.contains("face pull") -> "Hombros"
        // Pierna ANTES que pecho ("leg press" lleva "press" pero es pierna).
        n.contains("sentadilla") || n.contains("squat") || n.contains("zancada") || n.contains("lunge") ||
            n.contains("step") || n.contains("pierna") || n.contains("prensa") || n.contains("leg ") ||
            n.contains("gemelo") || n.contains("hip thrust") || n.contains("glúteo") || n.contains("gluteo") ||
            n.contains("femoral") || n.contains("cuádriceps") || n.contains("cuadriceps") ||
            n.contains("búlgara") || n.contains("bulgara") || n.contains("pistol") || n.contains("puente") -> "Pierna"
        n.contains("curl") || n.contains("tríceps") || n.contains("triceps") ||
            n.contains("bíceps") || n.contains("biceps") -> "Brazos"
        n.contains("press") || n.contains("flexion") || n.contains("flexión") || n.contains("push-up") ||
            n.contains("push up") || n.contains("fondos") || n.contains("bench") || n.contains("apertura") ||
            n.contains("pec deck") || n.contains("pb ") -> "Pecho"
        n.contains("natac") || n.contains("carrera") || n.contains("sprint") || n.contains("rodaje") -> "Cardio"
        else -> "General"
    }
}

internal fun tipoEjercicioLabel(tipoIlustracion: String?, nombre: String, pilar: String?): String = when (
    poseFrom(tipoIlustracion, nombre, pilar)
) {
    "PUSH" -> "Empuje"
    "PULL" -> "Tirón"
    "SQUAT" -> "Pierna"
    "PLANK" -> "Core"
    "RUN", "CARDIO" -> "Cardio"
    "AGILITY" -> "Agilidad"
    "MOBILITY" -> "Movilidad"
    else -> "Fuerza"
}

internal fun poseFrom(tipoIlustracion: String?, nombre: String, pilar: String?): String {
    val pose = (tipoIlustracion ?: "").uppercase()
    if (pose.isNotBlank() && pose != "GENERAL") return pose
    val n = nombre.lowercase()
    val pil = pilar?.uppercase().orEmpty()
    return when {
        pil == "CORE" || n.contains("plancha") -> "PLANK"
        n.contains("dominada") || n.contains("remo") -> "PULL"
        n.contains("press") || n.contains("flexion") || n.contains("fondos") || n.contains("pb ") -> "PUSH"
        n.contains("sentadilla") || n.contains("zancada") || n.contains("step") -> "SQUAT"
        n.contains("sprint") || n.contains("velocidad") -> "AGILITY"
        n.contains("natac") || n.contains("carrera") || pil == "RESISTENCIA" -> "RUN"
        else -> "GENERAL"
    }
}

@Composable
fun ExerciseInfoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(size)
    ) {
        Box(
            modifier = Modifier
                .size(size * 0.88f)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = "Ver ejercicio",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(size * 0.5f)
            )
        }
    }
}
