package com.opofit.miapp.ui.components

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment as ComposeAlignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * Charts profesionales basados en Canvas (sin dependencias externas).
 * Soportan animación, gradientes Material 3, ejes con etiquetas y tooltips simples.
 */

private data class ChartGeometry(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val minY: Double,
    val maxY: Double,
    val invertY: Boolean = false
) {
    fun xFor(index: Int, count: Int): Float =
        if (count <= 1) left + width / 2f else left + width * index / (count - 1)

    fun yFor(value: Double): Float {
        // Bug previo: cuando invertY=true se pasaba minY=maxRaw+pad y maxY=minRaw-pad
        // → maxY - minY era negativo → coerceAtLeast(0.0001) lo aplastaba a ~0
        // → TODOS los puntos salían en la misma altura (línea plana). Ahora minY/maxY
        // siempre representan el rango real (min ≤ max) y la inversión se aplica al final.
        val range = (maxY - minY).coerceAtLeast(0.0001)
        val norm = ((value - minY) / range).coerceIn(0.0, 1.0)
        return if (invertY) {
            top + height * norm.toFloat() // valor mayor abajo (p.ej. ritmo lento)
        } else {
            top + height * (1f - norm.toFloat()) // valor mayor arriba (estándar)
        }
    }
}

@Composable
fun LineAreaChart(
    values: List<Double>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    fillTop: Color = lineColor.copy(alpha = 0.35f),
    fillBottom: Color = lineColor.copy(alpha = 0.0f),
    showDots: Boolean = false,
    yFormatter: (Double) -> String = { "%.1f".format(it) },
    xLabels: List<String> = emptyList(),
    pointLabels: List<String>? = null,
    yAxisLabel: String? = null,
    invertY: Boolean = false,
    interactive: Boolean = true
) {
    if (values.size < 2) {
        EmptyState("Faltan datos para el gráfico", modifier)
        return
    }
    val minRaw = values.min()
    val maxRaw = values.max()
    // pad mínimo de 0.5 evita rango cero cuando todos los valores son iguales,
    // pero también garantiza un mínimo de "espacio" en gráficos con poca varianza.
    val pad = ((maxRaw - minRaw) * 0.08).coerceAtLeast(0.5)
    // Siempre min ≤ max. La inversión visual se hace en ChartGeometry.yFor.
    val minY = minRaw - pad
    val maxY = maxRaw + pad

    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 900, easing = LinearOutSlowInEasing),
        label = "lineProgress"
    )

    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val axisColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall
    val tooltipStyle = MaterialTheme.typography.labelMedium.copy(color = Color.White, fontWeight = FontWeight.SemiBold)
    val tooltipMetaStyle = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.85f))

    var hoverIdx by remember { mutableStateOf<Int?>(null) }

    val interactionModifier = if (interactive) {
        Modifier
            .pointerInput(values.size) {
                detectTapGestures(
                    onTap = { hoverIdx = if (hoverIdx != null) null else 0 }
                )
            }
            .pointerInput(values.size) {
                detectDragGestures(
                    onDragStart = { offset ->
                        hoverIdx = nearestIndex(offset.x, values.size, size.width.toFloat())
                    },
                    onDragEnd = { /* keep tooltip until next tap */ },
                    onDragCancel = { },
                    onDrag = { change, _ ->
                        hoverIdx = nearestIndex(change.position.x, values.size, size.width.toFloat())
                    }
                )
            }
    } else Modifier

    Canvas(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
            .then(interactionModifier)
    ) {
        val labelWidth = 44.dp.toPx()
        val labelHeight = 22.dp.toPx()
        val topPadding = if (yAxisLabel != null) 22.dp.toPx() else 6.dp.toPx()
        val geo = ChartGeometry(
            left = labelWidth,
            top = topPadding,
            width = size.width - labelWidth - 4.dp.toPx(),
            height = size.height - labelHeight - topPadding,
            minY = minY,
            maxY = maxY,
            invertY = invertY
        )

        if (yAxisLabel != null) {
            val res = textMeasurer.measure(yAxisLabel, labelStyle.copy(color = labelColor, fontWeight = FontWeight.SemiBold))
            drawText(textLayoutResult = res, topLeft = Offset(geo.left, 0f))
        }

        // Grid + Y axis ticks
        for (i in 0..4) {
            val y = geo.top + geo.height * i.toFloat() / 4f
            drawLine(
                gridColor,
                Offset(geo.left, y),
                Offset(geo.left + geo.width, y),
                strokeWidth = 1f,
                pathEffect = if (i > 0 && i < 4) PathEffect.dashPathEffect(floatArrayOf(6f, 6f)) else null
            )
            // Etiquetas Y coherentes con yFor:
            // invertY=false → arriba=maxY (estándar Cartesiano: valor alto arriba)
            // invertY=true  → arriba=minY (convención Strava para ritmo: rápido arriba)
            // Así la posición del label coincide con dónde se dibuja la línea para ese valor.
            val v = if (invertY) {
                minY + (maxY - minY) * i.toDouble() / 4.0
            } else {
                maxY - (maxY - minY) * i.toDouble() / 4.0
            }
            val txt = yFormatter(v)
            val result = textMeasurer.measure(txt, labelStyle.copy(color = labelColor))
            drawText(
                textLayoutResult = result,
                topLeft = Offset(geo.left - result.size.width - 4.dp.toPx(), y - result.size.height / 2f)
            )
        }

        // X axis baseline
        drawLine(
            axisColor,
            Offset(geo.left, geo.top + geo.height),
            Offset(geo.left + geo.width, geo.top + geo.height),
            strokeWidth = 2f
        )

        val visibleCount = ((values.size) * progress).coerceAtLeast(2f).toInt().coerceAtMost(values.size)
        val path = Path()
        val area = Path()
        for (i in 0 until visibleCount) {
            val x = geo.xFor(i, values.size)
            val y = geo.yFor(values[i])
            if (i == 0) {
                path.moveTo(x, y)
                area.moveTo(x, geo.top + geo.height)
                area.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                area.lineTo(x, y)
            }
        }
        if (visibleCount >= 2) {
            val lastX = geo.xFor(visibleCount - 1, values.size)
            area.lineTo(lastX, geo.top + geo.height)
            area.close()
            drawPath(
                area,
                brush = Brush.verticalGradient(
                    colors = listOf(fillTop, fillBottom),
                    startY = geo.top,
                    endY = geo.top + geo.height
                )
            )
            drawPath(path, color = lineColor, style = Stroke(width = 6f, cap = StrokeCap.Round))
        }

        if (showDots) {
            for (i in 0 until visibleCount) {
                val cx = geo.xFor(i, values.size)
                val cy = geo.yFor(values[i])
                drawCircle(lineColor, radius = 5f, center = Offset(cx, cy))
                drawCircle(Color.White, radius = 2.5f, center = Offset(cx, cy))
            }
        }

        if (xLabels.isNotEmpty()) {
            val step = (values.size.toFloat() - 1f) / (xLabels.size - 1).coerceAtLeast(1)
            xLabels.forEachIndexed { i, lab ->
                val xi = (step * i).roundToInt().coerceIn(0, values.size - 1)
                val x = geo.xFor(xi, values.size)
                val res = textMeasurer.measure(lab, labelStyle.copy(color = labelColor))
                drawText(
                    textLayoutResult = res,
                    topLeft = Offset(x - res.size.width / 2f, geo.top + geo.height + 4.dp.toPx())
                )
            }
        }

        // Tooltip
        val idx = hoverIdx
        if (idx != null && idx in values.indices) {
            val v = values[idx]
            val cx = geo.xFor(idx, values.size)
            val cy = geo.yFor(v)
            // vertical guide
            drawLine(
                axisColor.copy(alpha = 0.55f),
                Offset(cx, geo.top),
                Offset(cx, geo.top + geo.height),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f))
            )
            // highlight dot
            drawCircle(Color.White, radius = 8f, center = Offset(cx, cy))
            drawCircle(lineColor, radius = 6f, center = Offset(cx, cy))

            val mainText = yFormatter(v)
            val metaText = pointLabels?.getOrNull(idx).orEmpty()
            val tooltipMain = textMeasurer.measure(mainText, tooltipStyle)
            val tooltipMeta = if (metaText.isNotEmpty()) textMeasurer.measure(metaText, tooltipMetaStyle) else null
            val w = maxOf(tooltipMain.size.width, tooltipMeta?.size?.width ?: 0) + 16.dp.toPx().toInt()
            val h = tooltipMain.size.height + (tooltipMeta?.size?.height ?: 0) + 12.dp.toPx().toInt()
            var tx = cx - w / 2f
            tx = tx.coerceIn(geo.left, geo.left + geo.width - w)
            var ty = cy - h - 12.dp.toPx()
            if (ty < geo.top) ty = cy + 14.dp.toPx()
            drawRoundRect(
                color = Color(0xFF1E1E1E).copy(alpha = 0.92f),
                topLeft = Offset(tx, ty),
                size = Size(w.toFloat(), h.toFloat()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
            )
            drawText(
                textLayoutResult = tooltipMain,
                topLeft = Offset(tx + 8.dp.toPx(), ty + 6.dp.toPx())
            )
            tooltipMeta?.let {
                drawText(
                    textLayoutResult = it,
                    topLeft = Offset(tx + 8.dp.toPx(), ty + 6.dp.toPx() + tooltipMain.size.height)
                )
            }
        }
    }
}

private fun nearestIndex(x: Float, count: Int, totalWidth: Float): Int {
    if (count <= 1) return 0
    val labelWidth = 44f * 2.5f // approx px
    val left = labelWidth
    val width = totalWidth - left - 16f
    val rel = ((x - left) / width).coerceIn(0f, 1f)
    return (rel * (count - 1)).roundToInt().coerceIn(0, count - 1)
}

@Suppress("UNUSED") private val _unusedTextStyle: TextStyle = TextStyle.Default
@Suppress("UNUSED") private val _unusedAbs = 0.0.absoluteValue

@Composable
fun ColumnsChart(
    values: List<Double>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    valueLabel: (Double) -> String = { v -> if (v >= 100) "${v.toInt()}" else "%.1f".format(v) }
) {
    if (values.isEmpty()) {
        EmptyState("Sin datos", modifier)
        return
    }
    val maxV = values.max().coerceAtLeast(0.0001)
    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 700, easing = LinearOutSlowInEasing),
        label = "colsProgress"
    )

    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall

    Canvas(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        val labelArea = 18.dp.toPx()
        val valueArea = 14.dp.toPx()
        val chartTop = valueArea
        val chartBottom = size.height - labelArea
        val chartHeight = chartBottom - chartTop
        val n = values.size
        val gap = 6.dp.toPx()
        val colWidth = ((size.width - gap * (n - 1)) / n).coerceAtLeast(4f)

        for (i in 0..3) {
            val y = chartTop + chartHeight * i / 3f
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        }

        values.forEachIndexed { i, v ->
            val x = (colWidth + gap) * i
            val targetH = (v / maxV).toFloat() * chartHeight
            val h = targetH * progress
            val topY = chartBottom - h
            drawRoundRect(
                color = color,
                topLeft = Offset(x, topY),
                size = Size(colWidth, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f),
                style = Fill
            )
            val lab = labels.getOrNull(i) ?: ""
            if (lab.isNotEmpty()) {
                val res = textMeasurer.measure(lab, labelStyle.copy(color = labelColor))
                drawText(textLayoutResult = res, topLeft = Offset(x + colWidth / 2f - res.size.width / 2f, chartBottom + 2.dp.toPx()))
            }
            if (progress > 0.7f && v > 0) {
                val res = textMeasurer.measure(
                    valueLabel(v),
                    labelStyle.copy(color = labelColor)
                )
                drawText(textLayoutResult = res, topLeft = Offset(x + colWidth / 2f - res.size.width / 2f, topY - res.size.height - 2.dp.toPx()))
            }
        }
    }
}

@Composable
fun Sparkline(
    values: List<Double>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    if (values.size < 2) return
    val min = values.min()
    val max = values.max()
    val range = (max - min).coerceAtLeast(0.0001)
    Canvas(modifier = modifier) {
        val path = Path()
        val stepX = size.width / (values.size - 1)
        values.forEachIndexed { i, v ->
            val x = stepX * i
            val y = (1f - ((v - min) / range).toFloat()) * size.height
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(width = 4f, cap = StrokeCap.Round))
    }
}

@Composable
fun CalendarHeatmap(
    countsByDate: Map<String, Int>,
    weeks: Int = 16,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    emptyColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
) {
    val today = remember {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        cal.time
    }
    val df = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US) }
    val maxCount = (countsByDate.values.maxOrNull() ?: 0).coerceAtLeast(1)

    Canvas(modifier = modifier) {
        val totalDays = weeks * 7
        val cell = (size.width / weeks).coerceAtMost(size.height / 7f)
        val gap = cell * 0.18f
        val cal = java.util.Calendar.getInstance()
        cal.time = today
        cal.add(java.util.Calendar.DAY_OF_YEAR, -(totalDays - 1))
        for (i in 0 until totalDays) {
            val dow = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
            val week = i / 7
            val key = df.format(cal.time)
            val count = countsByDate[key] ?: 0
            val intensity = (count.toFloat() / maxCount).coerceIn(0f, 1f)
            val color = if (count == 0) emptyColor
            else lerp(emptyColor, activeColor, 0.35f + 0.65f * intensity)
            val x = week * cell + gap / 2f
            val y = dow * cell + gap / 2f
            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(cell - gap, cell - gap),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cell * 0.18f, cell * 0.18f)
            )
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
    }
}

private fun lerp(a: Color, b: Color, t: Float): Color = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = a.alpha + (b.alpha - a.alpha) * t
)

@Composable
fun MetricBadge(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    sublabel: String? = null
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.58f))
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            sublabel?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyState(text: String, modifier: Modifier) {
    Box(
        modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

data class DonutSlice(val label: String, val value: Double, val color: Color)

@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier,
    centerTitle: String = "",
    centerSubtitle: String = ""
) {
    if (slices.isEmpty()) {
        EmptyState("Sin datos", modifier)
        return
    }
    val total = slices.sumOf { it.value }.coerceAtLeast(0.0001)
    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 800, easing = LinearOutSlowInEasing),
        label = "donutProgress"
    )
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface)
    val titleStyle = MaterialTheme.typography.titleLarge.copy(
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold
    )
    val subtitleStyle = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)

    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = ComposeAlignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(140.dp),
            contentAlignment = ComposeAlignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 26.dp.toPx()
                val arcSize = androidx.compose.ui.geometry.Size(
                    size.minDimension - stroke,
                    size.minDimension - stroke
                )
                val topLeft = androidx.compose.ui.geometry.Offset(
                    (size.width - arcSize.width) / 2f,
                    (size.height - arcSize.height) / 2f
                )
                var start = -90f
                slices.forEach { slice ->
                    val sweep = ((slice.value / total) * 360.0 * progress).toFloat()
                    drawArc(
                        color = slice.color,
                        startAngle = start,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    start += sweep
                }
            }
            if (centerTitle.isNotEmpty()) {
                Column(horizontalAlignment = ComposeAlignment.CenterHorizontally) {
                    Text(centerTitle, style = titleStyle)
                    if (centerSubtitle.isNotEmpty()) {
                        Text(centerSubtitle, style = subtitleStyle)
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            slices.forEach { s ->
                Row(verticalAlignment = ComposeAlignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(s.color, RoundedCornerShape(3.dp))
                    )
                    val pct = (s.value / total * 100).toInt()
                    Text(
                        "  ${s.label}  ·  ${formatValue(s.value)}  ·  $pct%",
                        style = labelStyle
                    )
                }
            }
        }
    }
    // unused for now but keeps import alive when textMeasurer is needed in future
    @Suppress("UNUSED") val _t = textMeasurer
}

private fun formatValue(v: Double): String {
    return if (v == v.toInt().toDouble()) v.toInt().toString() else "%.1f".format(v)
}

/** Permite a otros componibles dibujar segmentos pintados según índices de buckets. */
fun DrawScope.drawColoredPath(
    points: List<Offset>,
    bucketByPoint: List<Int>,
    palette: List<Color>,
    strokeWidth: Float
) {
    if (points.size < 2) return
    for (i in 1 until points.size) {
        val bucket = bucketByPoint.getOrNull(i)?.coerceIn(0, palette.size - 1) ?: 0
        drawLine(
            color = palette[bucket],
            start = points[i - 1],
            end = points[i],
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}
