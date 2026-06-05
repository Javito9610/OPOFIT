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
}
