package com.opofit.miapp.utils

/**
 * Clasificación coherente de ejercicios (cardio vs fuerza, unidad de registro).
 */
object EntrenoExerciseUtil {

    private val UNIDADES_VALIDAS = setOf("reps", "rep", "min", "s", "seg", "km", "m", "amrap")

    private val PATRON_CARDIO_CORRER = Regex(
        """\b(run|running|tempo|jog|jogging|trote|rodaje|fartlek|carrera|trail|maratón|maraton|interval|hiit|z2|continua|continuo|marcha|rucking)\b""",
        RegexOption.IGNORE_CASE
    )
    private val PATRON_NATACION = Regex("""\b(natación|natacion|nadar|swim)\b""", RegexOption.IGNORE_CASE)
    private val PATRON_BICI = Regex("""\b(bici|ciclismo|bicicleta|bike|cycling)\b""", RegexOption.IGNORE_CASE)

    fun esCardio(nombre: String, pilar: String? = null, enfoqueBloque: String? = null): Boolean =
        tipoCardio(nombre, pilar, enfoqueBloque) != null

    fun tipoCardio(nombre: String, pilar: String? = null, enfoqueBloque: String? = null): String? {
        val n = nombre.lowercase()
        if (PATRON_NATACION.containsMatchIn(n)) return "SWIM"
        if (PATRON_BICI.containsMatchIn(n)) return "RUN"
        if (PATRON_CARDIO_CORRER.containsMatchIn(n)) return "RUN"
        val pil = (pilar ?: enfoqueBloque)?.uppercase()
        if (pil == "RESISTENCIA") return "RUN"
        if (pil == "VELOCIDAD" && Regex("""\b(sprint|arranque|velocidad)\b""", RegexOption.IGNORE_CASE).containsMatchIn(n)) {
            return "RUN"
        }
        return null
    }

    fun inferirUnidad(
        nombre: String,
        unidadApi: String? = null,
        pilar: String? = null,
        enfoqueBloque: String? = null
    ): String {
        val desdeNombre = inferirUnidadDesdeNombre(nombre)
        val api = unidadApi?.lowercase()?.trim()?.let { if (it == "seg") "s" else it }
        val pil = (pilar ?: enfoqueBloque)?.uppercase()
        val esCardioEj = esCardio(nombre, pilar, enfoqueBloque)

        if (esCardioEj) {
            return when {
                desdeNombre != "reps" -> desdeNombre
                api in setOf("min", "km", "m", "s") -> api!!
                pil == "VELOCIDAD" && Regex("""\bsprint\b""", RegexOption.IGNORE_CASE).containsMatchIn(nombre) -> "m"
                else -> "min"
            }
        }

        if (!api.isNullOrBlank() && api in UNIDADES_VALIDAS && api != "reps") return api
        if (!api.isNullOrBlank() && api == "reps" && desdeNombre != "reps") return desdeNombre

        if (pil == "RESISTENCIA") return "min"
        if (pil == "VELOCIDAD") {
            return if (Regex("""\bsprint\b""", RegexOption.IGNORE_CASE).containsMatchIn(nombre)) "m" else desdeNombre
        }

        if (!api.isNullOrBlank() && api in UNIDADES_VALIDAS) return api
        return desdeNombre
    }

    private fun inferirUnidadDesdeNombre(nombre: String): String {
        val n = nombre.lowercase()
        if (Regex("""\bmin\b""").containsMatchIn(n) || Regex("""\d+\s*min""").containsMatchIn(n)) return "min"
        if (n.contains("seg") || n.contains("sprint") && Regex("""\d+\s*s\b""").containsMatchIn(n)) return "s"
        if (Regex("""\bkm\b""").containsMatchIn(n) || Regex("""\d+\s*km""").containsMatchIn(n)) return "km"
        if (PATRON_NATACION.containsMatchIn(n)) return "m"
        if (n.contains("metro") || Regex("""\d+\s*m\b""").containsMatchIn(n)) return "m"
        if (PATRON_CARDIO_CORRER.containsMatchIn(n)) return "min"
        return "reps"
    }

    /** Elimina frases duplicadas en instrucciones técnicas (planes cacheados antiguos). */
    fun deduplicarInstrucciones(texto: String?): String? {
        val t = texto?.trim()?.takeIf { it.isNotEmpty() } ?: return texto
        val partes = Regex("(?<=[.!?])\\s+").split(t).map { it.trim() }.filter { it.isNotEmpty() }
        val visto = linkedSetOf<String>()
        val unicos = mutableListOf<String>()
        for (p in partes) {
            val key = p.lowercase().replace(Regex("\\s+"), " ")
            if (visto.add(key)) unicos.add(p)
        }
        return unicos.joinToString(" ")
    }
}
