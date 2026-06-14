package com.opofit.miapp.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opofit.miapp.ui.theme.OpoRadii
import com.opofit.miapp.ui.theme.OpoSizes
import com.opofit.miapp.ui.theme.OpoSpacing

/**
 * Stat card profesional estilo Whoop/Strava.
 *
 * Patrón: número grande arriba a la izquierda, label debajo, delta (vs anterior)
 * arriba a la derecha en pill verde/rojo, sparkline opcional al fondo.
 *
 * Es la card que mide. Cuando se ve una cifra (kcal, km, sesiones, racha),
 * cabe aquí — no en una card genérica.
 */
@Composable
fun ProStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    deltaPct: Double? = null,
    sparkline: List<Double>? = null,
    accent: Color? = null
) {
    val color = accent ?: MaterialTheme.colorScheme.primary
    ProCard(modifier = modifier, contentPadding = OpoSpacing.lg) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon?.let {
                Icon(
                    it,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(OpoSizes.iconMd)
                )
                Spacer(Modifier.width(OpoSpacing.sm))
            }
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.weight(1f))
            deltaPct?.let {
                DeltaPill(it)
            }
        }
        Spacer(Modifier.height(OpoSpacing.xs))
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        sparkline?.takeIf { it.size >= 2 }?.let { datos ->
            Spacer(Modifier.height(OpoSpacing.sm))
            SparklineGrad(
                values = datos,
                color = color,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
            )
        }
    }
}

@Composable
private fun DeltaPill(deltaPct: Double) {
    val positive = deltaPct >= 0
    val container = if (positive) Color(0xFF1B5E20) else Color(0xFFB71C1C)
    val onContainer = Color.White
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(container.copy(alpha = 0.12f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Icon(
            if (positive) Icons.AutoMirrored.Outlined.TrendingUp else Icons.AutoMirrored.Outlined.TrendingDown,
            contentDescription = null,
            tint = container,
            modifier = Modifier.size(12.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            "${if (positive) "+" else ""}${"%.0f".format(deltaPct)}%",
            style = MaterialTheme.typography.labelSmall,
            color = container,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SparklineGrad(values: List<Double>, color: Color, modifier: Modifier = Modifier) {
    val min = values.min()
    val max = values.max()
    val range = (max - min).coerceAtLeast(0.0001)
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val path = Path()
        val stepX = size.width / (values.size - 1)
        values.forEachIndexed { i, v ->
            val x = stepX * i
            val y = (1f - ((v - min) / range).toFloat()) * size.height
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path,
            Brush.horizontalGradient(listOf(color.copy(alpha = 0.4f), color)),
            style = Stroke(width = 3f, cap = StrokeCap.Round)
        )
    }
}

/**
 * Empty state ilustrado: icono grande tintado + título + mensaje + CTA opcional.
 *
 * Antes algunas pantallas dejaban un texto plano "No hay datos". Pro: icono
 * grande con contenedor circular, jerarquía visual clara, llamada a la acción.
 */
@Composable
fun ProEmptyState(
    title: String,
    message: String? = null,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    cta: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(OpoSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(OpoSpacing.md)
    ) {
        icon?.let {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        androidx.compose.foundation.shape.CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    it,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        message?.takeIf { it.isNotBlank() }?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        cta?.invoke()
    }
}

/**
 * Skeleton loader con shimmer animado. Sustituye al CircularProgressIndicator
 * cuando se está cargando una LISTA o una CARD concreta: el usuario ve la
 * forma del contenido antes de que llegue (estándar Strava / Apple Fitness).
 */
@Composable
fun ProSkeleton(
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 16.dp,
    shape: RoundedCornerShape = OpoRadii.sm
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer-alpha"
    )
    Box(
        modifier
            .heightIn(min = height)
            .height(height)
            .background(
                MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.15f),
                shape
            )
    )
}

/** Skeleton card: simula un ProCard con 3 líneas y un avatar. */
@Composable
fun ProCardSkeleton(modifier: Modifier = Modifier) {
    ProCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProSkeleton(Modifier.size(40.dp), height = 40.dp, shape = OpoRadii.sm)
            Spacer(Modifier.width(OpoSpacing.md))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ProSkeleton(Modifier.fillMaxWidth(0.6f), height = 14.dp)
                ProSkeleton(Modifier.fillMaxWidth(0.4f), height = 10.dp)
            }
        }
        Spacer(Modifier.height(OpoSpacing.md))
        ProSkeleton(Modifier.fillMaxWidth(), height = 12.dp)
    }
}
