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
