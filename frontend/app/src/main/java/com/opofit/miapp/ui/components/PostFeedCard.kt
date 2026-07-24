package com.opofit.miapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.opofit.miapp.data.responsemodels.ActividadPost
import com.opofit.miapp.gps.util.GpsMetrics
import com.opofit.miapp.utils.DateFormatUtil
import com.opofit.miapp.utils.MediaUrlUtil

/**
 * PostFeedCard — feed estructurado Material 3 Expressive.
 *
 * Antes: avatar pequeño + título + texto + métricas todo en una columna
 * apretada, con interacciones (likes / comentarios) en una sola línea
 * sin jerarquía. La pantalla de Perfil mostraba publicaciones "en línea"
 * sin separación clara.
 *
 * Ahora la estructura es la que verías en Instagram / Strava modernos:
 *   1. HEADER: avatar grande + nombre destacado + fecha + icono de tipo
 *   2. CONTENIDO PRINCIPAL: título h2 + texto opcional
 *   3. STATS pills (Strava-style): cápsulas horizontales scrollables
 *   4. FOTO opcional con border-radius (no a sangre)
 *   5. DIVIDER sutil
 *   6. FOOTER: like + likes + comentarios + visibilidad
 */
@Composable
fun PostFeedCard(
    post: ActividadPost,
    onClick: () -> Unit = {},
    onLike: () -> Unit = {}
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(Modifier.padding(0.dp)) {
            // 1) HEADER
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileAvatar(post.usuarioNombre ?: "?", sizeDp = 44, avatarUrl = post.avatarUrl)
                Column(Modifier.weight(1f)) {
                    Text(
                        post.usuarioNombre ?: "Usuario",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    post.creadoEn?.let {
                        Text(
                            DateFormatUtil.formatearFechaHora(it),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                // Badge tipo actividad — chip suave en lugar de icono suelto.
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Row(
                        Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = EnfoqueIcons.forActividadTipo(post.fuente),
                            contentDescription = post.fuente,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            actividadTipoLabel(post.fuente),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // 2) CONTENIDO PRINCIPAL
            Column(
                Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    post.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                post.texto?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 3) STATS PILLS — cápsulas tipo Strava con etiqueta + valor
            post.stats?.let { s ->
                val stats = buildList {
                    s.distanciaM?.takeIf { it > 0 }?.let { add("Distancia" to GpsMetrics.formatDistance(it)) }
                    s.duracionSec?.takeIf { it > 0 }?.let { add("Tiempo" to GpsMetrics.formatDuration(it)) }
                    s.ritmoMedioSpkm?.takeIf { it > 0 }?.let { add("Ritmo" to "${GpsMetrics.formatPace(it)}/km") }
                    s.desnivelM?.takeIf { it > 0 }?.let { add("Desnivel" to "+${it.toInt()} m") }
                    s.avgHrBpm?.let { add("Pulso" to "$it bpm") }
                    s.kcal?.takeIf { it > 0 }?.let { add("Kcal" to "$it") }
                }
                if (stats.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(stats) { (label, value) ->
                            StatPill(label = label, value = value)
                        }
                    }
                }
            }

            // 4) FOTO opcional
            MediaUrlUtil.resolveAvatar(post.fotoUrl)?.let { url ->
                Spacer(Modifier.height(12.dp))
                AsyncImage(
                    model = url,
                    contentDescription = "Foto del post",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }

            // 5) DIVIDER + FOOTER
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onLike) {
                    Icon(
                        if (post.yoDiLike) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Me gusta",
                        tint = if (post.yoDiLike) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "${post.likes}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(16.dp))
                Icon(
                    Icons.Outlined.ChatBubbleOutline,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "${post.comentarios}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    if (post.visibilidad == "PUBLICO") Icons.Outlined.Public else Icons.Outlined.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (post.visibilidad == "PUBLICO") "Público" else "Solo amigos",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun actividadTipoLabel(fuente: String?): String = when (fuente?.uppercase()) {
    "GPS_ACTIVIDAD", "GPS" -> "Carrera"
    "ENTRENAMIENTO", "SESION" -> "Entreno"
    "MARCA" -> "Marca"
    else -> fuente?.replaceFirstChar { it.uppercase() } ?: "Actividad"
}
