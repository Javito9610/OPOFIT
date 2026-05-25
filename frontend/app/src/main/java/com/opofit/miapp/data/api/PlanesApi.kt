package com.opofit.miapp.data.api

import com.opofit.miapp.data.responsemodels.PlanCalendarioResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface PlanesApi {
    @GET("/api/planes/calendario/{idOposicion}")
    suspend fun getCalendario(
        @Header("Authorization") token: String,
        @Path("idOposicion") idOposicion: Int,
        @Query("year") year: Int,
        @Query("month") month: Int
    ): PlanCalendarioResponse
}
