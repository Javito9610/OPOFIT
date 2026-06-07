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

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = Color(0xFFD3E0F5),
    onPrimaryContainer = Color(0xFF0A1428),

    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = Color(0xFFDBE8FF),
    onSecondaryContainer = Color(0xFF0B2A6B),

    tertiary = AccentOrange,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = AccentOrangeContainer,
    onTertiaryContainer = AccentOrangeDark,

    error = ErrorLight,
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = ErrorLight,

    background = BackgroundLight,
    onBackground = OnBackgroundLight,

    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = OnSurfaceVariantLight,

    outline = OutlineLight,
    outlineVariant = Color(0xFF94A3B8)
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = Color(0xFF1E3358),
    onPrimaryContainer = Color(0xFFDDEAF7),

    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = Color(0xFF1E3A5F),
    onSecondaryContainer = Color(0xFFCFE0F2),

    tertiary = AccentOrange,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF7C2D12),
    onTertiaryContainer = AccentOrangeSoft,

    error = ErrorDark,
    onError = Color(0xFF4A0A0A),
    errorContainer = Color(0xFF6B1A1A),
    onErrorContainer = ErrorDark,

    background = BackgroundDark,
    onBackground = OnBackgroundDark,

    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = Color(0xFF1F2937),
    onSurfaceVariant = OnSurfaceVariantDark,

    outline = OutlineDark,
    outlineVariant = Color(0xFF475569)
)

@Composable
fun MiAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val shapes = Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(18.dp),
        large = RoundedCornerShape(22.dp),
        extraLarge = RoundedCornerShape(28.dp)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = shapes,
        content = content
    )
}
