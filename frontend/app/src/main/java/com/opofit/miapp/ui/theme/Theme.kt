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

// Dark PRO 2026 — grafito azulado + azul rendimiento + teal + ámbar.
private val DarkProColorScheme = darkColorScheme(
    primary             = AccentBlue,
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = AccentBlueContainer,
    onPrimaryContainer  = AccentBlueSoft,

    secondary           = AccentCyan,
    onSecondary         = Color(0xFF04231F),
    secondaryContainer  = Color(0xFF0B2E2A),
    onSecondaryContainer = Color(0xFF99F6E4),

    tertiary            = AccentAmber,
    onTertiary          = Color(0xFF231300),
    tertiaryContainer   = Color(0xFF3A2A08),
    onTertiaryContainer = Color(0xFFFDE68A),

    error               = SemanticError,
    onError             = Color(0xFFFFFFFF),
    errorContainer      = Color(0xFF4A100C),
    onErrorContainer    = Color(0xFFFFB4AC),

    background          = BgPrimary,
    onBackground        = TextPrimary,

    surface             = BgSecondary,
    onSurface           = TextPrimary,
    surfaceVariant      = BgTertiary,
    onSurfaceVariant    = TextSecondary,

    outline             = BorderSubtle,
    outlineVariant      = BorderDefault,

    inverseSurface      = Color(0xFFF1F5F9),
    inverseOnSurface    = BgPrimary,
    inversePrimary      = AccentBlueDim,

    scrim               = Color(0xCC000000)
)

// Light scheme conservado para no romper preview tools, pero no se usa en runtime.
private val LightColorScheme = lightColorScheme(
    primary             = PrimaryLight,
    onPrimary           = OnPrimaryLight,
    primaryContainer    = Color(0xFFDBE8FF),
    onPrimaryContainer  = Color(0xFF0B2A6B),
    secondary           = SecondaryLight,
    onSecondary         = OnSecondaryLight,
    secondaryContainer  = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF134E48),
    tertiary            = AccentAmber,
    onTertiary          = Color(0xFF231300),
    tertiaryContainer   = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF78350F),
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
