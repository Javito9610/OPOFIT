package com.opofit.miapp.data.api

import com.opofit.miapp.data.responsemodels.GuardarSimulacroApiResponse
import com.opofit.miapp.data.responsemodels.GuardarSimulacroRequest
import com.opofit.miapp.data.responsemodels.PruebasSimulacroResponse
import com.opofit.miapp.data.responsemodels.SimulacroHistorialResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface SimulacroApi {
    @GET("/api/simulacros/pruebas/{idOposicion}")
    suspend fun listarPruebas(
        @Header("Authorization") token: String,
        @Path("idOposicion") idOposicion: Int
    ): PruebasSimulacroResponse

    @POST("/api/simulacros/guardar")
    suspend fun guardar(
        @Header("Authorization") token: String,
        @Body body: GuardarSimulacroRequest
    ): GuardarSimulacroApiResponse

    @GET("/api/simulacros/historial/{idOposicion}")
    suspend fun historial(
        @Header("Authorization") token: String,
        @Path("idOposicion") idOposicion: Int
    ): SimulacroHistorialResponse

    @POST("/api/simulacros/aplicar-marcas")
    suspend fun aplicarMarcas(
        @Header("Authorization") token: String,
        @Body body: GuardarSimulacroRequest
    ): GuardarSimulacroApiResponse
}
