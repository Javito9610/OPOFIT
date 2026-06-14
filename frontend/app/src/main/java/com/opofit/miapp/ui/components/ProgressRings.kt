package com.opofit.miapp.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Anillos de progreso animados estilo Apple Fitness+ / Whoop.
 *
 * 3 anillos concéntricos (sesiones / racha / km) que se llenan con animación
 * de easing pro al renderizarse. El central pulsa suavemente con un infinite
 * transition para llamar la atención (técnica Strava: lo que importa pulsa).
 *
 * Uso típico:
 *   ProgressRingsTrio(
 *     sesionesActual = 3, sesionesObjetivo = 5,
 *     rachaActual = 7, rachaObjetivo = 14,
 *     kmActual = 12.3, kmObjetivo = 20.0,
 *     modifier = Modifier.fillMaxWidth()
 *   )
 */

@Composable
fun ProgressRingsTrio(
    sesionesActual: Int,
    sesionesObjetivo: Int,
    rachaActual: Int,
    rachaObjetivo: Int,
    kmActual: Double,
    kmObjetivo: Double,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val ringColors = listOf(
        cs.primary to cs.primary.copy(alpha = 0.18f),
        cs.tertiary to cs.tertiary.copy(alpha = 0.18f),
        cs.secondary to cs.secondary.copy(alpha = 0.18f)
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Box(modifier = Modifier.size(150.dp)) {
            AnimatedRing(
                progress = sesionesActual.toFloat().coerceAtLeast(0f) / sesionesObjetivo.coerceAtLeast(1).toFloat(),
                color = ringColors[0].first,
                bgColor = ringColors[0].second,
                modifier = Modifier
                    .size(150.dp)
                    .align(Alignment.Center),
                ringIndex = 0,
                strokeWidthDp = 14.dp
            )
            AnimatedRing(
                progress = rachaActual.toFloat().coerceAtLeast(0f) / rachaObjetivo.coerceAtLeast(1).toFloat(),
                color = ringColors[1].first,
                bgColor = ringColors[1].second,
                modifier = Modifier
                    .size(108.dp)
                    .align(Alignment.Center),
                ringIndex = 1,
                strokeWidthDp = 12.dp
            )
            AnimatedRing(
                progress = (kmActual / kmObjetivo.coerceAtLeast(0.001)).toFloat().coerceIn(0f, 1.5f),
                color = ringColors[2].first,
                bgColor = ringColors[2].second,
                modifier = Modifier
                    .size(70.dp)
                    .align(Alignment.Center),
                ringIndex = 2,
                strokeWidthDp = 10.dp
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RingLegend(label = "Sesiones", value = "$sesionesActual/$sesionesObjetivo", color = ringColors[0].first)
            RingLegend(label = "Racha", value = "$rachaActual días", color = ringColors[1].first)
            RingLegend(label = "Distancia", value = "%.1f / %.0f km".format(kmActual, kmObjetivo), color = ringColors[2].first)
        }
    }
}

@Composable
private fun AnimatedRing(
    progress: Float,
    color: Color,
    bgColor: Color,
    modifier: Modifier,
    ringIndex: Int,
    strokeWidthDp: androidx.compose.ui.unit.Dp
) {
    val target = progress.coerceIn(0f, 1f)
    val anim = remember { Animatable(0f) }
    LaunchedEffect(target) {
        anim.animateTo(
            targetValue = target,
            animationSpec = tween(
                durationMillis = 1100 + ringIndex * 120,
                delayMillis = ringIndex * 80,
                easing = EaseOutCubic
            )
        )
    }
    // Pulse infinito muy suave para "respirar" — Whoop / Apple Fitness lo hacen.
    val transition = rememberInfiniteTransition(label = "ring-pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400 + ringIndex * 300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(modifier = modifier) {
        val stroke = strokeWidthDp.toPx()
        val pad = stroke / 2f
        val ringSize = Size(size.width - stroke, size.height - stroke)
        // Anillo background.
        drawArc(
            color = bgColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(pad, pad),
            size = ringSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        // Anillo progreso (con gradient horizontal hacia más intenso).
        val sweep = anim.value * 360f * pulse.coerceIn(0.95f, 1.05f)
        if (sweep > 0f) {
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(color.copy(alpha = 0.55f), color, color)
                ),
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(pad, pad),
                size = ringSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
private fun RingLegend(label: String, value: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .padding(0.dp)
        ) {
            Canvas(modifier = Modifier.size(10.dp)) {
                drawCircle(color)
            }
        }
        Column {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
