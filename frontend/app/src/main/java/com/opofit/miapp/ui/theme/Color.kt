package com.opofit.miapp.ui.theme

import androidx.compose.ui.graphics.Color

// =====================================================================
//   PALETA OpoFit PRO 2026 — "Performance Blue"
//   Referencias: Garmin Connect, TrainingPeaks, Whoop (superficies),
//   Linear/Stripe (neutros y jerarquía).
//
//   Identidad: grafito azulado profundo + azul rendimiento como único
//   acento de marca + teal (salud/progreso) + ámbar (récords/kcal).
//   Sobria, confiable y premium — pensada para vender suscripción a
//   opositores de Policía, Guardia Civil, Bomberos y Ejército.
//
//   NOTA de compatibilidad: los nombres antiguos (AccentLime*,
//   AccentOrange*, AccentCyan*) se conservan como alias del nuevo
//   sistema para no tocar los ~90 ficheros que los consumen.
// =====================================================================

// === Backgrounds / Surfaces — grafito azulado escalonado ===
// Ya no usamos negro puro: un grafito con ligero tinte azul lee más
// "premium hardware" y las cards ganan profundidad sin bordes duros.
val BgPrimary      = Color(0xFF0A0E14)   // fondo de app
val BgSecondary    = Color(0xFF121722)   // surface de cards
val BgTertiary     = Color(0xFF1A2130)   // surface elevado (sheets, dialogs)
val BgCard         = Color(0xFF161C28)   // card interna

// === ACENTO PRIMARIO — AZUL RENDIMIENTO ===
// #2563EB: azul profesional con contraste AA (5.1:1) para texto blanco
// encima. Para texto/iconos azules sobre fondo oscuro usar Bright.
val AccentBlue          = Color(0xFF2563EB)
val AccentBlueBright    = Color(0xFF6FA5FF)   // texto/iconos acento sobre fondo oscuro
val AccentBlueDim       = Color(0xFF1D4FBC)   // pressed / disabled
val AccentBlueGlow      = Color(0x332563EB)   // capa translúcida para badges
val AccentBlueSoft      = Color(0xFFD6E4FF)   // texto sobre container azul
val AccentBlueContainer = Color(0xFF13253F)   // container secundario azul oscuro

// === ACENTO SECUNDARIO — TEAL (salud / progreso) ===
val AccentCyan          = Color(0xFF14B8A6)
val AccentCyanBright    = Color(0xFF5EEAD4)
val AccentCyanGlow      = Color(0x3314B8A6)

// === ACENTO TERCIARIO — ÁMBAR (récords, calorías, fuego) ===
val AccentAmber         = Color(0xFFF59E0B)
val AccentAmberGlow     = Color(0x33F59E0B)

// === RETROCOMPATIBILIDAD: alias del sistema anterior ===
// Todo lo que antes era lime/naranja apunta ahora al azul de marca.
val AccentLime          = AccentBlue
val AccentLimeBright    = AccentBlueBright
val AccentLimeDim       = AccentBlueDim
val AccentLimeGlow      = AccentBlueGlow
val AccentLimeSoft      = AccentBlueSoft
val AccentLimeContainer = AccentBlueContainer

val AccentOrange          = AccentBlue
val AccentOrangeBright    = AccentBlueBright
val AccentOrangeDim       = AccentBlueDim
val AccentOrangeGlow      = AccentBlueGlow
val AccentOrangeSoft      = AccentBlueSoft
val AccentOrangeContainer = AccentBlueContainer

// === Text ===
val TextPrimary   = Color(0xFFF1F5F9)   // texto principal — blanco suave
val TextSecondary = Color(0xFF97A3B6)   // texto secundario — slate frío
val TextMuted     = Color(0xFF566173)   // texto desactivado / hint
val TextOnAccent  = Color(0xFFFFFFFF)   // texto sobre el azul de marca

// === Semantic ===
val SemanticSuccess = Color(0xFF3FB950)
val SemanticWarning = Color(0xFFE3A008)
val SemanticError   = Color(0xFFF0524A)
val SemanticInfo    = AccentBlueBright

// === Borders / Dividers ===
val BorderSubtle  = Color(0xFF283143)
val BorderDefault = Color(0xFF1E2634)

// === GRADIENTES DE MARCA (hero cards, paywall, momentos de celebración) ===
// Usar con Brush.linearGradient(listOf(BrandGradientStart, BrandGradientMid, BrandGradientEnd)).
val BrandGradientStart = Color(0xFF0E2A5C)   // azul noche profundo
val BrandGradientMid   = Color(0xFF1D4FBC)   // azul de marca oscuro
val BrandGradientEnd   = Color(0xFF2563EB)   // azul de marca
// Dorado premium — exclusivo para la oferta Premium (paywall, badge PRO)
val PremiumGold        = Color(0xFFF3C558)
val PremiumGoldDeep    = Color(0xFFC9992B)

// === Aliases para compatibilidad con los esquemas M3 ===
// Light (no lo usamos pero M3 lo exige)
val PrimaryLight          = Color(0xFF1D4ED8)
val OnPrimaryLight        = Color(0xFFFFFFFF)
val SecondaryLight        = Color(0xFF0F766E)
val OnSecondaryLight      = Color(0xFFFFFFFF)
val BackgroundLight       = Color(0xFFF8FAFC)
val SurfaceLight          = Color(0xFFFFFFFF)
val OutlineLight          = Color(0xFFCBD5E1)
val OnBackgroundLight     = Color(0xFF0F172A)
val OnSurfaceLight        = Color(0xFF0F172A)
val OnSurfaceVariantLight = Color(0xFF475569)
val SuccessLight          = Color(0xFF15803D)
val WarningLight          = Color(0xFFB45309)
val ErrorLight            = Color(0xFFDC2626)
val InfoLight             = Color(0xFF2563EB)

// Dark
val PrimaryDark          = AccentBlue
val OnPrimaryDark        = Color(0xFFFFFFFF)
val SecondaryDark        = AccentCyan
val OnSecondaryDark      = Color(0xFF04231F)
val BackgroundDark       = BgPrimary
val SurfaceDark          = BgSecondary
val OutlineDark          = BorderSubtle
val OnBackgroundDark     = TextPrimary
val OnSurfaceDark        = TextPrimary
val OnSurfaceVariantDark = TextSecondary
val SuccessDark          = SemanticSuccess
val WarningDark          = SemanticWarning
val ErrorDark            = SemanticError
val InfoDark             = SemanticInfo

// Extras usados en componentes existentes
val AccentOrangeDark  = AccentBlueDim
val AccentTeal        = Color(0xFF34D399)   // verde acento para streaks
val AccentSlate       = Color(0xFF97A3B6)
val AccentIndigo      = Color(0xFF6FA5FF)   // azul para datos informativos
