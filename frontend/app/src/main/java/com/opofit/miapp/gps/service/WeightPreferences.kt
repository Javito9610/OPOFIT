package com.opofit.miapp.gps.service

import android.content.Context

/**
 * Pequeño caché síncrono del peso del usuario para que el servicio de GPS
 * pueda estimar calorías sin tener que abrir DataStore en suspend.
 */
object WeightPreferences {
    private const val FILE = "gps_user_prefs"
    private const val KEY = "weight_kg"

    fun save(context: Context, weightKg: Double?) {
        val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        if (weightKg == null) {
            prefs.edit().remove(KEY).apply()
        } else {
            prefs.edit().putFloat(KEY, weightKg.toFloat()).apply()
        }
    }

    fun get(context: Context): Double? {
        val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY)) return null
        return prefs.getFloat(KEY, 0f).takeIf { it > 0f }?.toDouble()
    }
}
