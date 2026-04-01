package com.opofit.miapp.utils

import kotlin.math.roundToInt

object Units {
    private const val LB_PER_KG = 2.2046226218
    private const val KM_PER_MI = 1.609344
    private const val YD_PER_M = 1.0936132983

    fun kgToLb(kg: Double): Double = kg * LB_PER_KG
    fun lbToKg(lb: Double): Double = lb / LB_PER_KG

    fun kmToMi(km: Double): Double = km / KM_PER_MI
    fun miToKm(mi: Double): Double = mi * KM_PER_MI

    fun mToYd(m: Double): Double = m * YD_PER_M
    fun ydToM(yd: Double): Double = yd / YD_PER_M

    fun mToMi(m: Double): Double = kmToMi(m / 1000.0)
    fun miToM(mi: Double): Double = miToKm(mi) * 1000.0

    fun kmhToMph(kmh: Double): Double = kmToMi(kmh)
    fun mphToKmh(mph: Double): Double = miToKm(mph)

    fun formatPace(secondsPerUnit: Double): String {
        val paceMin = (secondsPerUnit / 60.0).toInt()
        val paceSec = (secondsPerUnit - paceMin * 60).roundToInt().coerceIn(0, 59)
        return "%d:%02d".format(paceMin, paceSec)
    }

    
    fun nombreConEquivalenciaDistancia(nombre: String, unitDist: String): String {
        if (unitDist != "mi") return nombre
        val regex = Regex("(\\d{1,3}(?:[\\.,]\\d{3})*(?:[\\.,]\\d+)?)\\s*(km|m|metros|metro)\\b", RegexOption.IGNORE_CASE)
        val match = regex.find(nombre) ?: return nombre
        val raw = match.groupValues.getOrNull(1).orEmpty()
        val unit = match.groupValues.getOrNull(2).orEmpty().lowercase()
        val n = raw.replace(".", "").replace(",", ".").toDoubleOrNull() ?: return nombre
        val meters = when (unit) {
            "km" -> n * 1000.0
            else -> n
        }
        val extra = if (meters >= 1000.0) {
            "%.2f mi".format(mToMi(meters))
        } else {
            "%.0f yd".format(mToYd(meters))
        }
        return "$nombre ($extra)"
    }

    /** Muestra marca según ajuste de distancia (km/m → mi/yd). Segundos y reps no se convierten. */
    fun formatMarcaDisplay(valor: Double?, unidadRaw: String?, unitDist: String): String {
        if (valor == null) return "-"
        val u = unidadRaw?.lowercase()?.trim().orEmpty()
        return when {
            u == "s" || u == "seg" || u == "segundos" -> String.format("%.1f s", valor)
            u == "reps" || u.contains("rep") -> String.format("%.1f reps", valor)
            u == "m" || u == "metros" || u == "metro" -> if (unitDist == "mi") {
                if (valor >= 1000) "%.2f mi".format(mToMi(valor)) else "%.0f yd".format(mToYd(valor))
            } else String.format("%.1f m", valor)
            u == "km" -> if (unitDist == "mi") "%.2f mi".format(kmToMi(valor)) else String.format("%.2f km", valor)
            else -> String.format("%.1f %s", valor, unidadRaw ?: "")
        }
    }

    /** Altura: cm o pies/pulgadas si el usuario usa millas. */
    fun formatAltura(cm: Double, unitDist: String): String {
        return if (unitDist == "mi") {
            val totalIn = cm / 2.54
            val ft = (totalIn / 12).toInt().coerceAtLeast(0)
            val inch = (totalIn - ft * 12).roundToInt().coerceIn(0, 11)
            "$ft' $inch\""
        } else {
            "%.0f cm".format(cm)
        }
    }
}

