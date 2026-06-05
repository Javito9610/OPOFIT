package com.opofit.miapp.utils

import com.opofit.miapp.gps.model.ActivityLap
import com.opofit.miapp.gps.model.ActivitySummary
import com.opofit.miapp.gps.model.ActivityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EntrenoRelojImportTest {

    private fun cardioRun() = ActivitySummary(
        type = ActivityType.RUN,
        startedAtMs = 1_700_000_000_000L,
        endedAtMs = 1_700_000_1_800_000L,
        durationSec = 1800,
        movingSec = 1800,
        distanceM = 5000.0,
        avgSpeedMps = 2.7,
        maxSpeedMps = 4.0,
        avgPaceSecPerKm = 360.0,
        maxPaceSecPerKm = 300.0,
        minPaceSecPerKm = 420.0,
        elevationGainM = 10.0,
        elevationMinM = 100.0,
        elevationMaxM = 110.0
    )

    @Test
    fun apply_cardio_rellena_distancia_y_tiempo() {
        val slots = listOf(
            EntrenoRelojImport.Slot(
                index = 0,
                nombre = "Tempo run 30 min",
                tipo = "RUN",
                pilar = "RESISTENCIA",
                objetivoSegundos = 1800,
                hecho = false,
                valor = "",
                distancia = ""
            )
        )
        val result = EntrenoRelojImport.apply(
            activity = cardioRun(),
            slots = slots,
            unidadFor = { "min" },
            esGps = { true },
            unitDist = "km"
        )
        assertEquals(1, result.updates.size)
        assertEquals(5.0, result.updates[0].valor!!.toDouble(), 0.01)
        assertEquals(5.0, result.updates[0].distancia!!.replace(",", ".").toDouble(), 0.01)
        assertEquals(true, result.updates[0].hecho)
        assertEquals(1_800_000L, result.elapsedMs)
    }

    @Test
    fun apply_laps_mapea_ejercicios_en_orden() {
        val activity = cardioRun().copy(
            distanceM = 0.0,
            durationSec = 600,
            laps = listOf(
                ActivityLap(1, 300, 0.0),
                ActivityLap(2, 1200, 3000.0)
            )
        )
        val slots = listOf(
            EntrenoRelojImport.Slot(0, "Plancha 5 min", null, "CORE", 300, false, "", ""),
            EntrenoRelojImport.Slot(1, "Trote 5 min", "RUN", "RESISTENCIA", 300, false, "", "")
        )
        val result = EntrenoRelojImport.apply(
            activity = activity,
            slots = slots,
            unidadFor = { idx -> if (idx == 0) "min" else "min" },
            esGps = { it.tipo == "RUN" },
            unitDist = "km"
        )
        assertEquals(2, result.updates.size)
        assertEquals(5.0, result.updates[0].valor!!.toDouble(), 0.01)
        assertEquals(true, result.updates[0].hecho)
        assertEquals(3.0, result.updates[1].valor!!.toDouble(), 0.01)
        assertEquals(true, result.updates[1].hecho)
    }

    @Test
    fun deduplicar_instrucciones_elimina_frases_repetidas() {
        val texto =
            "Codos apuntan al techo. Codos apuntan al techo. Mueve cada lado con simetría. Mueve cada lado con simetría."
        val limpio = EntrenoExerciseUtil.deduplicarInstrucciones(texto)
        assertTrue(limpio!!.contains("Codos apuntan"))
        assertEquals(1, limpio.split("Codos apuntan").size - 1)
        assertEquals(1, limpio.split("Mueve cada lado").size - 1)
    }
}
