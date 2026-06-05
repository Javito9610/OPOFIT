package com.opofit.miapp.gps.service

import com.opofit.miapp.gps.model.ActivityType

/** Pasa el tipo de actividad (correr/caminar/bici) y si hay ruta planificada hacia la grabación GPS. */
object GpsRecordingContext {
    @Volatile
    var pendingType: ActivityType? = null

    @Volatile
    var conRutaPlanificada: Boolean = false

    fun prepare(type: ActivityType, conRuta: Boolean) {
        pendingType = type
        conRutaPlanificada = conRuta
    }

    fun consumeType(): ActivityType? {
        val t = pendingType
        pendingType = null
        return t
    }

    fun consumeConRuta(): Boolean {
        val r = conRutaPlanificada
        conRutaPlanificada = false
        return r
    }
}
