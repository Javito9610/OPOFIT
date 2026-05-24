package com.opofit.miapp.data.api

import com.opofit.miapp.data.responsemodels.DashboardResumenResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface DashboardApi {
    @GET("/api/dashboard/resumen")
    suspend fun resumen(
        @Header("Authorization") token: String,
        @Query("idOposicion") idOposicion: Int? = null
    ): DashboardResumenResponse
}
