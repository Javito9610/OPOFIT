package com.opofit.miapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.opofit.miapp.data.responsemodels.PostStats
import com.opofit.miapp.gps.service.RoutePoint
import com.opofit.miapp.gps.util.GpsMetrics
import com.opofit.miapp.utils.MediaUrlUtil
import androidx.compose.foundation.Canvas
import kotlin.math.max

@Composable
fun ShareCardPreview(
    titulo: String,
    stats: PostStats?,
    fotoFondo: String? = null,
    usarFoto: Boolean = true,
    routePoints: List<RoutePoint> = emptyList(),
    modifier: Modifier = Modifier
) {
    val tipo = stats?.tipo?.uppercase().orEmpty()
    val emoji = when {
        tipo.contains("RUN") || tipo == "CARRERA" -> "🏃"
        tipo.contains("BIKE") || tipo == "BICI" -> "🚴"
        tipo.contains("WALK") || tipo == "PASEO" -> "🚶"
        tipo == "ENTRENO" -> "💪"
        else -> "⚡"
    }
    val accent = Color(0xFFFF5722)

    Box(
        modifier = modifier
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(16.dp))
    ) {
        if (usarFoto && !fotoFondo.isNullOrBlank()) {
            AsyncImage(
                model = MediaUrlUtil.resolveAvatar(fotoFondo) ?: fotoFondo,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF1A237E), Color(0xFF0D1B3E), Color(0xFF000000))
                        )
                    )
            )
        }

        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.45f to Color.Black.copy(alpha = 0.15f),
                        1f to Color.Black.copy(alpha = 0.82f)
                    )
                )
        )

        if (routePoints.size >= 2) {
            Canvas(Modifier.fillMaxSize().padding(24.dp)) {
                val pts = routePoints
                val minLat = pts.minOf { it.lat }
                val maxLat = pts.maxOf { it.lat }
                val minLng = pts.minOf { it.lng }
                val maxLng = pts.maxOf { it.lng }
                val latSpan = max(maxLat - minLat, 0.0001)
                val lngSpan = max(maxLng - minLng, 0.0001)
                val pad = 0.08f
                fun map(lat: Double, lng: Double): Offset {
                    val x = ((lng - minLng) / lngSpan).toFloat()
                    val y = 1f - ((lat - minLat) / latSpan).toFloat()
                    return Offset(
                        (pad + x * (1f - 2 * pad)) * size.width,
                        (pad + y * (1f - 2 * pad)) * size.height * 0.55f + size.height * 0.08f
                    )
                }
                val path = Path()
                pts.forEachIndexed { i, p ->
                    val o = map(p.lat, p.lng)
                    if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
                }
                drawPath(path, accent.copy(alpha = 0.35f), style = Stroke(size.width * 0.02f, cap = StrokeCap.Round))
                drawPath(path, accent, style = Stroke(size.width * 0.008f, cap = StrokeCap.Round))
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("OpoFit", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text(emoji, fontSize = 28.sp)
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    titulo.ifBlank { "Mi entrenamiento" },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    maxLines = 2
                )
                val hero = when {
                    (stats?.distanciaM ?: 0.0) > 0 ->
                        GpsMetrics.formatDistance(stats!!.distanciaM!!)
                    (stats?.duracionSec ?: 0) > 0 ->
                        GpsMetrics.formatDuration(stats!!.duracionSec!!)
                    (stats?.ejercicios ?: 0) > 0 ->
                        "${stats!!.ejercicios} ejercicios"
                    else -> "Entreno completado"
                }
                Text(hero, color = Color.White, fontWeight = FontWeight.Black, fontSize = 42.sp)

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    stats?.duracionSec?.takeIf { it > 0 }?.let {
                        StatChip("Tiempo", GpsMetrics.formatDuration(it))
                    }
                    stats?.ritmoMedioSpkm?.takeIf { it > 0 }?.let {
                        StatChip("Ritmo", "${GpsMetrics.formatPace(it)}/km")
                    }
                    stats?.desnivelM?.takeIf { it > 0 }?.let {
                        StatChip("Desnivel", "+${it.toInt()} m")
                    }
                    stats?.avgHrBpm?.let { StatChip("Pulso", "$it lpm") }
                    stats?.kcal?.takeIf { it > 0 }?.let { StatChip("kcal", "$it") }
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Column {
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}
