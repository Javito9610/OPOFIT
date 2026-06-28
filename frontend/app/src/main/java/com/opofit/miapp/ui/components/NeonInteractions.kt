package com.opofit.miapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opofit.miapp.utils.Haptics
import com.opofit.miapp.utils.rememberHaptics
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * ===============================================================
 *   KIT NEON 2026: interacciones con físicas y efectos visuales
 *
 *   Inspiración: Strava 2025, Whoop, Apple Fitness+, iOS Reminders.
 *
 *   - AnimatedCalloutTip:   pop-up explicativo con bounce spring
 *                           + glow pulsante + cierre tap.
 *   - SwipeableActionCard:  card que arrastra horizontal con
 *                           físicas (back-spring si <threshold,
 *                           commit + haptic SUCCESS si >threshold).
 *   - AnimatedNumber:       número que anima su cambio con
 *                           tween + cambio de color momentáneo
 *                           (verde subida / rojo bajada).
 * ===============================================================
 */

// --------------------------------------------------------------
//   1) AnimatedCalloutTip — pop-up explicativo con bounce
// --------------------------------------------------------------

/**
 * Tip flotante explicativo que aparece con animación spring (bounce),
 * pulsa muy sutilmente para llamar la atención, y se cierra con tap.
 *
 *   ┌──────────────────────────────────────┐
 *   │  ⓘ  Pulsa una carta para empezar  ✕  │  ← spring entrance
 *   └──────────────────────────────────────┘
 *
 *  Uso:
 *      var verTip by remember { mutableStateOf(true) }
 *      AnimatedCalloutTip(
 *          visible = verTip,
 *          text = "Pulsa una carta para empezar el entreno",
 *          onDismiss = { verTip = false }
 *      )
 */
@Composable
fun AnimatedCalloutTip(
    visible: Boolean,
    text: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Info,
    accent: Color? = null
) {
    val cs = MaterialTheme.colorScheme
    val color = accent ?: cs.primary

    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeIn(),
        exit = scaleOut(animationSpec = tween(180)) + fadeOut(),
        modifier = modifier
    ) {
        // Glow pulsante muy sutil (lime/cyan se mueven entre alpha 0.18 ↔ 0.32)
        val transition = rememberInfiniteTransition(label = "callout-pulse")
        val glow by transition.animateFloat(
            initialValue = 0.18f,
            targetValue = 0.32f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glow"
        )
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = cs.surface,
            shadowElevation = 12.dp,
            modifier = Modifier
                .background(color.copy(alpha = glow), RoundedCornerShape(14.dp))
                .padding(2.dp)
                .clickable(onClick = onDismiss)
        ) {
            Row(
                modifier = Modifier
                    .background(cs.surface, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(color.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                }
                Text(
                    text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = cs.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Cerrar",
                    tint = cs.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// --------------------------------------------------------------
//   2) SwipeableActionCard — físicas + haptic + back-spring
// --------------------------------------------------------------

/**
 * Card swipeable horizontal con físicas reales:
 *   - Arrastras hacia la derecha → revela acción de COMPLETAR (lime)
 *   - Arrastras hacia la izquierda → revela acción de SALTAR (rojo)
 *   - Si NO llegas al threshold, vuelve con spring (efecto rebote)
 *   - Si LLEGAS al threshold, dispara haptic SUCCESS + onCompleted/onSkipped
 *
 *  Es lo que hace Strava con los segmentos, iOS con los emails, etc.
 *
 *  Uso:
 *      SwipeableActionCard(
 *          onCompleted = { ... },
 *          onSkipped = { ... }
 *      ) {
 *          // tu contenido visual de la card
 *      }
 */
@Composable
fun SwipeableActionCard(
    onCompleted: () -> Unit,
    onSkipped: () -> Unit,
    modifier: Modifier = Modifier,
    completeLabel: String = "Hecho",
    skipLabel: String = "Saltar",
    content: @Composable () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val haptic = rememberHaptics()
    val scope = rememberCoroutineScope()

    // Offset del card en px. Animatable para sumar gesto + spring back.
    val offset = remember { Animatable(0f) }
    val thresholdPx = with(density) { 120.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
    ) {
        // Fondo con acciones que se revelan según el offset.
        val abs = abs(offset.value)
        val revealAlpha = (abs / thresholdPx).coerceIn(0f, 1f)
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .alpha(revealAlpha),
            shape = RoundedCornerShape(16.dp),
            color = if (offset.value > 0f) cs.primary.copy(alpha = 0.85f)
                    else cs.error.copy(alpha = 0.85f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (offset.value > 0f) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        completeLabel.uppercase(),
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )
                } else if (offset.value < 0f) {
                    Spacer(Modifier.weight(1f))
                    Text(
                        skipLabel.uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // Card visible que se mueve con el dedo.
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationX = offset.value }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                when {
                                    offset.value >= thresholdPx -> {
                                        haptic(Haptics.Type.SUCCESS)
                                        onCompleted()
                                        offset.animateTo(
                                            0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow
                                            )
                                        )
                                    }
                                    offset.value <= -thresholdPx -> {
                                        haptic(Haptics.Type.MEDIUM)
                                        onSkipped()
                                        offset.animateTo(
                                            0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow
                                            )
                                        )
                                    }
                                    else -> {
                                        // Back-spring si no llegó al threshold
                                        offset.animateTo(
                                            0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                    }
                                }
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            scope.launch {
                                offset.snapTo((offset.value + dragAmount).coerceIn(-280f, 280f))
                            }
                        }
                    )
                },
            shape = RoundedCornerShape(16.dp),
            color = cs.surface,
            shadowElevation = 2.dp
        ) {
            content()
        }
    }
}

// --------------------------------------------------------------
//   3) AnimatedNumber — número que tween entre valores + flash color
// --------------------------------------------------------------

/**
 * Texto numérico que anima entre su valor anterior y el nuevo cuando
 * cambia. Si el nuevo es mayor que el anterior, hace un flash LIME
 * (verde subida); si es menor, flash ROJO (bajada).
 *
 *      AnimatedNumber(value = sesionesEstaSemana, suffix = " sesiones")
 *
 *  Whoop hace exactamente esto con sus métricas — la pantalla se siente
 *  viva.
 */
@Composable
fun AnimatedNumber(
    value: Int,
    modifier: Modifier = Modifier,
    suffix: String = "",
    fontSize: Float = 32f,
    accent: Color? = null
) {
    val cs = MaterialTheme.colorScheme
    val color = accent ?: Color.White

    var prev by remember { mutableStateOf(value) }
    var flash by remember { mutableStateOf(false) }
    var flashColor by remember { mutableStateOf(color) }

    LaunchedEffect(value) {
        if (value != prev) {
            flashColor = when {
                value > prev -> cs.primary    // lime subida
                else         -> cs.error      // rojo bajada
            }
            flash = true
            kotlinx.coroutines.delay(420)
            flash = false
            prev = value
        }
    }

    val animatedValue by androidx.compose.animation.core.animateIntAsState(
        targetValue = value,
        animationSpec = tween(durationMillis = 600),
        label = "number-tween"
    )
    val animatedColor by animateFloatAsState(
        targetValue = if (flash) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "flash"
    )
    val finalColor = lerp(color, flashColor, animatedColor)

    Text(
        text = "$animatedValue$suffix",
        modifier = modifier,
        color = finalColor,
        fontSize = fontSize.sp,
        fontWeight = FontWeight.Black
    )
}

private fun lerp(a: Color, b: Color, t: Float): Color {
    val tt = t.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * tt,
        green = a.green + (b.green - a.green) * tt,
        blue = a.blue + (b.blue - a.blue) * tt,
        alpha = a.alpha + (b.alpha - a.alpha) * tt
    )
}
