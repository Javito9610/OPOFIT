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

    primaryContainer = Color(0xFFDBE4F2),

    onPrimaryContainer = Color(0xFF0F1A2E),



    secondary = SecondaryLight,

    onSecondary = OnSecondaryLight,

    secondaryContainer = Color(0xFFDCE8FF),

    onSecondaryContainer = Color(0xFF1E3A8A),



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

    surfaceVariant = Color(0xFFDCE3EB),

    onSurfaceVariant = OnSurfaceVariantLight,



    outline = OutlineLight,

    outlineVariant = Color(0xFFA8B4C0)

)



private val DarkColorScheme = darkColorScheme(

    primary = PrimaryDark,

    onPrimary = OnPrimaryDark,

    primaryContainer = Color(0xFF243B5C),

    onPrimaryContainer = Color(0xFFD6E4F0),



    secondary = SecondaryDark,

    onSecondary = OnSecondaryDark,

    secondaryContainer = Color(0xFF1E3A5F),

    onSecondaryContainer = Color(0xFFCFDEE8),



    tertiary = Color(0xFF5EC8E0),

    onTertiary = Color(0xFF0A3D4A),

    tertiaryContainer = Color(0xFF0E4A5A),

    onTertiaryContainer = Color(0xFFB8E0EA),



    error = ErrorDark,

    onError = Color(0xFF4A0A0A),

    errorContainer = Color(0xFF6B1A1A),

    onErrorContainer = ErrorDark,



    background = BackgroundDark,

    onBackground = OnBackgroundDark,



    surface = SurfaceDark,

    onSurface = OnSurfaceDark,

    surfaceVariant = Color(0xFF252E38),

    onSurfaceVariant = OnSurfaceVariantDark,



    outline = OutlineDark,

    outlineVariant = Color(0xFF4A5866)

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

        medium = RoundedCornerShape(16.dp),

        large = RoundedCornerShape(20.dp),

        extraLarge = RoundedCornerShape(24.dp)

    )



    MaterialTheme(

        colorScheme = colorScheme,

        typography = AppTypography,

        shapes = shapes,

        content = content

    )

}

