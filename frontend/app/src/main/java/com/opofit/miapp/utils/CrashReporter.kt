package com.opofit.miapp.utils

import android.content.Context

/**
 * Guarda el último crash (excepción no capturada) en SharedPreferences para
 * poder verlo/copiarlo desde Ajustes → Diagnóstico. Así se diagnostica un
 * cierre inesperado sin necesidad de conectar el móvil por USB.
 */
object CrashReporter {
    private const val PREFS = "opofit_crash"
    private const val KEY = "last_crash"

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val ts = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
                    .format(java.util.Date())
                val trace = throwable.stackTraceToString().take(4000)
                app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putString(KEY, "[$ts] hilo=${thread.name}\n$trace")
                    .commit()
            }
            // Encadenamos con el handler por defecto para no cambiar el
            // comportamiento del sistema (sigue mostrando el diálogo de cierre).
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun lastCrash(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply()
    }
}
