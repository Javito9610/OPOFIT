package com.opofit.miapp.ui.theme

import androidx.compose.ui.graphics.Color

// =====================================================================
//   PALETA OpoFit NEON 2026
//   Inspiración: Strava 2025, Whoop, Apple Fitness+, Centr, Nike Run.
//
//   Decisión: ROMPER con el naranja apagado anterior. Vamos a un look
//   FULL BLACK + LIME (verde Strava 2024) + CYAN (Whoop) + acentos cálidos.
//   Los acentos LIME y CYAN explotan sobre negro puro, dan sensación
//   de "alto rendimiento", deportiva y MUY moderna.
// =====================================================================

// === Backgrounds / Surfaces — negro puro escalonado ===
val BgPrimary      = Color(0xFF000000)   // negro absoluto — feel premium
val BgSecondary    = Color(0xFF0A0A0A)   // surface de cards
val BgTertiary     = Color(0xFF141414)   // surface elevado (sheets, dialogs)
val BgCard         = Color(0xFF1C1C1E)   // card interna (referencia iOS)

// === ACENTO PRIMARIO — LIME ELÉCTRICO (Strava 2024 vibe) ===
val AccentLime          = Color(0xFFC7F73C)   // verde lima saturado — el COLOR
val AccentLimeBright    = Color(0xFFD8FF5C)   // hover / glow
val AccentLimeDim       = Color(0xFF95C515)   // pressed / disabled
val AccentLimeGlow      = Color(0x33C7F73C)   // capa translucida para badges
val AccentLimeSoft      = Color(0xFFE5FFB3)   // texto sobre container lime
val AccentLimeContainer = Color(0xFF1A2900)   // container secundario lime oscuro

// === ACENTO SECUNDARIO — CYAN ELÉCTRICO (Whoop) ===
val AccentCyan          = Color(0xFF22D3EE)   // cyan vibrante datos / info
val AccentCyanBright    = Color(0xFF67E8F9)
val AccentCyanGlow      = Color(0x3322D3EE)

// === ACENTO TERCIARIO — ÁMBAR (calorías, fuego) ===
val AccentAmber         = Color(0xFFFFB300)   // ámbar dorado, PRs y kcal
val AccentAmberGlow     = Color(0x33FFB300)

// === RETROCOMPATIBILIDAD: los componentes existentes usan AccentOrange ===
// Mapean al lime para no romper código antiguo. Si quieres recuperar
// el naranja real, usa AccentAmber.
val AccentOrange          = AccentLime
val AccentOrangeBright    = AccentLimeBright
val AccentOrangeDim       = AccentLimeDim
val AccentOrangeGlow      = AccentLimeGlow
val AccentOrangeSoft      = AccentLimeSoft
val AccentOrangeContainer = AccentLimeContainer

// === Text ===
val TextPrimary   = Color(0xFFF0F6FC)   // texto principal — blanco suave
val TextSecondary = Color(0xFF8B949E)   // texto secundario — gris GitHub
val TextMuted     = Color(0xFF484F58)   // texto desactivado / hint
val TextOnAccent  = Color(0xFFFFFFFF)   // texto sobre naranja

// === Semantic ===
val SemanticSuccess = Color(0xFF3FB950)   // verde GitHub
val SemanticWarning = Color(0xFFD29922)   // amarillo ámbar
val SemanticError   = Color(0xFFF85149)   // rojo GitHub
val SemanticInfo    = Color(0xFF58A6FF)   // azul GitHub

// === Borders / Dividers ===
val BorderSubtle  = Color(0xFF30363D)
val BorderDefault = Color(0xFF21262D)

// === Aliases para compatibilidad con los esquemas M3 ===
// Light (no lo usamos pero M3 lo exige)
val PrimaryLight          = Color(0xFF132340)
val OnPrimaryLight        = Color(0xFFFFFFFF)
val SecondaryLight        = Color(0xFF1D4ED8)
val OnSecondaryLight      = Color(0xFFFFFFFF)
val BackgroundLight       = Color(0xFF0D1117)
val SurfaceLight          = Color(0xFF161B22)
val OutlineLight          = Color(0xFF30363D)
val OnBackgroundLight     = Color(0xFFF0F6FC)
val OnSurfaceLight        = Color(0xFFF0F6FC)
val OnSurfaceVariantLight = Color(0xFF8B949E)
val SuccessLight          = Color(0xFF3FB950)
val WarningLight          = Color(0xFFD29922)
val ErrorLight            = Color(0xFFF85149)
val InfoLight             = Color(0xFF58A6FF)

// Dark
val PrimaryDark          = AccentLime
// Lime es muy claro → texto sobre primary debe ser NEGRO (no oscuro azulado).
val OnPrimaryDark        = Color(0xFF000000)
val SecondaryDark        = AccentCyan
val OnSecondaryDark      = Color(0xFF000000)
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
val AccentOrangeDark  = AccentOrangeDim
val AccentTeal        = Color(0xFF39D353)   // verde acento para streaks
val AccentSlate       = Color(0xFF8B949E)
val AccentIndigo      = Color(0xFF58A6FF)   // azul para datos informativos
