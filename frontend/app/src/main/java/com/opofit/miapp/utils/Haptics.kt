package com.opofit.miapp.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable

/**
 * Helper de haptics para feedback táctil pro.
 *
 * Apple Watch / Strava / Hevy: cada interacción importante (botón principal,
 * completar serie, batir PR, cambiar fase) tiene su propio tipo de vibración.
 * Reduce la sensación de "pulso al vacío" en pantalla táctil.
 *
 * 3 tipos:
 *  - light  → toque ligero, para feedback de "registrado"
 *  - medium → impacto medio, para CTAs principales
 *  - success → patrón doble para celebración (PR, sesión completada)
 *
 * En Android 12+: usa VibrationEffect.Composition.PRIMITIVE_* si está disponible.
 * Fallback: VibrationEffect.createOneShot.
 */

object Haptics {
    enum class Type { LIGHT, MEDIUM, SUCCESS }

    fun perform(context: Context, type: Type) {
        val vibrator = vibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        when (type) {
            Type.LIGHT -> vibrator.vibrate(VibrationEffect.createOneShot(20, 80))
            Type.MEDIUM -> vibrator.vibrate(VibrationEffect.createOneShot(40, 140))
            Type.SUCCESS -> {
                // Patrón doble: tap-tap fuerte para celebrar.
                val pattern = longArrayOf(0, 60, 50, 100)
                val amplitudes = intArrayOf(0, 200, 0, 255)
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
            }
        }
    }

    private fun vibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val mgr = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            mgr?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}

/**
 * Composable helper: obtiene una lambda { Haptics.Type -> Unit } con context.
 * Uso:
 *   val haptic = rememberHaptics()
 *   PrimaryButton(..., onClick = { haptic(Haptics.Type.MEDIUM); doStuff() })
 */
@Composable
fun rememberHaptics(): (Haptics.Type) -> Unit {
    val context = LocalContext.current
    return { type -> Haptics.perform(context, type) }
}
