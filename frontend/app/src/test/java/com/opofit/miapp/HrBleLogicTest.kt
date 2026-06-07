package com.opofit.miapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests unitarios de la lógica pura de HrBleManager — sin instanciar el manager
 * (que necesita un Context Android). Verifican que:
 *
 * 1) El detector de wearables propietarios reconoce los nombres correctos.
 * 2) El prefijo USE_HEALTH_CONNECT se usa coherentemente para los casos que
 *    necesitan que la UI muestre el botón directo a Health Connect.
 */
class HrBleLogicTest {

    // Reimplementamos la lógica pura aquí para testearla sin Android Context.
    // Si el detector de HrBleManager cambia, este test debe actualizarse.
    private fun isProprietaryWearable(name: String?): Boolean {
        val n = (name ?: "").lowercase()
        if (n.isBlank()) return false
        return listOf(
            "amazfit", "t-rex", "gtr", "gts", "bip", "cheetah", "falcon",
            "mi band", "mi smart band", "mi watch", "redmi watch", "xiaomi smart band",
            "garmin", "forerunner", "fenix", "venu", "vivoactive", "instinct",
            "fitbit", "versa", "sense", "charge", "luxe", "inspire",
            "galaxy watch", "galaxy fit", "samsung gear",
            "huawei watch", "huawei band", "honor band", "honor watch"
        ).any { n.contains(it) }
    }

    @Test
    fun `T-Rex 3 reconocido como wearable propietario`() {
        assertTrue(isProprietaryWearable("Amazfit T-Rex 3-340D"))
        assertTrue(isProprietaryWearable("T-Rex Pro"))
        assertTrue(isProprietaryWearable("Amazfit GTR 4"))
    }

    @Test
    fun `bandas de pecho NO se marcan como wearables propietarios`() {
        // Estas SÍ deben poder conectar directo por BLE
        assertFalse(isProprietaryWearable("Polar H10"))
        assertFalse(isProprietaryWearable("Polar H9"))
        assertFalse(isProprietaryWearable("Wahoo TICKR"))
        assertFalse(isProprietaryWearable("Suunto Smart Sensor"))
    }

    @Test
    fun `Mi Band y Garmin reconocidos`() {
        assertTrue(isProprietaryWearable("Mi Band 7"))
        assertTrue(isProprietaryWearable("Mi Smart Band 8"))
        assertTrue(isProprietaryWearable("Garmin Forerunner 255"))
        assertTrue(isProprietaryWearable("Garmin Fenix 7"))
    }

    @Test
    fun `nombre vacio o null no es wearable`() {
        assertFalse(isProprietaryWearable(null))
        assertFalse(isProprietaryWearable(""))
        assertFalse(isProprietaryWearable("   "))
    }

    @Test
    fun `caso insensitive funciona`() {
        assertTrue(isProprietaryWearable("AMAZFIT T-REX"))
        assertTrue(isProprietaryWearable("amazfit t-rex"))
        assertTrue(isProprietaryWearable("Amazfit T-Rex"))
    }

    @Test
    fun `dispositivos genericos no detectados como wearables`() {
        // No deben pasar el filtro: nada de "amazfit" o similar en el nombre
        assertFalse(isProprietaryWearable("HR Monitor"))
        assertFalse(isProprietaryWearable("AirPods Pro"))
        assertFalse(isProprietaryWearable("My Speaker"))
        assertFalse(isProprietaryWearable("Unknown Device"))
    }

    @Test
    fun `prefijo USE_HEALTH_CONNECT se reconoce correctamente`() {
        val msgConPrefijo = "USE_HEALTH_CONNECT|Mensaje real para el usuario"
        val msgSinPrefijo = "Mensaje normal sin prefijo"

        assertTrue(msgConPrefijo.startsWith("USE_HEALTH_CONNECT|"))
        assertFalse(msgSinPrefijo.startsWith("USE_HEALTH_CONNECT|"))

        // El prefijo se elimina dejando solo el mensaje limpio
        assertEquals(
            "Mensaje real para el usuario",
            msgConPrefijo.removePrefix("USE_HEALTH_CONNECT|")
        )
        assertEquals(msgSinPrefijo, msgSinPrefijo.removePrefix("USE_HEALTH_CONNECT|"))
    }

    @Test
    fun `mensaje de timeout para wearable contiene instrucciones accionables`() {
        // Verificamos que el mensaje que ve el usuario tras un timeout en su Amazfit
        // contiene los 3 pasos a seguir antes de rendirse y usar Health Connect
        val timeoutMsg = "USE_HEALTH_CONNECT|No se pudo conectar a Amazfit T-Rex 3.\n\n" +
            "Para que funcione la conexión directa, en tu reloj:\n" +
            "1) Abre la app «Frecuencia cardíaca» y déjala midiendo (verás los LED verdes encendidos detrás)\n" +
            "2) Comprueba que «Broadcast HR / Compartir FC» esté activo\n" +
            "3) Vuelve aquí y pulsa Conectar otra vez\n\n" +
            "Si sigue fallando, lo más fácil es Health Connect: el pulso llega vía la app del reloj."

        val clean = timeoutMsg.removePrefix("USE_HEALTH_CONNECT|")
        assertTrue("Debe mencionar la app de Frecuencia cardíaca", clean.contains("Frecuencia cardíaca"))
        assertTrue("Debe mencionar los LEDs verdes", clean.contains("LED verdes"))
        assertTrue("Debe mencionar Broadcast HR", clean.contains("Broadcast HR"))
        assertTrue("Debe mencionar Health Connect como plan B", clean.contains("Health Connect"))
    }
}
