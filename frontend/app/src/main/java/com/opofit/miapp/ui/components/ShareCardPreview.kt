package com.opofit.miapp.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.clickable
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.opofit.miapp.data.responsemodels.PostStats
import com.opofit.miapp.gps.service.RoutePoint
import com.opofit.miapp.gps.util.GpsMetrics
import androidx.compose.foundation.Canvas
import kotlin.math.max

/**
 * Estado HOIST del editor de tarjeta de compartir. Vive en la pantalla del editor
 * y se pasa a ShareCardPreview tanto durante la edición como durante el render a
 * bitmap. Sin esto, cada llamada a ShareCardPreview crea sus propios `remember`
 * → el offset/scale editado por el usuario se PIERDE al renderizar para exportar.
 */
class ShareEditorState {
    var fotoOffset by mutableStateOf(Offset.Zero)
    var fotoScale by mutableStateOf(1f)
    var routeOffset by mutableStateOf(Offset.Zero)
    var routeScale by mutableStateOf(1f)
    var modoActivo by mutableStateOf("ruta")
    fun reset() {
        fotoOffset = Offset.Zero
        fotoScale = 1f
        routeOffset = Offset.Zero
        routeScale = 1f
    }
    fun isDirty(): Boolean =
        fotoOffset != Offset.Zero || fotoScale != 1f ||
            routeOffset != Offset.Zero || routeScale != 1f
}

@Composable
fun rememberShareEditorState(): ShareEditorState = remember { ShareEditorState() }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShareCardPreview(
    titulo: String,
    stats: PostStats?,
    fotoFondoUri: Uri? = null,
    fotoFondoBitmap: Bitmap? = null,
    usarFoto: Boolean = true,
    routePoints: List<RoutePoint> = emptyList(),
    modifier: Modifier = Modifier,
    // editing=true: muestra el chip de instrucciones, el botón reset, etc.
    // editing=false: composición limpia para exportar a bitmap (sin UI editor).
    // Antes los controles se exportaban a la foto que iba a Instagram.
    editing: Boolean = true,
    // Estado hoist. Si es null usa estado local (compatibilidad). Si pasamos el state
    // desde el editor, las posiciones se preservan en el render bitmap final.
    editorState: ShareEditorState? = null
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

    // Estado hoist: si nos lo pasan desde el editor lo usamos; si no, local.
    val localState = editorState ?: rememberShareEditorState()
    val fotoOffset = localState.fotoOffset
    val fotoScale = localState.fotoScale
    val routeOffset = localState.routeOffset
    val routeScale = localState.routeScale
    val modoActivo = localState.modoActivo

    Box(
        modifier = modifier
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(16.dp))
    ) {
        // Capa de foto con gestos. graphicsLayer aplica el offset+scale.
        val fotoGestureMod = if (editing && usarFoto && modoActivo == "foto") {
            Modifier
                .fillMaxSize()
                .pointerInput("foto") {
                    detectTransformGestures { _, pan, zoom, _ ->
                        localState.fotoOffset = localState.fotoOffset + pan
                        localState.fotoScale = (localState.fotoScale * zoom).coerceIn(0.5f, 3f)
                    }
                }
        } else {
            Modifier.fillMaxSize()
        }
        val fotoTransformMod = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = fotoScale
                scaleY = fotoScale
                translationX = fotoOffset.x
                translationY = fotoOffset.y
            }

        Box(modifier = fotoGestureMod) {
            if (usarFoto && fotoFondoBitmap != null) {
                Image(
                    bitmap = fotoFondoBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = fotoTransformMod,
                    contentScale = ContentScale.Crop
                )
            } else if (usarFoto && fotoFondoUri != null) {
                AsyncImage(
                    model = fotoFondoUri,
                    contentDescription = null,
                    modifier = fotoTransformMod,
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

        // Ruta movible y redimensionable (solo si hay puntos).
        if (routePoints.size >= 2) {
            val rutaGestureMod = if (editing && modoActivo == "ruta") {
                Modifier
                    .fillMaxSize()
                    .pointerInput("ruta") {
                        detectTransformGestures { _, pan, zoom, _ ->
                            localState.routeOffset = localState.routeOffset + pan
                            localState.routeScale = (localState.routeScale * zoom).coerceIn(0.4f, 3f)
                        }
                    }
            } else {
                Modifier.fillMaxSize()
            }
            Box(modifier = rutaGestureMod) {
                Canvas(Modifier.fillMaxSize().padding(24.dp)) {
                    val pts = routePoints
                    val minLat = pts.minOf { it.lat }
                    val maxLat = pts.maxOf { it.lat }
                    val minLng = pts.minOf { it.lng }
                    val maxLng = pts.maxOf { it.lng }
                    val latSpan = max(maxLat - minLat, 0.0001)
                    val lngSpan = max(maxLng - minLng, 0.0001)
                    val pad = 0.08f
                    // Centro de la ruta para pivotar el escalado desde ahí
                    val centerX = size.width / 2f
                    val centerY = size.height * 0.55f * 0.5f + size.height * 0.08f
                    fun map(lat: Double, lng: Double): Offset {
                        val x = ((lng - minLng) / lngSpan).toFloat()
                        val y = 1f - ((lat - minLat) / latSpan).toFloat()
                        val baseX = (pad + x * (1f - 2 * pad)) * size.width
                        val baseY = (pad + y * (1f - 2 * pad)) * size.height * 0.55f + size.height * 0.08f
                        // Aplica escala desde el centro + offset por drag
                        val scaledX = centerX + (baseX - centerX) * routeScale
                        val scaledY = centerY + (baseY - centerY) * routeScale
                        return Offset(scaledX + routeOffset.x, scaledY + routeOffset.y)
                    }
                    val path = Path()
                    pts.forEachIndexed { i, p ->
                        val o = map(p.lat, p.lng)
                        if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
                    }
                    drawPath(path, accent.copy(alpha = 0.35f), style = Stroke(size.width * 0.02f * routeScale, cap = StrokeCap.Round))
                    drawPath(path, accent, style = Stroke(size.width * 0.008f * routeScale, cap = StrokeCap.Round))
                }
            }
        }

        // ── CONTROLES DE EDITOR ──────────────────────────────────────────
        // Solo visibles cuando editing=true (en pantalla de edición).
        // Cuando renderizamos a bitmap para exportar a IG/WhatsApp, editing=false
        // y estos controles NO se dibujan. Antes salían en la foto exportada.
        if (editing) {
            // Selector Foto / Ruta arriba-izquierda
            val hayFoto = usarFoto && (fotoFondoBitmap != null || fotoFondoUri != null)
            val hayRuta = routePoints.size >= 2
            if (hayFoto || hayRuta) {
                Row(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 56.dp, start = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (hayFoto) {
                        EditorPill(
                            text = "Foto",
                            selected = modoActivo == "foto",
                            onClick = { localState.modoActivo = "foto" }
                        )
                    }
                    if (hayRuta) {
                        EditorPill(
                            text = "Ruta",
                            selected = modoActivo == "ruta",
                            onClick = { localState.modoActivo = "ruta" }
                        )
                    }
                }
            }
            // Botón reset arriba-derecha (siempre visible mientras editas)
            val movido = fotoOffset != Offset.Zero || fotoScale != 1f ||
                routeOffset != Offset.Zero || routeScale != 1f
            Row(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 56.dp, end = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!movido) {
                    Box(
                        Modifier
                            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Filled.OpenWith, null,
                                tint = Color.White,
                                modifier = Modifier.padding(2.dp)
                            )
                            Text("Arrastra · pellizca", color = Color.White, fontSize = 10.sp)
                        }
                    }
                } else {
                    IconButton(
                        onClick = { localState.reset() },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Filled.RestartAlt, "Restaurar", tint = Color.White)
                    }
                }
            }
        }
        // ── FIN CONTROLES EDITOR ─────────────────────────────────────────

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

                // FlowRow: cuando hay 5+ stats salta a la siguiente línea automáticamente.
                // Antes una sola Row apretaba "kcal" en columna vertical (k/c/a/l).
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
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

/**
 * Pildora seleccionable arriba-izquierda del editor para elegir qué objeto
 * estás moviendo: foto o ruta. Solo visible mientras `editing=true`.
 */
@Composable
private fun EditorPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .clickable(onClick = onClick)
            .background(
                if (selected) Color(0xFFFF5722) else Color.Black.copy(alpha = 0.55f),
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
