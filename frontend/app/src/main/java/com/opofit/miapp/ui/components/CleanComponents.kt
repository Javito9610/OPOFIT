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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * ===============================================================
 *   CLEAN COMPONENTS — versión PREMIUM con MÁS aire visual.
 *
 *   Filosofía: NO más densidad de texto. Cada componente respira.
 *   El usuario entiende a la primera. Estilo Apple Health / Whoop /
 *   Apple Fitness — espaciado generoso, jerarquía clarísima.
 *
 *   - CleanHero        — hero card simple, 1 título grande + 1 frase clara + CTA
 *   - CleanInfoTile    — tile métrica con label arriba + número enorme + 1 frase
 *   - CleanSection     — header de sección con título + opcional descripción
 *   - CleanListItem    — fila lista con icono + título + descripción + chevron
 *
 *   TODOS usan paddings amplios (20-24dp), tipografía clara, blancos.
 * ===============================================================
 */

// --------------------------------------------------------------
//   CleanHero — hero principal de pantalla
// --------------------------------------------------------------

@Composable
fun CleanHero(
    titulo: String,
    descripcion: String,
    ctaText: String,
    onCta: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
    icono: ImageVector? = null
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 220.dp),
        shape = RoundedCornerShape(24.dp),
        color = cs.surface,
        border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                badge?.let {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = cs.primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            it.uppercase(),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            color = cs.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.5.sp
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    icono?.let {
                        Icon(
                            it,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                    }
                    Text(
                        titulo,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Text(
                    descripcion,
                    style = MaterialTheme.typography.bodyLarge,
                    color = cs.onSurfaceVariant,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 22.sp
                )
            }

            Spacer(Modifier.height(24.dp))

            FreeleticsBigButton(
                text = ctaText,
                onClick = onCta
            )
        }
    }
}

// --------------------------------------------------------------
//   CleanInfoTile — métrica con explicación
// --------------------------------------------------------------

@Composable
fun CleanInfoTile(
    label: String,
    valor: String,
    descripcion: String,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    accent: Color? = null
) {
    val cs = MaterialTheme.colorScheme
    val acc = accent ?: cs.primary
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp),
        shape = RoundedCornerShape(20.dp),
        color = cs.surface,
        border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Label arriba (no agresivo, solo informativo)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                icono?.let {
                    Icon(it, contentDescription = null, tint = acc, modifier = Modifier.size(18.dp))
                }
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
            // Valor ENORME
            Text(
                valor,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            // Frase descriptiva (la clave para "entender a la primera")
            Text(
                descripcion,
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}

// --------------------------------------------------------------
//   CleanSection — header de sección
// --------------------------------------------------------------

@Composable
fun CleanSection(
    titulo: String,
    modifier: Modifier = Modifier,
    descripcion: String? = null
) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            titulo,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        descripcion?.takeIf { it.isNotBlank() }?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
                lineHeight = 20.sp
            )
        }
    }
}

// --------------------------------------------------------------
//   CleanListItem — fila lista respirable
// --------------------------------------------------------------

@Composable
fun CleanListItem(
    titulo: String,
    descripcion: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    rightLabel: String? = null,
    accent: Color? = null
) {
    val cs = MaterialTheme.colorScheme
    val acc = accent ?: cs.primary
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = cs.surface,
        border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            icono?.let {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(acc.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(it, contentDescription = null, tint = acc, modifier = Modifier.size(22.dp))
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    titulo,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                descripcion?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                        lineHeight = 18.sp,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
            rightLabel?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelMedium,
                    color = acc,
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
