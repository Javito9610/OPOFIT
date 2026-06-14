package com.opofit.miapp.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opofit.miapp.ui.theme.OpoElevation
import com.opofit.miapp.ui.theme.OpoRadii
import com.opofit.miapp.ui.theme.OpoSizes
import com.opofit.miapp.ui.theme.OpoSpacing

/**
 * Familia de cards profesionales OpoFit.
 *
 * - ProCard: card estándar con borde sutil + padding 16. Equivalente al patrón
 *   Caliber/Hevy.
 * - EmphasizedCard: card destacada con elevación + gradiente sutil para el
 *   item PRINCIPAL de la pantalla (la sesión de hoy en Home, p.ej.).
 * - LockedCard: card bloqueada premium con candado + tinte gris.
 * - SectionHeader: header reutilizable para abrir cualquier card pro.
 */

@Composable
fun ProCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: androidx.compose.ui.unit.Dp = OpoSpacing.lg,
    border: Boolean = true,
    content: @Composable () -> Unit
) {
    // Animación de press: la card "se hunde" 3% al tocarla y vuelve con
    // spring suave. Estándar Caliber/Future/Hevy. Antes era un click plano
    // sin feedback visual — sensación de "no respondió".
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "card-press"
    )

    val base = modifier
        .fillMaxWidth()
        .graphicsLayer { scaleX = scale; scaleY = scale }
    val clickable = if (onClick != null) {
        // null indication → usa el ripple default del tema (material3).
        base.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    } else base

    val borderStroke = if (border) BorderStroke(
        1.dp,
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    ) else null

    OutlinedCard(
        modifier = clickable,
        shape = OpoRadii.lg,
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = borderStroke ?: BorderStroke(0.dp, Color.Transparent),
        elevation = CardDefaults.outlinedCardElevation(defaultElevation = OpoElevation.l1)
    ) {
        Column(Modifier.padding(contentPadding), content = { content() })
    }
}

@Composable
fun EmphasizedCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    gradientColors: List<Color>? = null,
    contentPadding: androidx.compose.ui.unit.Dp = OpoSpacing.lg,
    content: @Composable () -> Unit
) {
    val base = modifier.fillMaxWidth()
    val clickable = if (onClick != null) base.clickable { onClick() } else base
    val colors = gradientColors ?: listOf(
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
    )
    ElevatedCard(
        modifier = clickable,
        shape = OpoRadii.lg,
        colors = CardDefaults.elevatedCardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = OpoElevation.l2)
    ) {
        Box(
            Modifier
                .background(Brush.linearGradient(colors))
                .fillMaxWidth()
        ) {
            Column(Modifier.padding(contentPadding), content = { content() })
        }
    }
}

@Composable
fun LockedCard(
    title: String,
    subtitle: String? = null,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    ctaText: String = "Desbloquear"
) {
    ProCard(modifier = modifier, onClick = onUnlock, border = true) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OpoSpacing.md)) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ) {
                Icon(
                    icon ?: Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(10.dp).size(OpoSizes.iconLg)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                subtitle?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                ctaText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Header reutilizable: icono + título + (subtítulo) + (acción a la derecha).
 * Estilo Caliber: pequeño icono coloreado dentro de surface, título semibold,
 * subtítulo opcional en labelSmall.
 */
@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color? = null,
    trailing: @Composable (RowScope.() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OpoSpacing.sm)
    ) {
        icon?.let {
            Surface(
                shape = OpoRadii.sm,
                color = (iconTint ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.12f)
            ) {
                Icon(
                    it,
                    contentDescription = null,
                    tint = iconTint ?: MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(6.dp).size(OpoSizes.iconMd)
                )
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            subtitle?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(OpoSpacing.xs))
            trailing()
        }
    }
}
