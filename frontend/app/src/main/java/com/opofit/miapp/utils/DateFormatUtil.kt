package com.opofit.miapp.utils

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateFormatUtil {
    private val zona = ZoneId.of("Europe/Madrid")
    // Locale(es, ES) está deprecado en Java 21+: usamos forLanguageTag.
    private val localeEs = Locale.forLanguageTag("es-ES")
    private val fechaHora =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", localeEs)
    private val soloFecha =
        DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", localeEs)

    fun formatearFechaHora(isoOrSql: String?): String {
        if (isoOrSql.isNullOrBlank()) return "—"
        return try {
            val limpio = isoOrSql.trim().replace(" ", "T")
            val instant = when {
                limpio.endsWith("Z") -> Instant.parse(limpio)
                limpio.contains("T") -> LocalDateTime.parse(limpio.substringBefore("."))
                    .atZone(zona).toInstant()
                else -> LocalDateTime.parse(limpio.replace(" ", "T").substringBefore("."))
                    .atZone(zona).toInstant()
            }
            fechaHora.format(instant.atZone(zona))
        } catch (_: Exception) {
            try {
                val d = isoOrSql.take(10)
                soloFecha.format(
                    LocalDateTime.parse("${d}T12:00").atZone(zona)
                )
            } catch (_: Exception) {
                isoOrSql.take(16)
            }
        }
    }

    fun formatearSoloFecha(isoOrSql: String?): String {
        if (isoOrSql.isNullOrBlank()) return "—"
        return try {
            val d = isoOrSql.trim().take(10)
            soloFecha.format(LocalDateTime.parse("${d}T12:00").atZone(zona))
        } catch (_: Exception) {
            isoOrSql.take(10)
        }
    }
}
