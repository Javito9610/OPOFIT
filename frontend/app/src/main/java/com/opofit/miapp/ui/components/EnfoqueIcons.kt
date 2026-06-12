package com.opofit.miapp.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.outlined.DirectionsBike
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Pool
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.SportsMartialArts
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Iconos canónicos para cada pilar de entreno y tipo de actividad.
 *
 * Sustituye los emojis dispersos por la app por iconos Material con
 * significado consistente. Una sola fuente de verdad para la UI: si en
 * el futuro queremos cambiar el icono de "fuerza", lo cambiamos aquí.
 */
object EnfoqueIcons {

    fun forEnfoque(enfoque: String?): ImageVector = when (enfoque?.uppercase()) {
        "FUERZA" -> Icons.Outlined.FitnessCenter
        "RESISTENCIA" -> Icons.AutoMirrored.Outlined.DirectionsRun
        "VELOCIDAD" -> Icons.Filled.Bolt
        "CORE" -> Icons.Outlined.SelfImprovement
        "MOVILIDAD" -> Icons.Outlined.SelfImprovement
        else -> Icons.Outlined.FitnessCenter
    }

    /** Tipo de post/actividad social (Strava/Hevy/Adidas style). */
    fun forActividadTipo(tipo: String?): ImageVector {
        val t = tipo?.uppercase() ?: ""
        return when {
            t.contains("RUN") || t == "CARRERA" -> Icons.AutoMirrored.Outlined.DirectionsRun
            t.contains("BIKE") || t == "BICI" -> Icons.Outlined.DirectionsBike
            t == "WALK" || t == "ANDAR" -> Icons.AutoMirrored.Outlined.DirectionsRun
            t == "SWIM" || t == "PISCINA" -> Icons.Outlined.Pool
            t == "ENTRENO" -> Icons.Outlined.FitnessCenter
            t == "SIMULACRO" -> Icons.Outlined.EmojiEvents
            t == "GPS" -> Icons.Outlined.LocationOn
            t == "DEFENSA" || t == "DPN" -> Icons.Outlined.SportsMartialArts
            t == "HIIT" -> Icons.Outlined.Whatshot
            else -> Icons.Outlined.PushPin
        }
    }
}
