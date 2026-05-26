package com.opofit.miapp.gps.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.opofit.miapp.data.responsemodels.EjercicioPlan
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Exporta un entrenamiento planificado como TCX 1.0 (`Workouts` schema, Garmin) y
 * también un fichero TXT resumen, ambos compartibles via FileProvider.
 *
 * Compatible (import "workout file" o "training file") con:
 *   - Garmin Connect (Web → Training → Workouts → Import)
 *   - Polar Flow (Training → Diary → Add training → Import)
 *   - Suunto, Coros (cuando aceptan TCX), Zepp Life en última versión.
 *
 * Para relojes que no aceptan TCX Workout directamente, el TXT da los pasos legibles.
 */
object TcxExport {
    private val ISO: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    data class WorkoutStep(
        val nombre: String,
        val tipo: String,
        val durationSec: Int?,
        val distanceM: Int?,
        val repeticiones: Int?,
        val descansoSec: Int
    )

    fun stepsFromEjercicios(ejercicios: List<EjercicioPlan>): List<WorkoutStep> {
        return ejercicios.map { ej ->
            val tipo = detectarTipo(ej.nombre, ej.pilar)
            val unidad = ej.unidad?.lowercase()
            val durSec = when {
                unidad == "min" -> ej.repeticiones * 60
                unidad == "s" || unidad == "seg" -> ej.repeticiones
                else -> null
            }
            val distM = when {
                unidad == "km" -> ej.repeticiones * 1000
                unidad == "m" -> ej.repeticiones
                else -> null
            }
            WorkoutStep(
                nombre = ej.nombre.take(60),
                tipo = tipo,
                durationSec = durSec,
                distanceM = distM,
                repeticiones = if (durSec == null && distM == null) ej.repeticiones else null,
                descansoSec = (ej.descanso).coerceAtLeast(0)
            )
        }
    }

    private fun detectarTipo(nombre: String, pilar: String?): String {
        val n = nombre.lowercase()
        return when {
            n.contains("cinta") || n.contains("tapiz") -> "Treadmill"
            n.contains("carrera") || n.contains("trote") || n.contains("rodaje") || n.contains("fartlek") -> "Run"
            n.contains("natación") || n.contains("natacion") || n.contains("nadar") -> "Swim"
            n.contains("bici") || n.contains("ciclismo") -> "Bike"
            n.contains("caminar") || n.contains("paseo") || n.contains("marcha") -> "Walk"
            pilar == "RESISTENCIA" -> "Cardio"
            pilar == "VELOCIDAD" -> "Speed"
            else -> "Strength"
        }
    }

    fun buildTcx(
        nombreEntreno: String,
        steps: List<WorkoutStep>
    ): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>""").append('\n')
        sb.append(
            "<TrainingCenterDatabase " +
                "xmlns=\"http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
        ).append('\n')
        sb.append("  <Workouts>\n")
        sb.append("    <Workout Sport=\"Running\">\n")
        sb.append("      <Name>${escape(nombreEntreno.take(15))}</Name>\n")
        steps.forEachIndexed { i, s ->
            sb.append("      <Step xsi:type=\"Step_t\">\n")
            sb.append("        <StepId>${i + 1}</StepId>\n")
            sb.append("        <Name>${escape(s.nombre)}</Name>\n")
            val durTag = when {
                s.durationSec != null -> "<Duration xsi:type=\"Time_t\"><Seconds>${s.durationSec}</Seconds></Duration>"
                s.distanceM != null -> "<Duration xsi:type=\"Distance_t\"><Meters>${s.distanceM}</Meters></Duration>"
                s.repeticiones != null -> "<Duration xsi:type=\"Repetitions_t\"><Repetitions>${s.repeticiones}</Repetitions></Duration>"
                else -> "<Duration xsi:type=\"UserAction_t\"><UserAction>LapButton</UserAction></Duration>"
            }
            sb.append("        ").append(durTag).append('\n')
            sb.append("        <Intensity>Active</Intensity>\n")
            sb.append("        <Target xsi:type=\"None_t\" />\n")
            sb.append("      </Step>\n")
            if (s.descansoSec > 0 && i < steps.size - 1) {
                sb.append("      <Step xsi:type=\"Step_t\">\n")
                sb.append("        <StepId>${i + 1}_rest</StepId>\n")
                sb.append("        <Name>Descanso</Name>\n")
                sb.append(
                    "        <Duration xsi:type=\"Time_t\"><Seconds>${s.descansoSec}</Seconds></Duration>\n"
                )
                sb.append("        <Intensity>Resting</Intensity>\n")
                sb.append("        <Target xsi:type=\"None_t\" />\n")
                sb.append("      </Step>\n")
            }
        }
        sb.append("    </Workout>\n")
        sb.append("  </Workouts>\n")
        sb.append("</TrainingCenterDatabase>\n")
        return sb.toString()
    }

    fun buildResumenTxt(nombreEntreno: String, steps: List<WorkoutStep>): String {
        val sb = StringBuilder()
        sb.append("# $nombreEntreno\n")
        sb.append("Generado por OpoFit (${ISO.format(Date())})\n\n")
        steps.forEachIndexed { i, s ->
            sb.append("${i + 1}. ${s.nombre}")
            when {
                s.durationSec != null -> sb.append(" — ${s.durationSec / 60} min ${s.durationSec % 60}s")
                s.distanceM != null -> sb.append(" — ${s.distanceM} m")
                s.repeticiones != null -> sb.append(" — ${s.repeticiones} reps")
            }
            if (s.descansoSec > 0) sb.append("  · descanso ${s.descansoSec}s")
            sb.append('\n')
        }
        return sb.toString()
    }

    fun shareIntent(
        context: Context,
        nombre: String,
        steps: List<WorkoutStep>
    ): Intent? {
        if (steps.isEmpty()) return null
        val dir = File(context.filesDir, "gps_exports").apply { mkdirs() }
        val slug = nombre.replace(Regex("[^A-Za-z0-9]+"), "_").take(40).ifBlank { "entreno" }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val tcxFile = File(dir, "opofit_${slug}_${stamp}.tcx")
        tcxFile.writeText(buildTcx(nombre, steps))
        val txtFile = File(dir, "opofit_${slug}_${stamp}.txt")
        txtFile.writeText(buildResumenTxt(nombre, steps))
        val tcxUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tcxFile)
        val txtUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", txtFile)
        return Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayListOf(tcxUri, txtUri))
            putExtra(Intent.EXTRA_SUBJECT, "Entrenamiento OpoFit: $nombre")
            putExtra(
                Intent.EXTRA_TEXT,
                "Abre el .tcx en la app de tu reloj (Garmin Connect, Polar Flow, Zepp, etc.) para enviar el entrenamiento al reloj."
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun escape(s: String): String =
        s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
}
