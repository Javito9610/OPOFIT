package com.opofit.miapp.gps.service

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.routeDataStore by preferencesDataStore("route_prefs")

/** Ruta planificada para mostrar en GPS y exportar a reloj. */
data class PlannedRoute(
    val id: String,
    val nombre: String,
    val distanciaKm: Double,
    val puntos: List<RoutePoint> = emptyList(),
    val origen: String = "sugerida"
)

data class RoutePoint(val lat: Double, val lng: Double)

object RoutePreferences {
    private val KEY = stringPreferencesKey("planned_route")
    private val gson = Gson()

    suspend fun save(context: Context, route: PlannedRoute?) {
        context.routeDataStore.edit { prefs ->
            if (route == null) prefs.remove(KEY)
            else prefs[KEY] = gson.toJson(route)
        }
    }

    suspend fun load(context: Context): PlannedRoute? {
        val json = context.routeDataStore.data.map { it[KEY] }.first() ?: return null
        return try {
            gson.fromJson(json, PlannedRoute::class.java)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun clear(context: Context) = save(context, null)
}
