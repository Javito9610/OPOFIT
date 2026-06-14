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

data class OnboardingBody(
    val objetivo: String,         // perder_grasa | ganar_musculo | resistencia | rendimiento
    val diasSemana: Int,
    val tiempoMin: Int,
    val lesiones: List<String> = emptyList()
)

data class OnboardingDataResponse(
    val objetivo: String? = null,
    val diasSemana: Int = 0,
    val tiempoMin: Int = 0,
    val lesiones: List<String> = emptyList()
)

data class OnboardingResponse(
    val ok: Boolean = false,
    val msg: String? = null,
    val data: OnboardingDataResponse? = null
)

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

    @PUT("/api/planes/onboarding")
    suspend fun putOnboarding(
        @Header("Authorization") token: String,
        @Body body: OnboardingBody
    ): OnboardingResponse

    @POST("/api/planes/regenerar/{idOposicion}")
    suspend fun regenerarPlan(
        @Header("Authorization") token: String,
        @Path("idOposicion") idOposicion: Int
    ): PlanRegenerarResponse

    @POST("/api/planes/regenerar-dia/{idOposicion}/{idPlanDia}")
    suspend fun regenerarDia(
        @Header("Authorization") token: String,
        @Path("idOposicion") idOposicion: Int,
        @Path("idPlanDia") idPlanDia: Int
    ): PlanRegenerarResponse
}
