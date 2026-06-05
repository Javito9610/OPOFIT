package com.opofit.miapp.utils

import kotlin.math.roundToInt

object PrescripcionFormat {
    fun formatRepeticiones(reps: Double, unidad: String? = null, nombre: String = ""): String {
        val u = unidad?.lowercase().orEmpty()
        val n = nombre.lowercase()
        return when {
            u == "km" || n.contains("km") -> {
                val v = (reps * 10).roundToInt() / 10.0
                if (v % 1.0 == 0.0) v.toLong().toString() else "%.1f".format(v)
            }
            u == "min" || n.contains("min") -> reps.roundToInt().toString()
            u == "s" || n.contains("seg") -> reps.roundToInt().toString()
            reps % 1.0 == 0.0 -> reps.toLong().toString()
            else -> "%.1f".format(reps)
        }
    }
}
