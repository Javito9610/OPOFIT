package com.opofit.miapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.opofit.miapp.ui.theme.AccentOrange

private val LogoNavy = Color(0xFF1B2A4A)
private val LogoWhite = Color(0xFFFFFFFF)

/**
 * Logo C) Pico ascenso — chevron + OF + diamante naranja. Mismo diseño que el launcher.
 */
@Composable
fun OpoFitLogo(
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    onDarkBackground: Boolean = false
) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.minDimension / 108f
        val offsetX = (this.size.width - 108f * s) / 2f
        val offsetY = (this.size.height - 108f * s) / 2f
        translate(offsetX, offsetY) {
            scale(s, s, pivot = Offset.Zero) {
                drawOpoFitMark(lightMark = onDarkBackground)
            }
        }
    }
}

internal fun DrawScope.drawOpoFitMark(lightMark: Boolean = true) {
    val markColor = if (lightMark) LogoWhite else LogoNavy

    drawPath(
        path = peakFramePath(),
        color = markColor,
        style = Stroke(width = 3.5f, join = StrokeJoin.Round, cap = StrokeCap.Round)
    )

    drawPath(path = peakDiamondPath(), color = AccentOrange)

    drawCircle(
        color = markColor,
        radius = 11f,
        center = Offset(42f, 58f),
        style = Stroke(width = 5f, cap = StrokeCap.Round)
    )

    drawRect(markColor, topLeft = Offset(50f, 46f), size = Size(5f, 28f))
    drawRect(markColor, topLeft = Offset(55f, 46f), size = Size(15f, 6f))
    drawRect(markColor, topLeft = Offset(55f, 58f), size = Size(11f, 5f))
}

private fun peakFramePath(): Path = Path().apply {
    moveTo(54f, 12f)
    lineTo(80f, 44f)
    lineTo(80f, 80f)
    lineTo(54f, 66f)
    lineTo(28f, 80f)
    lineTo(28f, 44f)
    close()
}

private fun peakDiamondPath(): Path = Path().apply {
    moveTo(54f, 8f)
    lineTo(59f, 14f)
    lineTo(54f, 20f)
    lineTo(49f, 14f)
    close()
}
