package com.opofit.miapp.data.api

import com.opofit.miapp.data.responsemodels.CrearSegmentoGeoRequest
import com.opofit.miapp.data.responsemodels.EsfuerzoDesdeActividadRequest
import com.opofit.miapp.data.responsemodels.EsfuerzoSegmentoRequest
import com.opofit.miapp.data.responsemodels.GenericOkResponse
import com.opofit.miapp.data.responsemodels.SegmentoRankingResponse
import com.opofit.miapp.data.responsemodels.SegmentosListResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface SegmentosApi {
    @GET("/api/segmentos")
    suspend fun listar(@Header("Authorization") token: String): SegmentosListResponse

    @GET("/api/segmentos/{id}/ranking")
    suspend fun ranking(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): SegmentoRankingResponse

    @POST("/api/segmentos/{id}/esfuerzo")
    suspend fun registrarEsfuerzo(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body body: EsfuerzoSegmentoRequest
    ): GenericOkResponse

    @POST("/api/segmentos/desde-actividad")
    suspend fun desdeActividad(
        @Header("Authorization") token: String,
        @Body body: EsfuerzoDesdeActividadRequest
    ): GenericOkResponse

    @POST("/api/segmentos/geo")
    suspend fun crearGeografico(
        @Header("Authorization") token: String,
        @Body body: CrearSegmentoGeoRequest
    ): GenericOkResponse
}
