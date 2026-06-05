package com.opofit.miapp.utils

object EntrenoValidation {
    fun inferirUnidad(nombre: String, unidadExplicita: String?): String {
        if (!unidadExplicita.isNullOrBlank()) return unidadExplicita.lowercase()
        val n = nombre.lowercase()
        if (Regex("""\bmin\b""").containsMatchIn(n) || Regex("""\d+\s*min""").containsMatchIn(n)) return "min"
        if (n.contains("seg") || n.contains("sprint")) return "s"
        if (n.contains("km")) return "km"
        if (n.contains("natac") || n.contains("metro") || Regex("""\d+\s?m\b""").containsMatchIn(n)) return "m"
        return "reps"
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
