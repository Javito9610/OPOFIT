package com.opofit.miapp.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Sistema de coach marks (onboarding tipo Duolingo/Headspace).
 *
 * Filosofía: la primera vez que el usuario entra a una pantalla, le mostramos
 * 1-3 burbujas explicativas en overlay. Si pulsa "Entendido" o "Cerrar",
 * persistimos la marca en SharedPreferences y no las volvemos a mostrar.
 *
 * Uso:
 * ```
 * Box(Modifier.fillMaxSize()) {
 *     // Tu UI normal
 *     YourScreenContent()
 *
 *     // Overlay de coach marks (solo se ve la primera vez)
 *     CoachMarkOverlay(
 *         screenKey = "home_v1",
 *         steps = listOf(
 *             CoachStep("Bienvenido a OpoFit", "Aquí ves tu progreso..."),
 *             CoachStep("Plan semanal", "Tu sesión de hoy aparece arriba..."),
 *             CoachStep("Pulso en vivo", "Conecta tu reloj para ver tu pulso...")
 *         )
 *     )
 * }
 * ```
 *
 * Versionar el screenKey (p.ej. "home_v1", "home_v2") permite re-mostrar
 * coach marks tras un rediseño grande.
 */
data class CoachStep(
    val title: String,
    val text: String
)

private const val PREFS_NAME = "opofit_coachmarks"

private fun isShown(context: Context, key: String): Boolean =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(key, false)

private fun markShown(context: Context, key: String) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(key, true)
        .apply()
}

/** Util para resetear todos los coach marks (testing o "ver tutorial otra vez"). */
fun resetAllCoachMarks(context: Context) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit().clear().apply()
}

/** Util para marcar coach marks individualmente. */
fun markCoachShown(context: Context, screenKey: String) {
    markShown(context, screenKey)
}

@Composable
fun CoachMarkOverlay(
    screenKey: String,
    steps: List<CoachStep>,
    onFinished: () -> Unit = {}
) {
    val context = LocalContext.current
    var visible by remember(screenKey) { mutableStateOf(false) }
    var current by remember(screenKey) { mutableIntStateOf(0) }

    LaunchedEffect(screenKey) {
        if (steps.isNotEmpty() && !isShown(context, screenKey)) {
            visible = true
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        // Overlay semitransparente sobre toda la pantalla
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = true,
                enter = scaleIn() + fadeIn()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .padding(24.dp),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Header: bombilla + paso actual
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.Lightbulb,
                                null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                "Tip ${current + 1} / ${steps.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            steps[current].title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            steps[current].text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                        )

                        // Dots indicator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            steps.forEachIndexed { index, _ ->
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (index == current) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        )
                                )
                            }
                        }

                        // Botones: Saltar / Siguiente / Entendido
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = {
                                visible = false
                                markShown(context, screenKey)
                                onFinished()
                            }) {
                                Text(
                                    "Saltar",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = {
                                    if (current < steps.size - 1) {
                                        current += 1
                                    } else {
                                        visible = false
                                        markShown(context, screenKey)
                                        onFinished()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(
                                    if (current < steps.size - 1) "Siguiente" else "Entendido",
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (current < steps.size - 1) {
                                    Icon(
                                        Icons.Filled.ChevronRight,
                                        null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
