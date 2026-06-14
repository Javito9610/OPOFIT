package com.opofit.miapp.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta OpoFit Dark Pro — fondo negro azulado, acento naranja intenso, estilo Strava/TrainingPeaks.

// === Backgrounds / Surfaces ===
val BgPrimary      = Color(0xFF0D1117)   // fondo principal — casi negro
val BgSecondary    = Color(0xFF161B22)   // surface de cards
val BgTertiary     = Color(0xFF1C2128)   // surface elevado (sheets, dialogs)
val BgCard         = Color(0xFF21262D)   // card interna / row destacado

// === Accent naranja ===
val AccentOrange        = Color(0xFFFF6B00)   // naranja primario
val AccentOrangeBright  = Color(0xFFFF8C38)   // hover / estado activo
val AccentOrangeDim     = Color(0xFFCC5500)   // pressed / disabled
val AccentOrangeGlow    = Color(0x33FF6B00)   // capa translucida para fondos de badge/icono
val AccentOrangeSoft    = Color(0xFFFFD0A8)   // texto sobre container naranja oscuro
val AccentOrangeContainer = Color(0xFF3D1A00) // container secundario naranja

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
val PrimaryDark          = AccentOrange
val OnPrimaryDark        = Color(0xFF0D1117)
val SecondaryDark        = AccentOrangeBright
val OnSecondaryDark      = Color(0xFF0D1117)
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
