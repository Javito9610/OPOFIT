package com.opofit.miapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import coil.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.opofit.miapp.ui.theme.AccentOrange
import com.opofit.miapp.ui.utils.isCompactScreen
import com.opofit.miapp.utils.MediaUrlUtil

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(22.dp)
                .background(AccentOrange, MaterialTheme.shapes.extraSmall)
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Estilo estándar de tarjetas OpoFit: blanco, borde visible y sombra sobre el fondo gris. */
object OpoCardDefaults {
    @Composable
    fun colors(containerColor: Color = MaterialTheme.colorScheme.surface) =
        CardDefaults.cardColors(containerColor = containerColor)

    @Composable
    fun elevation() = CardDefaults.cardElevation(
        defaultElevation = 5.dp,
        pressedElevation = 7.dp,
        focusedElevation = 6.dp,
        hoveredElevation = 6.dp
    )

    @Composable
    fun border() = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.58f))
}

/** Alias corto — usar en lugar de `Card` de Material para contraste uniforme. */
@Composable
fun OpoCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) = ElevatedCard(modifier, containerColor, onClick, content)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ElevatedCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val colors = OpoCardDefaults.colors(containerColor)
    val elevation = OpoCardDefaults.elevation()
    val border = OpoCardDefaults.border()
    val shape = MaterialTheme.shapes.large

    if (onClick != null) {
        Card(
            modifier = modifier,
            onClick = onClick,
            shape = shape,
            colors = colors,
            elevation = elevation,
            border = border
        ) {
            content()
        }
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = colors,
            elevation = elevation,
            border = border
        ) {
            content()
        }
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    icon: ImageVector? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null,
    // Si el valor es un entero (sesiones, racha, kcal), pasa este Int extra
    // y la card animará el cambio con tween + flash de color. Si null, usa
    // el String value normal.
    animatedIntValue: Int? = null
) {
    val compact = isCompactScreen()
    val pad = if (compact) 10.dp else 12.dp
    ElevatedCard(modifier = modifier, onClick = onClick) {
        Column(
            modifier = Modifier.padding(pad),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (icon != null) {
                    Surface(
                        shape = CircleShape,
                        color = accentColor.copy(alpha = 0.12f),
                        modifier = Modifier.size(if (compact) 26.dp else 30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(if (compact) 14.dp else 16.dp),
                                tint = accentColor
                            )
                        }
                    }
                }
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (animatedIntValue != null) {
                AnimatedNumber(
                    value = animatedIntValue,
                    fontSize = if (compact) 22f else 28f,
                    accent = MaterialTheme.colorScheme.onSurface
                )
            } else Text(
                text = value,
                style = if (compact) MaterialTheme.typography.titleLarge
                else MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            if (!supporting.isNullOrBlank()) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** CTA principal estilo Nike/Hevy: naranja, altura generosa, icono opcional. */
@Composable
fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.buttonColors(
            containerColor = AccentOrange,
            contentColor = Color.White,
            disabledContainerColor = AccentOrange.copy(alpha = 0.4f),
            disabledContentColor = Color.White.copy(alpha = 0.7f)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        )
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
        }
        Text(text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
fun ProfileAvatar(
    name: String,
    modifier: Modifier = Modifier,
    sizeDp: Int = 56,
    avatarUrl: String? = null
) {
    val initials = name.trim().split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifBlank { "?" }
    val url = MediaUrlUtil.resolveAvatar(avatarUrl)
    Surface(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(CircleShape),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (url != null) {
                AsyncImage(
                    model = url,
                    contentDescription = "Foto de perfil",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Empty state premium con icono Material en un disco con tono de superficie.
 * El parámetro `emoji` queda como compatibilidad con código legacy y se
 * ignora; lo importante ahora es `icon`. Si llega un emoji legacy se mapea
 * heurísticamente a un Material Icon coherente.
 */
@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Filled.Inbox,
    emoji: String = "",
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val effectiveIcon = if (emoji.isNotBlank()) emojiToMaterialIcon(emoji) ?: icon else icon
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = androidx.compose.foundation.shape.CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = effectiveIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/** Mapeo defensivo emoji → Material Icon para callsites legacy. */
private fun emojiToMaterialIcon(emoji: String): androidx.compose.ui.graphics.vector.ImageVector? = when {
    emoji.contains("🏃") || emoji.contains("🏋") -> Icons.Filled.FitnessCenter
    emoji.contains("🎯") -> Icons.Filled.EmojiEvents
    emoji.contains("📊") || emoji.contains("📈") -> Icons.Filled.BarChart
    emoji.contains("📅") -> Icons.Filled.CalendarMonth
    emoji.contains("👥") || emoji.contains("👤") -> Icons.Filled.People
    emoji.contains("💬") -> Icons.AutoMirrored.Filled.Chat
    emoji.contains("⏱") || emoji.contains("⏰") -> Icons.Filled.Timer
    emoji.contains("🗺") || emoji.contains("📍") -> Icons.Filled.LocationOn
    emoji.contains("🔥") || emoji.contains("⚡") -> Icons.Filled.Whatshot
    emoji.contains("🏆") || emoji.contains("⭐") -> Icons.Filled.EmojiEvents
    emoji.contains("❤") -> Icons.Filled.FavoriteBorder
    else -> null
}

@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        OutlinedButton(onClick = onRetry) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Text("Reintentar", modifier = Modifier.padding(start = 8.dp))
        }
    }
}
