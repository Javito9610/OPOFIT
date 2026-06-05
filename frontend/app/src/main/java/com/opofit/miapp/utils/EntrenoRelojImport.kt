package com.opofit.miapp.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.opofit.miapp.gps.data.GpsRepository
import com.opofit.miapp.gps.model.ActivitySummary
import com.opofit.miapp.gps.util.GpxImport
import com.opofit.miapp.gps.util.TcxImport
import com.opofit.miapp.integraciones.EntrenoSyncService
import java.util.Locale

/**
 * Importa el entrenamiento **realizado** en el reloj y rellena valores en la sesión de OpoFit.
 */
object EntrenoRelojImport {

    data class Slot(
        val index: Int,
        val nombre: String,
        val tipo: String?,
        val pilar: String?,
        val objetivoSegundos: Int?,
        val hecho: Boolean,
        val valor: String,
        val distancia: String
    )

    data class Update(
        val index: Int,
        val valor: String? = null,
        val distancia: String? = null,
        val hecho: Boolean? = null
    )

    data class Result(
        val activityId: String,
        val elapsedMs: Long,
        val updates: List<Update>,
        val message: String
    )

    suspend fun syncFromWatch(context: Context, token: String): String {
        return EntrenoSyncService.syncDesdeRelojYCloud(context, token).mensaje()
    }

    fun findRecentActivity(context: Context, sinceMs: Long? = null): ActivitySummary? {
        val repo = GpsRepository.get(context)
        val cutoff = sinceMs ?: (System.currentTimeMillis() - 6 * 3_600_000L)
        return repo.listAll()
            .filter { it.endedAtMs >= cutoff || it.startedAtMs >= cutoff }
            .maxByOrNull { it.endedAtMs }
    }

    suspend fun importFromUri(context: Context, uri: Uri): ActivitySummary? {
        val name = runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
            }
        }.getOrNull().orEmpty().lowercase()

        val summary = context.contentResolver.openInputStream(uri)?.use { stream ->
            when {
                name.endsWith(".tcx") -> TcxImport.parse(stream).getOrThrow()
                else -> {
                    val bytes = stream.readBytes()
                    runCatching { GpxImport.parse(bytes.inputStream()).getOrThrow() }
                        .getOrElse { TcxImport.parse(bytes.inputStream()).getOrThrow() }
                }
            }
        } ?: return null

        val repo = GpsRepository.get(context)
        if (repo.get(summary.id) == null) {
            repo.save(summary)
        }
        return summary
    }

    fun apply(
        activity: ActivitySummary,
        slots: List<Slot>,
        unidadFor: (Int) -> String,
        esGps: (Slot) -> Boolean,
        unitDist: String
    ): Result {
        val pending = slots.filter { !it.hecho }
        if (pending.isEmpty()) {
            return Result(
                activityId = activity.id,
                elapsedMs = activity.durationSec * 1000L,
                updates = emptyList(),
                message = "Todos los ejercicios ya estaban completados."
            )
        }

        val updates = mutableListOf<Update>()
        val laps = activity.laps.filter { it.durationSec > 0 || it.distanceM > 0 }

        if (laps.size >= pending.size && laps.size >= 2) {
            pending.forEachIndexed { lapIdx, slot ->
                val lap = laps[lapIdx]
                updates += buildUpdate(slot, lap, unidadFor(slot.index), esGps(slot), unitDist, markDone = true)
            }
        } else {
            val cardioSlot = pending.firstOrNull { esGps(it) }
            if (cardioSlot != null && activity.distanceM >= 25.0) {
                val lap = laps.firstOrNull { it.distanceM >= 25.0 }
                    ?: com.opofit.miapp.gps.model.ActivityLap(1, activity.durationSec, activity.distanceM)
                updates += buildUpdate(cardioSlot, lap, unidadFor(cardioSlot.index), true, unitDist, markDone = true)
            }

            val timedPending = pending.filter {
                it.tipo != null || unidadFor(it.index) in setOf("min", "s")
            }
            if (timedPending.size == 1 && updates.isEmpty()) {
                val slot = timedPending.first()
                val lap = com.opofit.miapp.gps.model.ActivityLap(1, activity.durationSec, activity.distanceM)
                updates += buildUpdate(slot, lap, unidadFor(slot.index), esGps(slot), unitDist, markDone = true)
            } else if (timedPending.isNotEmpty() && laps.isEmpty() && activity.durationSec >= 30 && updates.isEmpty()) {
                val share = activity.durationSec / timedPending.size.coerceAtLeast(1)
                timedPending.forEach { slot ->
                    val lap = com.opofit.miapp.gps.model.ActivityLap(slot.index + 1, share, 0.0)
                    updates += buildUpdate(slot, lap, unidadFor(slot.index), esGps(slot), unitDist, markDone = true)
                }
            }
        }

        val filled = updates.count { it.valor != null || it.distancia != null }
        val msg = when {
            filled == 0 -> "Actividad encontrada (${formatActivity(activity)}), pero no pude mapearla a tus ejercicios. Rellena los valores manualmente."
            updates.any { it.hecho == true } ->
                "Importado del reloj: $filled ejercicio(s) con valores reales (${formatActivity(activity)}). Revisa y finaliza."
            else -> "Importado: ${formatActivity(activity)}. Revisa los valores antes de finalizar."
        }

        return Result(
            activityId = activity.id,
            elapsedMs = activity.durationSec * 1000L,
            updates = updates,
            message = msg
        )
    }

    private fun buildUpdate(
        slot: Slot,
        lap: com.opofit.miapp.gps.model.ActivityLap,
        unidad: String,
        esGps: Boolean,
        unitDist: String,
        markDone: Boolean
    ): Update {
        val u = unidad.lowercase()
        if (u in setOf("reps", "rep") && !esGps) {
            return Update(index = slot.index, hecho = if (markDone) true else null)
        }
        return when {
            esGps || slot.tipo == "RUN" -> {
                val km = lap.distanceM / 1000.0
                val dist = when {
                    slot.tipo == "SWIM" -> String.format(Locale.US, "%.0f", lap.distanceM)
                    unitDist == "mi" -> String.format(Locale.US, "%.2f", km / 1.609344)
                    else -> String.format(Locale.US, "%.2f", km)
                }
                Update(
                    index = slot.index,
                    valor = String.format(Locale.US, "%.3f", km),
                    distancia = dist,
                    hecho = if (markDone && km > 0) true else null
                )
            }
            unidad == "min" -> Update(
                index = slot.index,
                valor = String.format(Locale.US, "%.2f", lap.durationSec / 60.0),
                hecho = if (markDone) true else null
            )
            unidad == "s" -> Update(
                index = slot.index,
                valor = lap.durationSec.toString(),
                hecho = if (markDone) true else null
            )
            lap.distanceM >= 100 -> Update(
                index = slot.index,
                valor = String.format(Locale.US, "%.3f", lap.distanceM / 1000.0),
                hecho = if (markDone) true else null
            )
            lap.durationSec >= 20 -> Update(
                index = slot.index,
                valor = String.format(Locale.US, "%.2f", lap.durationSec / 60.0),
                hecho = if (markDone) true else null
            )
            else -> Update(index = slot.index)
        }
    }

    private fun formatActivity(a: ActivitySummary): String {
        val dist = if (a.distanceM >= 100) "%.2f km".format(a.distanceM / 1000) else ""
        val dur = TimeFormatUtil.formatElapsedMs(a.durationSec * 1000L)
        val laps = if (a.laps.isNotEmpty()) " · ${a.laps.size} vueltas" else ""
        return listOf(a.type.display, dist, dur).filter { it.isNotBlank() }.joinToString(" · ") + laps
    }
}
