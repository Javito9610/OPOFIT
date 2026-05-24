package com.opofit.miapp.utils

import org.json.JSONObject
import retrofit2.HttpException

object ApiErrorParser {
    fun message(t: Throwable): String = when (t) {
        is HttpException -> fromHttp(t)
        else -> t.message ?: "Error de conexión"
    }

    fun fromHttp(e: HttpException): String {
        val serverMsg = try {
            val raw = e.response()?.errorBody()?.string()
            if (!raw.isNullOrEmpty()) JSONObject(raw).optString("msg", "") else ""
        } catch (_: Exception) {
            ""
        }
        return when {
            serverMsg.isNotEmpty() -> serverMsg
            e.code() == 403 -> "Acceso denegado (revisa configuración del servidor)"
            e.code() == 402 -> "Función Premium"
            e.code() >= 500 -> "Error del servidor. Si acabas de actualizar, espera 1 minuto al redespliegue."
            else -> "Error HTTP ${e.code()}"
        }
    }
}
