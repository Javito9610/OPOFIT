package com.opofit.miapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Tipografía Dark Pro — headlines muy bold y condensadas como Strava/TrainingPeaks.
// Tres pesos: Normal (400), SemiBold (600), ExtraBold (800).
val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily   = FontFamily.Default,
        fontWeight   = FontWeight.ExtraBold,
        fontSize     = 44.sp,
        lineHeight   = 50.sp,
        letterSpacing = (-1.0).sp
    ),
    displayMedium = TextStyle(
        fontFamily   = FontFamily.Default,
        fontWeight   = FontWeight.ExtraBold,
        fontSize     = 36.sp,
        lineHeight   = 42.sp,
        letterSpacing = (-0.75).sp
    ),
    headlineLarge = TextStyle(
        fontFamily   = FontFamily.Default,
        fontWeight   = FontWeight.ExtraBold,
        fontSize     = 30.sp,
        lineHeight   = 36.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily   = FontFamily.Default,
        fontWeight   = FontWeight.ExtraBold,
        fontSize     = 24.sp,
        lineHeight   = 30.sp,
        letterSpacing = (-0.25).sp
    ),
    headlineSmall = TextStyle(
        fontFamily   = FontFamily.Default,
        fontWeight   = FontWeight.Bold,
        fontSize     = 20.sp,
        lineHeight   = 26.sp,
        letterSpacing = 0.sp
    ),

    titleLarge = TextStyle(
        fontFamily   = FontFamily.Default,
        fontWeight   = FontWeight.Bold,
        fontSize     = 18.sp,
        lineHeight   = 24.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily   = FontFamily.Default,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 15.sp,
        lineHeight   = 22.sp,
        letterSpacing = 0.1.sp
    ),
    titleSmall = TextStyle(
        fontFamily   = FontFamily.Default,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 13.sp,
        lineHeight   = 18.sp,
        letterSpacing = 0.1.sp
    ),

    bodyLarge = TextStyle(
        fontFamily   = FontFamily.Default,
        fontWeight   = FontWeight.Normal,
        fontSize     = 15.sp,
        lineHeight   = 23.sp,     // 150% para lectura cómoda
        letterSpacing = 0.15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily   = FontFamily.Default,
        fontWeight   = FontWeight.Normal,
        fontSize     = 13.sp,
        lineHeight   = 20.sp,
        letterSpacing = 0.15.sp
    ),
    bodySmall = TextStyle(
        fontFamily   = FontFamily.Default,
        fontWeight   = FontWeight.Normal,
        fontSize     = 11.sp,
        lineHeight   = 16.sp,
        letterSpacing = 0.2.sp
    ),

    labelLarge = TextStyle(
        fontFamily   = FontFamily.Default,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 13.sp,
        lineHeight   = 18.sp,
        letterSpacing = 0.5.sp    // uppercase labels más legibles
    ),
    labelMedium = TextStyle(
        fontFamily   = FontFamily.Default,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 11.sp,
        lineHeight   = 14.sp,
        letterSpacing = 0.6.sp
    ),
    labelSmall = TextStyle(
        fontFamily   = FontFamily.Default,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 10.sp,
        lineHeight   = 13.sp,
        letterSpacing = 0.7.sp
    )
)
