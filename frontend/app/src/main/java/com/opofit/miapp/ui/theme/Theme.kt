package com.opofit.miapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ============ ESQUEMA DE COLORES - MODO CLARO ============
private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = Color(0xFFE3F2FD),  // Azul muy claro
    onPrimaryContainer = PrimaryLight,

    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = Color(0xFFC5E1FF),
    onSecondaryContainer = SecondaryLight,

    tertiary = InfoLight,
    onTertiary = Color(0xFFFFFFFF),

    error = ErrorLight,
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = ErrorLight,

    background = BackgroundLight,
    onBackground = OnBackgroundLight,

    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = OnSurfaceVariantLight,

    outline = OutlineLight,
    outlineVariant = Color(0xFFC7C7C7)
)

// ============ ESQUEMA DE COLORES - MODO OSCURO ============
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = Color(0xFF1565C0),
    onPrimaryContainer = PrimaryDark,

    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = Color(0xFF0D47A1),
    onSecondaryContainer = SecondaryDark,

    tertiary = InfoDark,
    onTertiary = Color(0xFF0D47A1),

    error = ErrorDark,
    onError = Color(0xFF0D47A1),
    errorContainer = Color(0xFF8B0000),
    onErrorContainer = ErrorDark,

    background = BackgroundDark,
    onBackground = OnBackgroundDark,

    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = OnSurfaceVariantDark,

    outline = OutlineDark,
    outlineVariant = Color(0xFF565656)
)

// ============ TEMA GLOBAL ============
@Composable
fun MiAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),  // Automático según dispositivo
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}