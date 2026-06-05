package com.opofit.miapp.integraciones

import android.content.Context
import com.opofit.miapp.data.api.HcActivityPayload
import com.opofit.miapp.data.api.HcImportarRequest
import com.opofit.miapp.data.api.RetrofitClient
import com.opofit.miapp.gps.data.GpsRepository

/**
 * Sincroniza entrenamientos hechos sin el móvil (reloj → Health Connect / Strava / Polar)
 * y los sube al backend de OpoFit.
 */
object EntrenoSyncService {

    data class Result(
        val hcImportadas: Int = 0,
        val hcSaltadas: Int = 0,
        val gfImportadas: Int = 0,
        val gfSaltadas: Int = 0,
        val stravaImportadas: Int = 0,
        val polarImportadas: Int = 0,
        val error: String? = null
    ) {
        fun mensaje(): String {
            if (error != null) return error
            val partes = buildList {
                if (hcImportadas > 0 || hcSaltadas > 0) {
                    add("Reloj/Health Connect: $hcImportadas nuevas")
                }
                if (gfImportadas > 0 || gfSaltadas > 0) {
                    add("Google Fit: $gfImportadas nuevas")
                }
                if (stravaImportadas > 0) add("Strava: $stravaImportadas")
                if (polarImportadas > 0) add("Polar: $polarImportadas")
            }
            return when {
                partes.isEmpty() -> "Todo al día — no hay entrenos nuevos que importar"
                else -> partes.joinToString(" · ")
            }
        }
    }

    suspend fun syncDesdeRelojYCloud(context: Context, token: String): Result {
        if (token.isBlank()) return Result(error = "Sesión no válida")

        var hcImportadas = 0
        var hcSaltadas = 0
        var gfImportadas = 0
        var gfSaltadas = 0
        var stravaImportadas = 0
        var polarImportadas = 0

        val hc = HealthConnectManager.get(context)
        if (hc.availability() == HealthConnectManager.Availability.AVAILABLE && hc.hasAllPermissions()) {
            val res = hc.syncLastDays(30)
            hcImportadas = res.importadas
            hcSaltadas = res.saltadas
            if (res.error == null) {
                subirActividadesLocalesAlBackend(context, token)
            }
        }

        val gf = GoogleFitManager.get(context)
        if (gf.hasPermissions()) {
            val res = gf.syncLastDays(30)
            gfImportadas = res.importadas
            gfSaltadas = res.saltadas
            if (res.error == null) {
                subirActividadesLocalesAlBackend(context, token)
            }
        }

        runCatching {
            val strava = RetrofitClient.integracionesApi.syncStrava("Bearer $token")
            if (strava.ok) stravaImportadas = strava.data?.importadas ?: 0
        }
        runCatching {
            val polar = RetrofitClient.integracionesApi.syncPolar("Bearer $token")
            if (polar.ok) polarImportadas = polar.data?.importadas ?: 0
        }

        return Result(
            hcImportadas = hcImportadas,
            hcSaltadas = hcSaltadas,
            gfImportadas = gfImportadas,
            gfSaltadas = gfSaltadas,
            stravaImportadas = stravaImportadas,
            polarImportadas = polarImportadas
        )
    }

    private suspend fun subirActividadesLocalesAlBackend(context: Context, token: String) {
        val repo = GpsRepository.get(context)
        val recientes = repo.listAll()
            .filter {
                it.id.startsWith("hc_") || it.id.startsWith("gf_") ||
                    it.id.startsWith("gpx_") || it.id.startsWith("tcx_")
            }
            .take(80)
            .map {
                HcActivityPayload(
                    externalId = it.id,
                    tipo = it.type.name,
                    startedAtMs = it.startedAtMs,
                    endedAtMs = it.endedAtMs,
                    durationSec = it.durationSec,
                    movingSec = it.movingSec,
                    distanceM = it.distanceM,
                    avgSpeedMps = it.avgSpeedMps,
                    maxSpeedMps = it.maxSpeedMps,
                    avgPaceSecPerKm = it.avgPaceSecPerKm,
                    minPaceSecPerKm = it.minPaceSecPerKm,
                    maxPaceSecPerKm = it.maxPaceSecPerKm,
                    elevationGainM = it.elevationGainM,
                    elevationMinM = it.elevationMinM,
                    elevationMaxM = it.elevationMaxM,
                    avgCadenceSpm = it.avgCadenceSpm
                )
            }
        if (recientes.isNotEmpty()) {
            RetrofitClient.integracionesApi.importarHealthConnect(
                "Bearer $token",
                HcImportarRequest(recientes)
            )
        }
    }
}
