package com.opofit.miapp.utils

import com.opofit.miapp.gps.model.ActivityType

object MapaEntrenoNav {
    const val MODO_RUTAS = "rutas"
    const val MODO_LUGARES = "lugares"

    data class LimitesRuta(val minKm: Float, val maxKm: Float, val steps: Int, val etiqueta: String)

    fun actividadDesdeGps(type: ActivityType): String = when (type) {
        ActivityType.BIKE -> "BICI"
        ActivityType.WALK -> "CAMINAR"
        else -> "CARRERA"
    }

    fun tipoDesdeActividad(actividad: String): ActivityType = when (actividad.uppercase()) {
        "BICI", "BIKE" -> ActivityType.BIKE
        "CAMINAR", "WALK" -> ActivityType.WALK
        else -> ActivityType.RUN
    }

    fun limitesRuta(actividad: String, terreno: String): LimitesRuta {
        val act = actividad.uppercase()
        val ter = terreno.uppercase()
        return if (act == "BICI" || act == "BIKE") {
            if (ter == "MONTANA") {
                LimitesRuta(1f, 100f, 33, "Bici montaña")
            } else {
                LimitesRuta(1f, 180f, 35, "Bici carretera")
            }
        } else if (act == "CAMINAR" || act == "WALK") {
            if (ter == "MONTANA") {
                LimitesRuta(1f, 30f, 14, "Caminata montaña")
            } else {
                LimitesRuta(1f, 30f, 14, "Caminata")
            }
        } else if (ter == "MONTANA") {
            LimitesRuta(1f, 42f, 20, "Carrera montaña")
        } else {
            LimitesRuta(1f, 42f, 20, "Carrera ciudad")
        }
    }

    fun rutaMapa(
        distKm: Double? = null,
        modo: String = MODO_RUTAS,
        tipoLugar: String = "GYM",
        variacion: Int = 0,
        actividad: String = "CARRERA",
        terreno: String = "CIUDAD"
    ): String {
        val km = distKm?.takeIf { it > 0 } ?: 0.0
        return "mapa_entreno?distKm=$km&modo=$modo&tipo=$tipoLugar&variacion=$variacion" +
            "&actividad=$actividad&terreno=$terreno"
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
