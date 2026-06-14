package com.opofit.miapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Confetti particle effect — anima cuando el usuario completa una sesión o
 * bate un PR.
 *
 * Implementación 100% Compose (sin libs externas): partículas con física
 * simple (gravedad + viento + rotación) renderizadas en un Canvas a tamaño
 * de pantalla. Self-cleanup: a los `durationMs` desaparece solo.
 *
 * Uso:
 *   var celebrar by remember { mutableStateOf(false) }
 *   Box(Modifier.fillMaxSize()) {
 *     // contenido...
 *     if (celebrar) ConfettiEffect(onFinish = { celebrar = false })
 *   }
 *
 * Cuando algo merezca celebración (PR batido, sesión completada):
 *   celebrar = true
 */

private data class ConfettiParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var rotation: Float,
    var rotationSpeed: Float,
    val color: Color,
    val sizePx: Float,
    val shape: Int   // 0=rectángulo, 1=círculo, 2=triángulo
)

@Composable
fun ConfettiEffect(
    durationMs: Int = 3500,
    particleCount: Int = 90,
    modifier: Modifier = Modifier,
    onFinish: () -> Unit = {}
) {
    val cs = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val confettiColors = remember {
        listOf(
            cs.primary,
            cs.tertiary,
            cs.secondary,
            Color(0xFFFFB300),  // amber - dorado de PR
            Color(0xFFE91E63),  // rosa
            Color(0xFF4CAF50)   // verde
        )
    }

    // Estado mutable: lista de partículas + bool para repintar
    val particles = remember {
        mutableListOf<ConfettiParticle>().also { list ->
            // Inicializamos al primer frame con dimensiones de canvas (en update)
            repeat(particleCount) {
                list.add(spawnParticle(0f, 0f, confettiColors, density))
            }
        }
    }
    var tick by remember { mutableStateOf(0L) }
    var canvasW by remember { mutableStateOf(0f) }
    var canvasH by remember { mutableStateOf(0f) }
    var inicializado by remember { mutableStateOf(false) }

    LaunchedEffect(durationMs) {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < durationMs) {
            withFrameNanos { now ->
                tick = now
            }
            // Avanzar la física en cada frame
            if (inicializado) {
                val dt = 1f / 60f
                for (p in particles) {
                    p.vy += 240f * dt  // gravedad
                    p.vx += (Random.nextFloat() - 0.5f) * 30f * dt  // viento leve
                    p.x += p.vx * dt
                    p.y += p.vy * dt
                    p.rotation += p.rotationSpeed * dt
                    // Loop suave: si sale por abajo, no respawn (terminado el ciclo)
                }
            }
        }
        onFinish()
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        if (!inicializado || canvasW != size.width || canvasH != size.height) {
            canvasW = size.width
            canvasH = size.height
            for (p in particles) {
                p.x = Random.nextFloat() * size.width
                p.y = -Random.nextFloat() * size.height * 0.3f
                p.vy = Random.nextFloat() * 80f + 60f
                p.vx = (Random.nextFloat() - 0.5f) * 180f
            }
            inicializado = true
        }
        // Acceso a tick para forzar recomposición (Compose canvas en frame loop)
        val ignored = tick
        for (p in particles) {
            if (p.y > size.height + 20) continue
            rotate(p.rotation, pivot = Offset(p.x, p.y)) {
                when (p.shape) {
                    0 -> drawRect(
                        color = p.color,
                        topLeft = Offset(p.x - p.sizePx / 2, p.y - p.sizePx / 3),
                        size = Size(p.sizePx, p.sizePx * 0.6f)
                    )
                    1 -> drawCircle(
                        color = p.color,
                        radius = p.sizePx / 2,
                        center = Offset(p.x, p.y)
                    )
                    else -> drawRect(  // simplificado
                        color = p.color,
                        topLeft = Offset(p.x - p.sizePx / 2, p.y - p.sizePx / 2),
                        size = Size(p.sizePx, p.sizePx)
                    )
                }
            }
        }
    }
}

private fun spawnParticle(
    initialX: Float,
    initialY: Float,
    colors: List<Color>,
    density: androidx.compose.ui.unit.Density
): ConfettiParticle {
    val sizePx = with(density) { (Random.nextInt(6, 14)).dp.toPx() }
    return ConfettiParticle(
        x = initialX,
        y = initialY,
        vx = (Random.nextFloat() - 0.5f) * 220f,
        vy = Random.nextFloat() * 80f + 40f,
        rotation = Random.nextFloat() * 360f,
        rotationSpeed = (Random.nextFloat() - 0.5f) * 720f,
        color = colors[Random.nextInt(colors.size)],
        sizePx = sizePx,
        shape = Random.nextInt(0, 3)
    )
}
