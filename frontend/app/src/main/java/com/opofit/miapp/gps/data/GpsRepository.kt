package com.opofit.miapp.gps.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.opofit.miapp.gps.model.ActivitySummary
import java.io.File

/**
 * File-backed repository for GPS activities. Stores one JSON per activity
 * plus a simple in-memory cache. Sufficient for an MVP and easy to migrate
 * to Room later if needed.
 */
class GpsRepository private constructor(context: Context) {

    private val gson = Gson()
    private val baseDir: File = File(context.filesDir, "gps_activities").apply { mkdirs() }

    fun save(summary: ActivitySummary) {
        val file = File(baseDir, "${summary.id}.json")
        file.writeText(gson.toJson(summary))
    }

    fun listAll(): List<ActivitySummary> {
        val files = baseDir.listFiles { f -> f.isFile && f.name.endsWith(".json") } ?: return emptyList()
        return files.sortedByDescending { it.lastModified() }
            .mapNotNull { runCatching { gson.fromJson(it.readText(), ActivitySummary::class.java) }.getOrNull() }
    }

    fun get(id: String): ActivitySummary? {
        val file = File(baseDir, "$id.json")
        if (!file.exists()) return null
        return runCatching { gson.fromJson(file.readText(), ActivitySummary::class.java) }.getOrNull()
    }

    fun delete(id: String): Boolean {
        return File(baseDir, "$id.json").let { if (it.exists()) it.delete() else false }
    }

    fun markSynced(id: String, remoteId: Int) {
        val existing = get(id) ?: return
        save(existing.copy(syncedRemoteId = remoteId))
    }

    /**
     * ¿Existe ya una actividad "equivalente" a esta aunque tenga otro id?
     *
     * El mismo entreno del reloj puede llegar por VARIAS fuentes a la vez
     * (Health Connect, Google Fit, TCX manual…), cada una con su propio id
     * (`hc_…`, `gf_…`, `tcx_…`). Sin este check el usuario veía la misma
     * carrera duplicada/triplicada tras cada sync.
     *
     * Firma de equivalencia: mismo tipo + inicio con ±3 min de diferencia +
     * duración con ±10% + distancia con ±5% (o ambas ~0 para entrenos sin GPS).
     */
    fun existsSimilar(candidate: ActivitySummary): Boolean {
        return listAll().any { a ->
            if (a.id == candidate.id) return@any true
            if (a.type != candidate.type) return@any false
            val dtInicio = kotlin.math.abs(a.startedAtMs - candidate.startedAtMs)
            if (dtInicio > 3 * 60_000L) return@any false
            val dDur = kotlin.math.abs(a.durationSec - candidate.durationSec)
            val durOk = dDur <= (candidate.durationSec * 0.10).coerceAtLeast(30.0)
            val dDist = kotlin.math.abs(a.distanceM - candidate.distanceM)
            val distOk = if (candidate.distanceM < 50.0 && a.distanceM < 50.0) true
            else dDist <= (candidate.distanceM * 0.05).coerceAtLeast(50.0)
            durOk && distOk
        }
    }

    /**
     * Limpieza one-shot de duplicados ya guardados: agrupa por firma
     * (tipo + inicio redondeado a 3 min + distancia redondeada a 100 m) y
     * conserva solo una copia por grupo, priorizando la que ya esté
     * sincronizada con el backend o la fuente más rica (gps propio > hc > gf).
     * Devuelve cuántos archivos eliminó.
     */
    fun dedupAll(): Int {
        val todas = listAll()
        val porFirma = todas.groupBy { a ->
            val inicioRedondeado = a.startedAtMs / (3 * 60_000L)
            val distRedondeada = (a.distanceM / 100.0).toInt()
            "${a.type}|$inicioRedondeado|$distRedondeada"
        }
        var eliminadas = 0
        for ((_, grupo) in porFirma) {
            if (grupo.size <= 1) continue
            // Prioridad de conservación: sincronizada > origen propio > hc > gf > resto
            val ordenadas = grupo.sortedWith(
                compareByDescending<ActivitySummary> { it.syncedRemoteId != null }
                    .thenByDescending { !it.id.startsWith("hc_") && !it.id.startsWith("gf_") }
                    .thenByDescending { it.id.startsWith("hc_") }
            )
            ordenadas.drop(1).forEach { dup ->
                if (delete(dup.id)) eliminadas += 1
            }
        }
        return eliminadas
    }

    companion object {
        @Volatile
        private var INSTANCE: GpsRepository? = null

        fun get(context: Context): GpsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GpsRepository(context.applicationContext).also { INSTANCE = it }
            }
        }

        // For tests / debug; reference kept so unused import warnings stay away.
        @Suppress("UNUSED")
        internal val TYPE_TOKEN: TypeToken<List<ActivitySummary>> = object : TypeToken<List<ActivitySummary>>() {}
    }
}
