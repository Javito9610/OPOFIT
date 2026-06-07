package com.opofit.miapp.data.api

import com.opofit.miapp.data.responsemodels.EvolucionResponse
import com.opofit.miapp.data.responsemodels.RegistrarHistorialRequest
import com.opofit.miapp.data.responsemodels.RegistrarHistorialResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface ProgresoApi {
    @POST("/api/historial/registrar")
    suspend fun registrarEntrenamiento(
        @Header("Authorization") token: String,
        @Body body: RegistrarHistorialRequest
    ): RegistrarHistorialResponse

    @GET("/api/historial/evolucion/{userId}/{idEjercicio}")
    suspend fun getEvolucion(
        @Header("Authorization") token: String,
        @Path("userId") userId: Int,
        @Path("idEjercicio") idEjercicio: Int
    ): EvolucionResponse

    @DELETE("/api/historial/sesion/{id}")
    suspend fun borrarSesion(
        @Header("Authorization") token: String,
        @Path("id") idSesion: Int
    ): com.opofit.miapp.data.responsemodels.SimpleOkResponse
}
