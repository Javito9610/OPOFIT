package com.opofit.miapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Dark NEON 2026 — full black + lime + cyan + amber. Inspiración Strava 2025.
private val DarkProColorScheme = darkColorScheme(
    primary             = AccentLime,
    onPrimary           = Color(0xFF000000),  // negro sobre lime (lime es muy claro)
    primaryContainer    = AccentLimeContainer,
    onPrimaryContainer  = AccentLimeSoft,

    secondary           = AccentCyan,
    onSecondary         = Color(0xFF000000),
    secondaryContainer  = Color(0xFF003640),
    onSecondaryContainer = Color(0xFFA5F3FC),

    tertiary            = AccentAmber,
    onTertiary          = Color(0xFF000000),
    tertiaryContainer   = Color(0xFF3D2A00),
    onTertiaryContainer = Color(0xFFFFE082),

    error               = SemanticError,
    onError             = Color(0xFF0D1117),
    errorContainer      = Color(0xFF4A0E0A),
    onErrorContainer    = Color(0xFFFFB4AC),

    background          = BgPrimary,
    onBackground        = TextPrimary,

    surface             = BgSecondary,
    onSurface           = TextPrimary,
    surfaceVariant      = BgTertiary,
    onSurfaceVariant    = TextSecondary,

    outline             = BorderSubtle,
    outlineVariant      = Color(0xFF21262D),

    inverseSurface      = Color(0xFFF0F6FC),
    inverseOnSurface    = BgPrimary,
    inversePrimary      = AccentOrangeDim,

    scrim               = Color(0xCC000000)
)

// Light scheme conservado para no romper preview tools, pero no se usa en runtime.
private val LightColorScheme = lightColorScheme(
    primary             = PrimaryLight,
    onPrimary           = OnPrimaryLight,
    primaryContainer    = Color(0xFFD3E0F5),
    onPrimaryContainer  = Color(0xFF0A1428),
    secondary           = SecondaryLight,
    onSecondary         = OnSecondaryLight,
    secondaryContainer  = Color(0xFFDBE8FF),
    onSecondaryContainer = Color(0xFF0B2A6B),
    tertiary            = AccentOrange,
    onTertiary          = Color(0xFFFFFFFF),
    tertiaryContainer   = AccentOrangeContainer,
    onTertiaryContainer = AccentOrangeDim,
    error               = ErrorLight,
    onError             = Color(0xFFFFFFFF),
    errorContainer      = Color(0xFFFEE2E2),
    onErrorContainer    = ErrorLight,
    background          = BackgroundLight,
    onBackground        = OnBackgroundLight,
    surface             = SurfaceLight,
    onSurface           = OnSurfaceLight,
    surfaceVariant      = Color(0xFFE2E8F0),
    onSurfaceVariant    = OnSurfaceVariantLight,
    outline             = OutlineLight,
    outlineVariant      = Color(0xFF94A3B8)
)

@Composable
fun MiAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Dark Pro: siempre oscuro — ignoramos la preferencia del sistema
    val colorScheme = DarkProColorScheme

    val shapes = Shapes(
        extraSmall = RoundedCornerShape(6.dp),
        small      = RoundedCornerShape(10.dp),
        medium     = RoundedCornerShape(14.dp),
        large      = RoundedCornerShape(18.dp),
        extraLarge = RoundedCornerShape(24.dp)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = AppTypography,
        shapes      = shapes,
        content     = content
    )
}
