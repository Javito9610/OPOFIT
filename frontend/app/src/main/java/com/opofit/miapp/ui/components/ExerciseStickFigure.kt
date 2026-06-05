package com.opofit.miapp.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Muñeco animado estilo Heavy: indica el tipo de movimiento del ejercicio.
 */
@Composable
fun ExerciseStickFigure(
    tipoIlustracion: String?,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val pose = (tipoIlustracion ?: "GENERAL").uppercase()
    val transition = rememberInfiniteTransition(label = "stick")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase"
    )

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val stroke = Stroke(width = w * 0.06f, cap = StrokeCap.Round)
        val headR = w * 0.1f
        val headY = h * 0.18f

        fun line(x1: Float, y1: Float, x2: Float, y2: Float) {
            drawLine(color, Offset(x1, y1), Offset(x2, y2), stroke.width, StrokeCap.Round)
        }

        drawCircle(color, headR, center = Offset(cx, headY), style = stroke)
        val neckY = headY + headR + w * 0.02f
        val hipY = h * 0.58f
        val shoulderY = h * 0.32f

        line(cx, neckY, cx, hipY)

        when (pose) {
            "PUSH" -> {
                val ang = 0.35f + phase * 0.25f
                val handX = cx + cos(ang) * w * 0.38f
                val handY = shoulderY + sin(ang) * h * 0.22f
                line(cx, shoulderY, handX, handY)
                line(cx, shoulderY, cx - w * 0.22f, handY)
                line(cx, hipY, cx - w * 0.12f, h * 0.82f)
                line(cx, hipY, cx + w * 0.18f, h * 0.78f - phase * h * 0.06f)
            }
            "PULL" -> {
                val pull = phase * w * 0.08f
                line(cx, shoulderY, cx - w * 0.28f, headY - pull)
                line(cx, shoulderY, cx + w * 0.28f, headY - pull)
                line(cx, hipY, cx - w * 0.14f, h * 0.88f)
                line(cx, hipY, cx + w * 0.14f, h * 0.88f)
            }
            "SQUAT" -> {
                val drop = phase * h * 0.12f
                line(cx, shoulderY, cx - w * 0.28f, shoulderY + h * 0.08f)
                line(cx, shoulderY, cx + w * 0.28f, shoulderY + h * 0.08f)
                line(cx, hipY + drop, cx - w * 0.22f, h * 0.82f)
                line(cx, hipY + drop, cx + w * 0.22f, h * 0.82f)
            }
            "PLANK" -> {
                val bodyY = shoulderY + h * 0.08f
                line(cx - w * 0.3f, bodyY, cx + w * 0.3f, bodyY)
                line(cx - w * 0.3f, bodyY, cx - w * 0.38f, bodyY + h * 0.1f)
                line(cx + w * 0.3f, bodyY, cx + w * 0.38f, bodyY + h * 0.1f)
            }
            "RUN", "CARDIO" -> {
                val swing = phase * w * 0.14f
                line(cx, shoulderY, cx - w * 0.2f + swing, shoulderY + h * 0.18f)
                line(cx, shoulderY, cx + w * 0.2f - swing, shoulderY + h * 0.12f)
                line(cx, hipY, cx - w * 0.1f + swing, h * 0.88f)
                line(cx, hipY, cx + w * 0.16f - swing, h * 0.82f)
            }
            "AGILITY" -> {
                val jump = phase * h * 0.1f
                line(cx, shoulderY, cx - w * 0.3f, shoulderY - jump * 0.3f)
                line(cx, shoulderY, cx + w * 0.3f, shoulderY - jump * 0.3f)
                line(cx, hipY - jump, cx - w * 0.18f, h * 0.78f)
                line(cx, hipY - jump, cx + w * 0.22f, h * 0.85f)
            }
            "MOBILITY" -> {
                val arc = phase * 0.5f
                line(cx, shoulderY, cx - w * 0.32f, shoulderY - h * arc * 0.15f)
                line(cx, shoulderY, cx + w * 0.32f, shoulderY + h * arc * 0.1f)
                line(cx, hipY, cx - w * 0.12f, h * 0.86f)
                line(cx, hipY, cx + w * 0.12f, h * 0.86f)
            }
            else -> {
                line(cx, shoulderY, cx - w * 0.26f, shoulderY + h * 0.14f)
                line(cx, shoulderY, cx + w * 0.26f, shoulderY + h * 0.14f)
                line(cx, hipY, cx - w * 0.14f, h * 0.88f)
                line(cx, hipY, cx + w * 0.14f, h * 0.88f)
            }
        }
    }
}
