package com.opofit.miapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.sp

/**
 * Kit Freeletics-style profesional: 5 composables que conforman el lenguaje
 * visual de la app cuando hay que ir más allá del Material 3 estándar.
 *
 *  - FreeleticsBigButton   — CTA primario gigante con uppercase + letter spacing.
 *  - FreeleticsGhostButton — CTA secundario "fantasma" outlined.
 *  - FreeleticsStatTile    — tile de estadística con valor enorme + label.
 *  - FreeleticsListRow     — fila de lista con leading icon + título + meta + chevron.
 *  - FreeleticsSectionTitle — título de sección estilo "SEMANA · 4/4 SESIONES".
 */

// ---------------------------------------------------------------------------
// Buttons
// ---------------------------------------------------------------------------

@Composable
fun FreeleticsBigButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    color: Color? = null
) {
    val cs = MaterialTheme.colorScheme
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color ?: cs.primary,
            contentColor = Color.White,
            disabledContainerColor = (color ?: cs.primary).copy(alpha = 0.35f),
            disabledContentColor = Color.White.copy(alpha = 0.65f)
        )
    ) {
        leadingIcon?.let {
            Icon(it, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text.uppercase(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp
        )
    }
}

@Composable
fun FreeleticsGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.5.dp,
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    ) {
        leadingIcon?.let {
            Icon(it, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text.uppercase(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
    }
}

// ---------------------------------------------------------------------------
// Stat tile
// ---------------------------------------------------------------------------

@Composable
fun FreeleticsStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accent: Color? = null
) {
    val cs = MaterialTheme.colorScheme
    val acc = accent ?: cs.primary
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 92.dp),
        shape = RoundedCornerShape(20.dp),
        color = cs.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                icon?.let {
                    Icon(it, contentDescription = null, tint = acc, modifier = Modifier.size(14.dp))
                }
                Text(
                    label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp
                )
            }
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Black
            )
        }
    }
}

// ---------------------------------------------------------------------------
// List row
// ---------------------------------------------------------------------------

@Composable
fun FreeleticsListRow(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    rightLabel: String? = null,
    badge: String? = null
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = cs.surface,
        border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            leadingIcon?.let {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(cs.primary.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        it,
                        contentDescription = null,
                        tint = cs.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    badge?.let {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = cs.primary
                        ) {
                            Text(
                                it.uppercase(),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp,
                                letterSpacing = 0.6.sp
                            )
                        }
                    }
                }
                subtitle?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
            rightLabel?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelMedium,
                    color = cs.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = cs.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Section title
// ---------------------------------------------------------------------------

@Composable
fun FreeleticsSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    overline: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    val cs = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        overline?.let {
            Text(
                it.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = cs.primary,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            action?.invoke()
        }
    }
}

// ---------------------------------------------------------------------------
// Goal card (used in onboarding wizard / settings)
// ---------------------------------------------------------------------------

@Composable
fun FreeleticsGoalCard(
    label: String,
    description: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) cs.primary.copy(alpha = 0.15f) else cs.surface,
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) cs.primary else cs.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        if (selected) Brush.linearGradient(
                            listOf(cs.primary, cs.tertiary)
                        ) else Brush.linearGradient(
                            listOf(cs.surfaceVariant, cs.surfaceVariant)
                        ),
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (selected) Color.White else cs.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) cs.primary else cs.onSurfaceVariant,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
