package com.opofit.miapp.utils

import kotlin.math.roundToInt

object TimeFormatUtil {
    /** mm:ss.SS — centésimas visibles (precisión típica en marcas). */
    fun formatElapsedMs(ms: Long, showMs: Boolean = true): String {
        val total = ms.coerceAtLeast(0L)
        val h = total / 3_600_000
        val m = (total % 3_600_000) / 60_000
        val s = (total % 60_000) / 1000
        val cs = ((total % 1000) / 10).toInt()
        return if (h > 0) {
            if (showMs) "%d:%02d:%02d.%02d".format(h, m, s, cs)
            else "%d:%02d:%02d".format(h, m, s)
        } else {
            if (showMs) "%02d:%02d.%02d".format(m, s, cs)
            else "%02d:%02d".format(m, s)
        }
    }

    /** Segundos decimales → texto legible (marcas, resultados). */
    fun formatSecondsValue(sec: Double, showMs: Boolean = true): String {
        if (!sec.isFinite() || sec < 0) return "-"
        if (showMs) return String.format("%.3f s", sec)
        return String.format("%.1f s", sec)
    }

    fun secondsFromMs(ms: Long): Double = ms / 1000.0

    fun msFromSeconds(sec: Double): Long = (sec * 1000.0).roundToInt().toLong()

    /**
     * Formato volumen acumulado (cards "Minutos entrenando", "Tu semana", etc.).
     *
     * Convierte una cantidad de minutos a la unidad más legible:
     *   - < 60        → "45 min"
     *   - < 1440 (24h)→ "5h 30 min"  (omite "0 min" si está justo)
     *   - < 10080 (1sem) → "2d 4h"
     *   - < 43200 (~30d) → "3 semanas"
     *   - ≥ 1 mes     → "5 meses" / "1 año 2 meses"
     *
     * Sin esto, las cards de la home muestran "300 min" / "1850 min" en cuanto
     * el usuario entrena varias semanas y se vuelven inmanejables.
     */
    fun formatDuracionLegible(totalMinutos: Int): String {
        val mins = totalMinutos.coerceAtLeast(0)
        if (mins == 0) return "0 min"
        if (mins < 60) return "$mins min"
        val horas = mins / 60
        val resto = mins % 60
        if (mins < 1440) {
            // < 24 h → "Xh Ymin" (omitimos los minutos si son 0)
            return if (resto == 0) "${horas}h" else "${horas}h ${resto} min"
        }
        if (mins < 10_080) {
            // < 1 semana → "Xd Yh"
            val dias = mins / 1440
            val h2 = (mins % 1440) / 60
            return if (h2 == 0) "${dias}d" else "${dias}d ${h2}h"
        }
        if (mins < 43_200) {
            // < 1 mes (~30 días) → "X semanas"
            val semanas = mins / 10_080
            return if (semanas == 1) "1 semana" else "$semanas semanas"
        }
        if (mins < 525_600) {
            // < 1 año → "X meses"
            val meses = mins / 43_200
            return if (meses == 1) "1 mes" else "$meses meses"
        }
        // ≥ 1 año → "1 año" o "2 años 3 meses"
        val anos = mins / 525_600
        val mesesResto = (mins % 525_600) / 43_200
        return when {
            anos == 1 && mesesResto == 0 -> "1 año"
            anos == 1 -> "1 año $mesesResto m"
            mesesResto == 0 -> "$anos años"
            else -> "$anos años $mesesResto m"
        }
    }
}
