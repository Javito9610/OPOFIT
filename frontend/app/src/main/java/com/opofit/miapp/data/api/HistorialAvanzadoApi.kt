package com.opofit.miapp.data.api

import com.opofit.miapp.data.responsemodels.DetalleSesionResponse
import com.opofit.miapp.data.responsemodels.HistorialEjercicioResponse
import com.opofit.miapp.data.responsemodels.HistorialPlanResponse
import com.opofit.miapp.data.responsemodels.ResumenHistorialResponse
import com.opofit.miapp.data.responsemodels.SesionesResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface HistorialAvanzadoApi {
    @GET("/api/historial-pro/resumen")
    suspend fun resumen(
        @Header("Authorization") token: String,
        @Query("periodo") periodo: String? = "week"
    ): ResumenHistorialResponse

    @GET("/api/historial-pro/sesiones")
    suspend fun sesiones(
        @Header("Authorization") token: String,
        @Query("tipo") tipo: String? = null,
        @Query("planId") planId: Int? = null,
        @Query("desde") desde: String? = null,
        @Query("hasta") hasta: String? = null,
        @Query("limit") limit: Int? = null
    ): SesionesResponse

    @GET("/api/historial-pro/sesion/{id}")
    suspend fun detalleSesion(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): DetalleSesionResponse

    @GET("/api/historial-pro/ejercicio/{idEjercicio}")
    suspend fun historialEjercicio(
        @Header("Authorization") token: String,
        @Path("idEjercicio") idEjercicio: Int
    ): HistorialEjercicioResponse

    @GET("/api/historial-pro/plan/{idPlan}")
    suspend fun historialPlan(
        @Header("Authorization") token: String,
        @Path("idPlan") idPlan: Int
    ): HistorialPlanResponse
}
