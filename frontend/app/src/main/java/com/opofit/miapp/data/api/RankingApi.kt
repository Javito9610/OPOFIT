package com.opofit.miapp.data.api

import com.opofit.miapp.data.responsemodels.GenericOkResponse
import com.opofit.miapp.data.responsemodels.MiPosicionApiResponse
import com.opofit.miapp.data.responsemodels.RankingListResponse
import com.opofit.miapp.data.responsemodels.TogglePerfilPublicoRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface RankingApi {
    @GET("/api/ranking/{idOposicion}")
    suspend fun getRanking(
        @Header("Authorization") token: String,
        @Path("idOposicion") idOposicion: Int,
        @Query("idPrueba") idPrueba: Int? = null
    ): RankingListResponse

    @GET("/api/ranking/{idOposicion}/mi-posicion")
    suspend fun miPosicion(
        @Header("Authorization") token: String,
        @Path("idOposicion") idOposicion: Int,
        @Query("idPrueba") idPrueba: Int
    ): MiPosicionApiResponse

    @PUT("/api/ranking/perfil-publico")
    suspend fun togglePerfilPublico(
        @Header("Authorization") token: String,
        @Body body: TogglePerfilPublicoRequest
    ): GenericOkResponse
}
