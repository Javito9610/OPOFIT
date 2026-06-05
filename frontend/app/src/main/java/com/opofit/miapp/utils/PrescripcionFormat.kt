package com.opofit.miapp.utils

import kotlin.math.roundToInt

object PrescripcionFormat {
    fun formatRepeticiones(reps: Double, unidad: String? = null, nombre: String = ""): String {
        val u = unidad?.lowercase().orEmpty()
        val n = nombre.lowercase()
        if (u == "max" || u == "amrap" || reps >= 90.0 ||
            n.contains("amrap") || n.contains("máx") || n.contains("max") ||
            n.contains("al fallo") || n.contains("a fallo")
        ) {
            return "máx"
        }
        return when {
            u == "km" || n.contains("km") -> {
                val v = (reps * 10).roundToInt() / 10.0
                if (v % 1.0 == 0.0) v.toLong().toString() else "%.1f".format(v)
            }
            u == "min" || Regex("""\d+\s*min\b""").containsMatchIn(n) || Regex("""\bminutos?\b""").containsMatchIn(n) ->
                reps.roundToInt().toString()
            u == "s" || n.contains("seg") -> reps.roundToInt().toString()
            reps % 1.0 == 0.0 -> reps.toLong().toString()
            else -> "%.1f".format(reps)
        }
    }
}
