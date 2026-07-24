package com.opofit.miapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Medallón OpoFit — insignia circular naranja con montaña + estrella (cima
// conquistada). Es una marca AUTOCONTENIDA (tiene su propio fondo naranja),
// así que luce igual sobre fondo claro (Amanecer) u oscuro (Aurora): no
// necesita adaptarse al tema. El nombre "OpoFit" se muestra como texto aparte.
private val GradTop  = Color(0xFFFDBA74)
private val GradMid  = Color(0xFFF97316)
private val GradBot  = Color(0xFFEA580C)
private val DiscInner = Color(0xFFB23C0A)
private val StarCream = Color(0xFFFFE3C2)

// Paths en espacio 108x108 (mismo que el icono del launcher).
private const val MOUNTAIN = "M28,72 L46,46 L55,57 L67,40 L80,72 Z"
private const val STAR =
    "M54,22 l3,6 6.6,1 -4.8,4.6 1.1,6.5 -5.9,-3.1 -5.9,3.1 1.1,-6.5 -4.8,-4.6 6.6,-1 z"

/**
 * Logo OpoFit — medallón insignia. Mismo diseño que el icono de la app.
 * [onDarkBackground] se conserva por compatibilidad pero ya no afecta: el
 * medallón es autocontenido y se ve bien sobre cualquier fondo.
 */
@Composable
fun OpoFitLogo(
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    @Suppress("UNUSED_PARAMETER") onDarkBackground: Boolean = false
) {
    val mountain = remember2(MOUNTAIN)
    val star = remember2(STAR)
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.minDimension / 108f
        val ox = (this.size.width - 108f * s) / 2f
        val oy = (this.size.height - 108f * s) / 2f
        translate(ox, oy) {
            scale(s, s, pivot = Offset.Zero) {
                val c = Offset(54f, 54f)
                // Moneda con degradado
                drawCircle(
                    brush = Brush.verticalGradient(
                        listOf(GradTop, GradMid, GradBot),
                        startY = 2f, endY = 106f
                    ),
                    radius = 52f, center = c
                )
                // Aro exterior blanco
                drawCircle(Color.White, radius = 52f, center = c, style = Stroke(width = 3f))
                // Disco interior (dos tonos → profundidad de medalla)
                drawCircle(DiscInner, radius = 43f, center = c)
                drawCircle(Color.White.copy(alpha = 0.5f), radius = 43f, center = c, style = Stroke(1.2f))
                // Remaches decorativos
                listOf(
                    Offset(54f, 8f), Offset(100f, 54f), Offset(8f, 54f),
                    Offset(86f, 22f), Offset(22f, 22f)
                ).forEach { drawCircle(Color.White, radius = 1.8f, center = it) }
                // Cima + estrella
                drawPath(mountain, Color.White)
                drawPath(star, StarCream)
            }
        }
    }
}

@Composable
private fun remember2(pathData: String) =
    androidx.compose.runtime.remember(pathData) {
        PathParser().parsePathString(pathData).toPath()
    }
