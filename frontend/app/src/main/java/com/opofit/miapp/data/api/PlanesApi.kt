package com.opofit.miapp.data.api

import com.opofit.miapp.data.responsemodels.EntornoUsuarioResponse
import com.opofit.miapp.data.responsemodels.EntornosListResponse
import com.opofit.miapp.data.responsemodels.PlanCalendarioResponse
import com.opofit.miapp.data.responsemodels.PlanRegenerarResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

data class EntornoBody(val entorno: String)

interface PlanesApi {
    @GET("/api/planes/calendario/{idOposicion}")
    suspend fun getCalendario(
        @Header("Authorization") token: String,
        @Path("idOposicion") idOposicion: Int,
        @Query("year") year: Int,
        @Query("month") month: Int
    ): PlanCalendarioResponse

    @GET("/api/planes/entornos")
    suspend fun getEntornos(@Header("Authorization") token: String): EntornosListResponse

    @GET("/api/planes/entorno")
    suspend fun getEntornoUsuario(@Header("Authorization") token: String): EntornoUsuarioResponse

    @PUT("/api/planes/entorno")
    suspend fun putEntornoUsuario(
        @Header("Authorization") token: String,
        @Body body: EntornoBody
    ): EntornoUsuarioResponse

    @POST("/api/planes/regenerar/{idOposicion}")
    suspend fun regenerarPlan(
        @Header("Authorization") token: String,
        @Path("idOposicion") idOposicion: Int
    ): PlanRegenerarResponse
}
