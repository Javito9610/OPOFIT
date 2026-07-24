package com.opofit.miapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Permite a cualquier composable (logo, splash) saber si el tema activo es
 * oscuro sin pasar flags a mano. Lo provee [MiAppTheme].
 */
val LocalIsDarkTheme = staticCompositionLocalOf { false }

// =====================================================================
//   TEMA CLARO — "Amanecer"  (por defecto)
//   Fondo cálido crema + naranja energía + azul de apoyo. Luminoso y
//   motivador, estilo Nike Training Club / Runna en claro.
// =====================================================================
private val AmanecerLightScheme = lightColorScheme(
    primary              = Color(0xFFF97316),  // naranja energía
    onPrimary            = Color(0xFFFFFFFF),
    primaryContainer     = Color(0xFFFFE7D3),
    onPrimaryContainer   = Color(0xFF7A3200),

    secondary            = Color(0xFF2563EB),  // azul de apoyo
    onSecondary          = Color(0xFFFFFFFF),
    secondaryContainer   = Color(0xFFDCE8FF),
    onSecondaryContainer = Color(0xFF0B2A6B),

    tertiary             = Color(0xFF0F766E),  // teal salud/progreso
    onTertiary           = Color(0xFFFFFFFF),
    tertiaryContainer    = Color(0xFFCCFBF1),
    onTertiaryContainer  = Color(0xFF134E48),

    error                = Color(0xFFDC2626),
    onError              = Color(0xFFFFFFFF),
    errorContainer       = Color(0xFFFEE2E2),
    onErrorContainer     = Color(0xFF7F1D1D),

    background           = Color(0xFFFFF7F0),  // crema cálido
    onBackground         = Color(0xFF1C1917),
    surface              = Color(0xFFFFFFFF),
    onSurface            = Color(0xFF1C1917),
    surfaceVariant       = Color(0xFFF3EAE1),
    onSurfaceVariant     = Color(0xFF78716C),

    outline              = Color(0xFFE3D8CC),
    outlineVariant       = Color(0xFFEFE7DE),

    inverseSurface       = Color(0xFF1C1917),
    inverseOnSurface     = Color(0xFFFAF7F4),
    inversePrimary       = Color(0xFFFFB27A),

    scrim                = Color(0x99000000)
)

// =====================================================================
//   TEMA OSCURO — "Aurora"  (seleccionable en Ajustes)
//   Azul noche + cian eléctrico + lima energía. Oscuro pero VIVO, sin
//   la sensación lúgubre del negro plano.
// =====================================================================
private val AuroraDarkScheme = darkColorScheme(
    primary              = Color(0xFF22D3EE),  // cian eléctrico
    onPrimary            = Color(0xFF04222A),
    primaryContainer     = Color(0xFF0A3A44),
    onPrimaryContainer   = Color(0xFFA5F3FC),

    secondary            = Color(0xFFA3E635),  // lima energía
    onSecondary          = Color(0xFF1A2E05),
    secondaryContainer   = Color(0xFF24310A),
    onSecondaryContainer = Color(0xFFD9F99D),

    tertiary             = Color(0xFFF59E0B),  // ámbar récords/kcal
    onTertiary           = Color(0xFF231300),
    tertiaryContainer    = Color(0xFF3A2A08),
    onTertiaryContainer  = Color(0xFFFDE68A),

    error                = Color(0xFFF0524A),
    onError              = Color(0xFFFFFFFF),
    errorContainer       = Color(0xFF4A100C),
    onErrorContainer     = Color(0xFFFFB4AC),

    background           = Color(0xFF0C1322),  // azul noche
    onBackground         = Color(0xFFE8EEF7),
    surface              = Color(0xFF16203A),
    onSurface            = Color(0xFFE8EEF7),
    surfaceVariant       = Color(0xFF1E2B4A),
    onSurfaceVariant     = Color(0xFF8DA0BC),

    outline              = Color(0xFF24314F),
    outlineVariant       = Color(0xFF1A2540),

    inverseSurface       = Color(0xFFE8EEF7),
    inverseOnSurface     = Color(0xFF0C1322),
    inversePrimary       = Color(0xFF0E7490),

    scrim                = Color(0xCC000000)
)

@Composable
fun MiAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) AuroraDarkScheme else AmanecerLightScheme

    val shapes = Shapes(
        extraSmall = RoundedCornerShape(6.dp),
        small      = RoundedCornerShape(10.dp),
        medium     = RoundedCornerShape(14.dp),
        large      = RoundedCornerShape(18.dp),
        extraLarge = RoundedCornerShape(24.dp)
    )

    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = AppTypography,
            shapes      = shapes,
            content     = content
        )
    }
}
