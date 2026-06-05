package com.opofit.miapp.ui.components

import com.opofit.miapp.ui.components.ElevatedCard
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.opofit.miapp.data.responsemodels.ActividadPost
import com.opofit.miapp.gps.util.GpsMetrics
import com.opofit.miapp.utils.DateFormatUtil
import com.opofit.miapp.utils.MediaUrlUtil

@Composable
fun PostFeedCard(
    post: ActividadPost,
    onClick: () -> Unit = {},
    onLike: () -> Unit = {}
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                ProfileAvatar(post.usuarioNombre ?: "?", sizeDp = 44, avatarUrl = post.avatarUrl)
                Column(Modifier.weight(1f)) {
                    Text(post.usuarioNombre ?: "Usuario", fontWeight = FontWeight.Bold)
                    post.creadoEn?.let {
                        Text(
                            DateFormatUtil.formatearFechaHora(it),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    when (post.fuente) {
                        "GPS" -> "🏃"
                        "ENTRENO" -> "💪"
                        "SIMULACRO" -> "🎯"
                        else -> "📌"
                    },
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            Text(post.titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            post.texto?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            post.stats?.let { s ->
                val parts = buildList {
                    s.distanciaM?.takeIf { it > 0 }?.let { add(GpsMetrics.formatDistance(it)) }
                    s.duracionSec?.takeIf { it > 0 }?.let { add(GpsMetrics.formatDuration(it)) }
                    s.ritmoMedioSpkm?.takeIf { it > 0 }?.let { add("${GpsMetrics.formatPace(it)}/km") }
                    s.desnivelM?.takeIf { it > 0 }?.let { add("+${it.toInt()} m") }
                    s.avgHrBpm?.let { add("♥ $it") }
                    s.kcal?.takeIf { it > 0 }?.let { add("$it kcal") }
                }
                if (parts.isNotEmpty()) {
                    Text(
                        parts.joinToString("  ·  "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            MediaUrlUtil.resolveAvatar(post.fotoUrl)?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = "Foto del post",
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onLike) {
                    Icon(
                        if (post.yoDiLike) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Me gusta",
                        tint = if (post.yoDiLike) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text("${post.likes}", style = MaterialTheme.typography.labelMedium)
                Icon(
                    Icons.Outlined.ChatBubbleOutline,
                    null,
                    Modifier.padding(start = 16.dp).size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    " ${post.comentarios}",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(start = 4.dp)
                )
                Text(
                    if (post.visibilidad == "PUBLICO") "  ·  Público" else "  ·  Solo amigos",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
