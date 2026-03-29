package com.opofit.miapp.data.api

import com.opofit.miapp.data.responsemodels.RutinasResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface RutinasApi {
    @GET("/api/rutinas/mi-entrenamiento/{userId}/{idOposicion}")
    suspend fun getMiEntrenamiento(
        @Header("Authorization") token: String,
        @Path("userId") userId: Int,
        @Path("idOposicion") idOposicion: Int
    ): RutinasResponse
}
