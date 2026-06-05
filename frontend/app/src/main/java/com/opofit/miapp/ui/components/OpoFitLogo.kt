package com.opofit.miapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val BlueTop = Color(0xFF2196F3)
private val BlueMid = Color(0xFF1565C0)
private val BlueDeep = Color(0xFF0D47A1)
private val White = Color.White
private val Orange = Color(0xFFFF6D00)

/**
 * Logo OpoFit — escudo blanco, O/F varsity octogonales, rayo naranja (dibujado en Canvas).
 */
@Composable
fun OpoFitLogo(
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    onDarkBackground: Boolean = false
) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.minDimension / 108f

        fun shieldPath(): Path = Path().apply {
            moveTo(54f * s, 7f * s)
            cubicTo(72f * s, 7f * s, 90f * s, 13f * s, 93f * s, 25f * s)
            lineTo(95f * s, 51f * s)
            cubicTo(95f * s, 75f * s, 78f * s, 91f * s, 54f * s, 99f * s)
            cubicTo(30f * s, 91f * s, 13f * s, 75f * s, 13f * s, 51f * s)
            lineTo(15f * s, 25f * s)
            cubicTo(18f * s, 13f * s, 36f * s, 7f * s, 54f * s, 7f * s)
            close()
        }

        fun octagonRing(
            cx: Float, cy: Float,
            outerW: Float, outerH: Float, outerC: Float,
            innerW: Float, innerH: Float, innerC: Float
        ): Path {
            fun ring(hW: Float, hH: Float, chamfer: Float): Path {
                val hw = hW * s
                val hh = hH * s
                val c = chamfer * s
                val x = cx * s
                val y = cy * s
                return Path().apply {
                    moveTo(x - hw + c, y - hh)
                    lineTo(x + hw - c, y - hh)
                    lineTo(x + hw, y - hh + c)
                    lineTo(x + hw, y + hh - c)
                    lineTo(x + hw - c, y + hh)
                    lineTo(x - hw + c, y + hh)
                    lineTo(x - hw, y + hh - c)
                    lineTo(x - hw, y - hh + c)
                    close()
                }
            }
            return Path().apply {
                fillType = PathFillType.EvenOdd
                addPath(ring(outerW / 2f, outerH / 2f, outerC))
                addPath(ring(innerW / 2f, innerH / 2f, innerC))
            }
        }

        val bgGradient = Brush.linearGradient(
            colors = listOf(BlueTop, BlueMid, BlueDeep),
            start = Offset(0f, 0f),
            end = Offset(108f * s, 108f * s)
        )

        if (!onDarkBackground) {
            drawRect(brush = bgGradient, size = Size(108f * s, 108f * s))
        }

        val shield = shieldPath()
        drawPath(
            shield,
            color = White,
            style = Stroke(
                width = 5.5f * s,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // O — octogonal varsity (izquierda)
        drawPath(
            octagonRing(32f, 54f, 24f, 40f, 4.5f, 13f, 26f, 3f),
            White,
            style = Fill
        )

        // F — bloque varsity con esquinas biseladas
        val fLetter = Path().apply {
            moveTo(68f * s, 34f * s)
            lineTo(88f * s, 34f * s)
            lineTo(88f * s, 42f * s)
            lineTo(72f * s, 42f * s)
            lineTo(72f * s, 50f * s)
            lineTo(86f * s, 50f * s)
            lineTo(86f * s, 58f * s)
            lineTo(72f * s, 58f * s)
            lineTo(72f * s, 70f * s)
            lineTo(68f * s, 74f * s)
            lineTo(64f * s, 70f * s)
            lineTo(64f * s, 38f * s)
            close()
        }
        drawPath(fLetter, White, style = Fill)

        // Rayo — diagonal naranja, encima del monograma
        val bolt = Path().apply {
            moveTo(59f * s, 12f * s)
            lineTo(47f * s, 40f * s)
            lineTo(55f * s, 40f * s)
            lineTo(43f * s, 76f * s)
            lineTo(73f * s, 42f * s)
            lineTo(61f * s, 42f * s)
            lineTo(69f * s, 12f * s)
            close()
        }
        drawPath(bolt, Orange, style = Fill)
    }
}
