package com.opofit.miapp.utils

import android.util.Log
import com.opofit.miapp.BuildConfig

/**
 * Logger central para errores no-fatales.
 *
 * Antes el código tenía 22 `catch (_: Exception) { }` silenciosos. En debug
 * eso oculta bugs. En prod no queremos llenar Logcat con stacktraces, pero
 * SÍ queremos un breadcrumb por si Crashlytics se conecta más adelante.
 *
 * SafeLog:
 *  - DEBUG: imprime el stacktrace completo a Logcat.
 *  - RELEASE: registra solo el mensaje (sin trace), no satura Logcat y
 *    deja un breadcrumb para herramientas de monitorización futuras.
 *
 * Uso:
 *   try { ... } catch (e: Exception) { SafeLog.w("HomeViewModel", "fetch feed", e) }
 */
object SafeLog {
    const val TAG_DEFAULT = "OpoFit"

    fun w(tag: String, msg: String, e: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            if (e != null) Log.w(tag, msg, e) else Log.w(tag, msg)
        } else {
            // En release solo el mensaje: no queremos trazas largas en Logcat
            // por defecto, pero sí dejar constancia para herramientas externas.
            Log.w(tag, "$msg${e?.message?.let { " :: $it" } ?: ""}")
        }
    }

    fun e(tag: String, msg: String, e: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            if (e != null) Log.e(tag, msg, e) else Log.e(tag, msg)
        } else {
            Log.e(tag, "$msg${e?.message?.let { " :: $it" } ?: ""}")
        }
    }

    fun d(tag: String, msg: String) {
        if (BuildConfig.DEBUG) Log.d(tag, msg)
    }
}
