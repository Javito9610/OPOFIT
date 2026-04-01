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
}

