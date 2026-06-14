package com.opofit.miapp.ui.components

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
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Hero card del plan de entrenamiento estilo Freeletics / Adidas Training /
 * Nike Training Club.
 *
 * Estructura visual:
 *  ┌────────────────────────────────────────┐
 *  │  ENTRENAMIENTO DE HOY (label pequeño)  │  ← naranja brillante
 *  │  FUERZA · TREN SUPERIOR (titular)      │  ← display large blanco
 *  │  6 ejercicios · 45 min · Nivel 3       │  ← chips meta
 *  │                                        │
 *  │      ┌──────────────────────────┐      │
 *  │      │  ▶  EMPEZAR AHORA  (CTA) │      │  ← botón naranja gigante
 *  │      └──────────────────────────┘      │
 *  └────────────────────────────────────────┘
 *
 * El fondo es un gradiente diagonal oscuro→naranja translúcido como las
 * apps top. La tipografía es gorda y agresiva. Compacta toda la info
 * relevante en una sola card.
 */
@Composable
fun FreeleticsHeroCard(
    label: String,
    titulo: String,
    subtitulo: String?,
    chips: List<String>,
    ctaTexto: String,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
    icono: ImageVector = Icons.Filled.FitnessCenter
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 260.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        shadowElevation = 12.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1A1A1A),
                            Color(0xFF2D1500),
                            cs.primary.copy(alpha = 0.85f)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            // Sello del icono en la esquina superior derecha (transparente).
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.08f),
                modifier = Modifier
                    .size(140.dp)
                    .align(Alignment.TopEnd)
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Label pequeño naranja: "ENTRENAMIENTO DE HOY"
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.primary,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )

                // Titular grande negrita: "FUERZA · TREN SUPERIOR"
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    lineHeight = 32.sp
                )

                subtitulo?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.75f),
                        fontWeight = FontWeight.Medium
                    )
                }

                // Chips meta
                if (chips.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        chips.take(4).forEach { chip ->
                            FreeleticsChip(chip)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // CTA grande naranja
                Button(
                    onClick = onStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = cs.primary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = ctaTexto.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun FreeleticsChip(text: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color.White.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (text.contains("min", ignoreCase = true)) {
                Icon(
                    Icons.Outlined.Timer,
                    null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}
