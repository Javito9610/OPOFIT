package com.opofit.miapp.utils

object EntrenoValidation {
    fun inferirUnidad(
        nombre: String,
        unidadExplicita: String?,
        pilar: String? = null,
        enfoqueBloque: String? = null
    ): String = EntrenoExerciseUtil.inferirUnidad(nombre, unidadExplicita, pilar, enfoqueBloque)

    /** Formatea la unidad para mostrarla al usuario (ej: "s" → "seg", "reps" → "reps"). */
    fun unidadLegible(unidad: String): String = when (unidad.lowercase().trim()) {
        "s", "seg", "segundos" -> "seg"
        "min", "minutos" -> "min"
        "reps", "rep" -> "reps"
        "km" -> "km"
        "m", "metros" -> "m"
        "vueltas", "vuelta" -> "vueltas"
        "rondas", "ronda" -> "rondas"
        else -> unidad
    }

    /**
     * Valida que el valor introducido tenga sentido respecto al tiempo total del entreno.
     * Si el ejercicio mide tiempo (s/min) y el valor supera el cronómetro, no es posible.
     */
    fun validarValorContraTiempoTotal(
        valor: String,
        unidad: String,
        elapsedMs: Long
    ): String? {
        if (elapsedMs <= 0L) return null
        val v = valor.replace(",", ".").toDoubleOrNull() ?: return null
        val elapsedSec = elapsedMs / 1000.0
        val elapsedMin = elapsedSec / 60.0
        return when (unidad.lowercase().trim()) {
            "s", "seg" -> if (v > elapsedSec)
                "Imposible: $valor seg > tiempo de entreno (${elapsedSec.toInt()} seg)"
            else null
            "min" -> if (v > elapsedMin)
                "Imposible: $valor min > tiempo de entreno (%.1f min)".format(elapsedMin)
            else null
            else -> null
        }
    }

    fun validarValor(valor: String, unidad: String): String? {
        if (valor.isBlank()) return "Introduce un valor"
        val v = valor.replace(",", ".").toDoubleOrNull()
            ?: return "Debe ser un número válido"
        if (v <= 0) return "Debe ser mayor que 0"
        return when (unidad.lowercase()) {
            "reps", "rep" -> when {
                v != v.toLong().toDouble() -> "Las repeticiones deben ser enteras"
                v > 10_000 -> "Valor imposible (máx. 10.000 reps)"
                else -> null
            }
            "min" -> when {
                v > 600 -> "Duración imposible (máx. 600 min)"
                else -> null
            }
            "s" -> when {
                v > 7200 -> "Tiempo imposible (máx. 2 h)"
                else -> null
            }
            "km" -> when {
                v > 1000 -> "Distancia imposible (máx. 1000 km)"
                else -> null
            }
            "m" -> when {
                v > 100_000 -> "Distancia imposible (máx. 100 km)"
                else -> null
            }
            else -> if (v > 1_000_000) "Valor imposible" else null
        }
    }
}
