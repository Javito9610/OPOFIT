package com.opofit.miapp.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta OpoFit v2 — navy + acento naranja, con mejores contrastes WCAG AA.

// === Light scheme ===
val PrimaryLight = Color(0xFF132340)        // navy más oscuro: mejor contraste sobre blanco
val OnPrimaryLight = Color(0xFFFFFFFF)

val SecondaryLight = Color(0xFF1D4ED8)      // azul más saturado para botones secundarios
val OnSecondaryLight = Color(0xFFFFFFFF)

val AccentOrange = Color(0xFFF97316)        // naranja vivo (tailwind orange-500)
val AccentOrangeDark = Color(0xFFC2410C)
val AccentOrangeSoft = Color(0xFFFED7AA)
val AccentOrangeContainer = Color(0xFFFFEDD5)

val SuccessLight = Color(0xFF15803D)
val WarningLight = Color(0xFFD97706)
val ErrorLight = Color(0xFFB91C1C)
val InfoLight = Color(0xFF0369A1)

val BackgroundLight = Color(0xFFF1F4F9)     // fondo más limpio (gris muy claro)
val SurfaceLight = Color(0xFFFFFFFF)
val OutlineLight = Color(0xFFCBD5E1)

val OnBackgroundLight = Color(0xFF0F172A)   // slate-900: contraste 16.6:1
val OnSurfaceLight = Color(0xFF0F172A)
val OnSurfaceVariantLight = Color(0xFF334155)  // slate-700: 11.5:1

val AccentTeal = Color(0xFF0D9488)
val AccentSlate = Color(0xFF475569)
val AccentIndigo = Color(0xFF4338CA)

// === Dark scheme ===
val PrimaryDark = Color(0xFFA8C5F0)
val OnPrimaryDark = Color(0xFF0A1428)

val SecondaryDark = Color(0xFF93C5FD)
val OnSecondaryDark = Color(0xFF0A1428)

val SuccessDark = Color(0xFF86EFAC)
val WarningDark = Color(0xFFFBBF24)
val ErrorDark = Color(0xFFFCA5A5)
val InfoDark = Color(0xFF7DD3FC)

val BackgroundDark = Color(0xFF0B1018)       // negro azulado más profundo
val SurfaceDark = Color(0xFF161E2B)
val OutlineDark = Color(0xFF334155)

val OnBackgroundDark = Color(0xFFF1F5F9)
val OnSurfaceDark = Color(0xFFF1F5F9)
val OnSurfaceVariantDark = Color(0xFFCBD5E1)
