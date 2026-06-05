package com.opofit.miapp.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

private data class PoseStyle(
    val emoji: String,
    val label: String,
    val top: Color,
    val bottom: Color,
    val accent: Color
)

private fun styleFor(pose: String): PoseStyle = when (pose) {
    "PUSH" -> PoseStyle("🏋️", "Empuje", Color(0xFF1565C0), Color(0xFF0D47A1), Color(0xFF90CAF9))
    "PULL" -> PoseStyle("🧗", "Tirón", Color(0xFF2E7D32), Color(0xFF1B5E20), Color(0xFFA5D6A7))
    "SQUAT" -> PoseStyle("🦵", "Pierna", Color(0xFF6A1B9A), Color(0xFF4A148C), Color(0xFFCE93D8))
    "PLANK" -> PoseStyle("🧘", "Core", Color(0xFF00838F), Color(0xFF006064), Color(0xFF80DEEA))
    "RUN", "CARDIO" -> PoseStyle("🏃", "Cardio", Color(0xFFEF6C00), Color(0xFFE65100), Color(0xFFFFCC80))
    "AGILITY" -> PoseStyle("⚡", "Agilidad", Color(0xFFC62828), Color(0xFF8E0000), Color(0xFFEF9A9A))
    "MOBILITY" -> PoseStyle("🤸", "Movilidad", Color(0xFF5D4037), Color(0xFF3E2723), Color(0xFFBCAAA4))
    else -> PoseStyle("💪", "Fuerza", Color(0xFF455A64), Color(0xFF263238), Color(0xFFB0BEC5))
}

/**
 * Ilustración de ejercicio con fondo, emoji y silueta animada más legible.
 */
@Composable
fun ExerciseStickFigure(
    tipoIlustracion: String?,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val pose = (tipoIlustracion ?: "GENERAL").uppercase()
    val style = styleFor(pose)
    val transition = rememberInfiniteTransition(label = "pose")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase"
    )

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(style.top, style.bottom))),
        contentAlignment = Alignment.Center
    ) {
        Text(
            style.emoji,
            fontSize = (size.value * 0.22f).sp,
            modifier = Modifier.align(Alignment.TopEnd).size(size * 0.35f),
            fontWeight = FontWeight.Normal
        )
        Canvas(modifier = Modifier.size(size * 0.82f)) {
            val w = this.size.width
            val h = this.size.height
            val cx = w / 2f
            val strokeW = w * 0.075f
            val stroke = Stroke(width = strokeW, cap = StrokeCap.Round)
            val fillColor = Color.White.copy(alpha = 0.95f)
            val accent = style.accent

            fun limb(x1: Float, y1: Float, x2: Float, y2: Float, thick: Float = strokeW) {
                drawLine(accent, Offset(x1, y1), Offset(x2, y2), thick, StrokeCap.Round)
            }
            fun joint(x: Float, y: Float, r: Float = strokeW * 0.55f) {
                drawCircle(fillColor, r, Offset(x, y))
            }

            val headR = w * 0.11f
            val headY = h * 0.17f
            drawCircle(fillColor, headR, Offset(cx, headY))
            drawCircle(accent, headR, Offset(cx, headY), style = Stroke(strokeW * 0.5f))

            val shoulderY = h * 0.33f
            val hipY = h * 0.56f
            val torsoW = w * 0.14f
            drawRoundRect(
                fillColor,
                topLeft = Offset(cx - torsoW / 2, shoulderY - h * 0.02f),
                size = Size(torsoW, hipY - shoulderY + h * 0.02f),
                cornerRadius = CornerRadius(torsoW * 0.3f)
            )

            when (pose) {
                "PUSH" -> {
                    val drop = phase * h * 0.07f
                    val barY = shoulderY + h * 0.1f + drop
                    drawRoundRect(
                        accent.copy(alpha = 0.85f),
                        topLeft = Offset(cx - w * 0.42f, barY - strokeW * 0.4f),
                        size = Size(w * 0.84f, strokeW * 0.9f),
                        cornerRadius = CornerRadius(strokeW * 0.4f)
                    )
                    limb(cx - w * 0.28f, shoulderY + drop, cx - w * 0.34f, barY, strokeW * 1.1f)
                    limb(cx + w * 0.28f, shoulderY + drop, cx + w * 0.34f, barY, strokeW * 1.1f)
                    limb(cx, hipY + drop, cx - w * 0.16f, h * 0.9f, strokeW * 1.05f)
                    limb(cx, hipY + drop, cx + w * 0.16f, h * 0.9f, strokeW * 1.05f)
                    joint(cx - w * 0.34f, barY)
                    joint(cx + w * 0.34f, barY)
                }
                "PULL" -> {
                    drawLine(
                        accent.copy(alpha = 0.9f),
                        Offset(cx - w * 0.38f, h * 0.1f),
                        Offset(cx + w * 0.38f, h * 0.1f),
                        strokeW * 0.9f,
                        StrokeCap.Round
                    )
                    val pull = phase * h * 0.09f
                    limb(cx - w * 0.22f, shoulderY, cx - w * 0.3f, h * 0.14f - pull, strokeW * 1.1f)
                    limb(cx + w * 0.22f, shoulderY, cx + w * 0.3f, h * 0.14f - pull, strokeW * 1.1f)
                    limb(cx, hipY, cx - w * 0.14f, h * 0.9f)
                    limb(cx, hipY, cx + w * 0.14f, h * 0.9f)
                    joint(cx - w * 0.3f, h * 0.14f - pull)
                    joint(cx + w * 0.3f, h * 0.14f - pull)
                }
                "SQUAT" -> {
                    val drop = phase * h * 0.14f
                    limb(cx - w * 0.26f, shoulderY, cx - w * 0.34f, shoulderY + h * 0.1f)
                    limb(cx + w * 0.26f, shoulderY, cx + w * 0.34f, shoulderY + h * 0.1f)
                    val kneeY = hipY + drop + h * 0.12f
                    limb(cx, hipY + drop, cx - w * 0.24f, kneeY, strokeW * 1.1f)
                    limb(cx, hipY + drop, cx + w * 0.24f, kneeY, strokeW * 1.1f)
                    limb(cx - w * 0.24f, kneeY, cx - w * 0.2f, h * 0.9f, strokeW * 1.05f)
                    limb(cx + w * 0.24f, kneeY, cx + w * 0.2f, h * 0.9f, strokeW * 1.05f)
                    joint(cx - w * 0.24f, kneeY, strokeW * 0.65f)
                    joint(cx + w * 0.24f, kneeY, strokeW * 0.65f)
                }
                "PLANK" -> {
                    val bodyY = shoulderY + h * 0.06f
                    val path = Path().apply {
                        moveTo(cx - w * 0.34f, bodyY)
                        lineTo(cx + w * 0.34f, bodyY)
                        lineTo(cx + w * 0.38f, bodyY + h * 0.1f)
                        lineTo(cx - w * 0.38f, bodyY + h * 0.1f)
                        close()
                    }
                    drawPath(path, fillColor, style = Fill)
                    drawPath(path, accent, style = Stroke(strokeW * 0.6f))
                    limb(cx - w * 0.34f, bodyY, cx - w * 0.42f, bodyY + h * 0.14f)
                    limb(cx + w * 0.34f, bodyY, cx + w * 0.42f, bodyY + h * 0.14f)
                    limb(cx - w * 0.1f, bodyY + h * 0.1f, cx - w * 0.1f, h * 0.88f)
                    limb(cx + w * 0.1f, bodyY + h * 0.1f, cx + w * 0.1f, h * 0.88f)
                }
                "RUN", "CARDIO" -> {
                    val swing = phase * w * 0.16f
                    limb(cx - w * 0.2f + swing, shoulderY, cx - w * 0.32f + swing, shoulderY + h * 0.2f, strokeW * 1.05f)
                    limb(cx + w * 0.2f - swing, shoulderY, cx + w * 0.32f - swing, shoulderY + h * 0.14f, strokeW * 1.05f)
                    limb(cx, hipY, cx - w * 0.1f + swing, h * 0.88f, strokeW * 1.1f)
                    limb(cx, hipY, cx + w * 0.18f - swing, h * 0.78f, strokeW * 1.1f)
                }
                "AGILITY" -> {
                    val jump = phase * h * 0.12f
                    limb(cx - w * 0.3f, shoulderY - jump * 0.4f, cx - w * 0.38f, shoulderY + h * 0.06f)
                    limb(cx + w * 0.3f, shoulderY - jump * 0.4f, cx + w * 0.38f, shoulderY + h * 0.06f)
                    limb(cx, hipY - jump, cx - w * 0.2f, h * 0.78f, strokeW * 1.1f)
                    limb(cx, hipY - jump, cx + w * 0.24f, h * 0.86f, strokeW * 1.1f)
                }
                "MOBILITY" -> {
                    val arc = phase * 0.6f
                    limb(cx - w * 0.32f, shoulderY - h * arc * 0.12f, cx - w * 0.4f, shoulderY + h * 0.08f)
                    limb(cx + w * 0.32f, shoulderY + h * arc * 0.08f, cx + w * 0.4f, shoulderY - h * 0.04f)
                    limb(cx, hipY, cx - w * 0.14f, h * 0.88f)
                    limb(cx, hipY, cx + w * 0.14f, h * 0.88f)
                }
                else -> {
                    limb(cx - w * 0.26f, shoulderY, cx - w * 0.36f, shoulderY + h * 0.16f)
                    limb(cx + w * 0.26f, shoulderY, cx + w * 0.36f, shoulderY + h * 0.16f)
                    limb(cx, hipY, cx - w * 0.14f, h * 0.9f)
                    limb(cx, hipY, cx + w * 0.14f, h * 0.9f)
                }
            }
        }
        Text(
            style.label,
            color = Color.White.copy(alpha = 0.92f),
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
