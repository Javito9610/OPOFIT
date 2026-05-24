package com.opofit.miapp.data.api

import com.opofit.miapp.data.responsemodels.PremiumEstadoResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface PremiumApi {
    @GET("/api/premium/estado")
    suspend fun estado(@Header("Authorization") token: String): PremiumEstadoResponse

    @POST("/api/premium/activar-prueba")
    suspend fun activarPrueba(@Header("Authorization") token: String): PremiumEstadoResponse
}
