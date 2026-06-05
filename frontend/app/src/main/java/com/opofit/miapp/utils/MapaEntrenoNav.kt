package com.opofit.miapp.utils

object MapaEntrenoNav {
    const val MODO_RUTAS = "rutas"
    const val MODO_LUGARES = "lugares"

    fun rutaMapa(
        distKm: Double? = null,
        modo: String = MODO_RUTAS,
        tipoLugar: String = "GYM",
        variacion: Int = 0
    ): String {
        val km = distKm?.takeIf { it > 0 } ?: 0.0
        return "mapa_entreno?distKm=$km&modo=$modo&tipo=$tipoLugar&variacion=$variacion"
    }

    fun rutaEntrenamiento(enfoque: String, idPlanDia: Int?, idRutinaOpo: Int?): String {
        val e = enfoque.ifBlank { "" }
        return "entrenamientos?enfoque=$e&idPlanDia=${idPlanDia ?: 0}&idRutinaOpo=${idRutinaOpo ?: 0}"
    }

    fun rutaEntrenamientoPers(rutinaId: Int): String = "entrenamiento_pers/$rutinaId"

    fun entornoATipoLugar(entorno: String?): String = when (entorno?.uppercase()) {
        "CROSSFIT" -> "CROSSFIT"
        "CALISTENIA" -> "CALISTENIA"
        "PISTA" -> "PISTA"
        "CASA", "PARQUE" -> "PARQUE"
        else -> "GYM"
    }

    /** Extrae km de textos como "rodaje 8 km", "7,5km", "10 K". */
    fun distanciaKmDesdeTexto(texto: String): Double? {
        val t = texto.lowercase().replace(",", ".")
        Regex("""(\d+(?:\.\d+)?)\s*k(?:m|ilometros|ilómetros)?""").find(t)?.groupValues?.get(1)?.toDoubleOrNull()
            ?.let { return it }
        Regex("""(\d+(?:\.\d+)?)\s*m\b""").find(t)?.groupValues?.get(1)?.toDoubleOrNull()?.let { m ->
            if (m >= 1000) return m / 1000.0
        }
        return null
    }
}
