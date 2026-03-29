package com.opofit.miapp.data.api

import com.opofit.miapp.data.responsemodels.CrearRutinaLibreRequest
import com.opofit.miapp.data.responsemodels.CrearRutinaLibreResponse
import com.opofit.miapp.data.responsemodels.RutinasLibresListResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface RutinasLibresApi {
    @POST("/api/rutinas-pers/crear")
    suspend fun crearRutina(
        @Header("Authorization") token: String,
        @Body body: CrearRutinaLibreRequest
    ): CrearRutinaLibreResponse

    @GET("/api/rutinas-pers/usuario/{userId}")
    suspend fun getRutinasUsuario(
        @Header("Authorization") token: String,
        @Path("userId") userId: Int
    ): RutinasLibresListResponse

    @retrofit2.http.DELETE("/api/rutinas-pers/eliminar/{userId}/{idRutina}")
    suspend fun eliminarRutina(
        @Header("Authorization") token: String,
        @Path("userId") userId: Int,
        @Path("idRutina") idRutina: Int
    ): CrearRutinaLibreResponse
}
