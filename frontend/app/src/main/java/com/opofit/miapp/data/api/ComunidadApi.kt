package com.opofit.miapp.data.api

import com.opofit.miapp.data.responsemodels.CercaResponse
import com.opofit.miapp.data.responsemodels.CrearGrupoRequest
import com.opofit.miapp.data.responsemodels.CrearQuedadaRequest
import com.opofit.miapp.data.responsemodels.EnviarMensajeGrupoRequest
import com.opofit.miapp.data.responsemodels.GrupoComunidad
import com.opofit.miapp.data.responsemodels.GruposListResponse
import com.opofit.miapp.data.responsemodels.MensajeGrupo
import com.opofit.miapp.data.responsemodels.MensajesGrupoResponse
import com.opofit.miapp.data.responsemodels.OkMsgResponse
import com.opofit.miapp.data.responsemodels.QuedadaGrupo
import com.opofit.miapp.data.responsemodels.QuedadasResponse
import com.opofit.miapp.data.responsemodels.UbicacionRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ComunidadApi {
    @GET("/api/comunidad/grupos")
    suspend fun listarGrupos(
        @Header("Authorization") token: String,
        @Query("idOposicion") idOposicion: Int? = null
    ): GruposListResponse

    @POST("/api/comunidad/grupos")
    suspend fun crearGrupo(
        @Header("Authorization") token: String,
        @Body body: CrearGrupoRequest
    ): GruposListResponse

    @POST("/api/comunidad/grupos/{id}/unirse")
    suspend fun unirseGrupo(
        @Header("Authorization") token: String,
        @Path("id") idGrupo: Int
    ): OkMsgResponse

    @GET("/api/comunidad/grupos/{id}/mensajes")
    suspend fun mensajesGrupo(
        @Header("Authorization") token: String,
        @Path("id") idGrupo: Int
    ): MensajesGrupoResponse

    @POST("/api/comunidad/grupos/{id}/mensajes")
    suspend fun enviarMensajeGrupo(
        @Header("Authorization") token: String,
        @Path("id") idGrupo: Int,
        @Body body: EnviarMensajeGrupoRequest
    ): MensajesGrupoResponse

    @GET("/api/comunidad/grupos/{id}/quedadas")
    suspend fun quedadasGrupo(
        @Header("Authorization") token: String,
        @Path("id") idGrupo: Int
    ): QuedadasResponse

    @POST("/api/comunidad/grupos/{id}/quedadas")
    suspend fun crearQuedada(
        @Header("Authorization") token: String,
        @Path("id") idGrupo: Int,
        @Body body: CrearQuedadaRequest
    ): QuedadasResponse

    @PUT("/api/comunidad/ubicacion")
    suspend fun actualizarUbicacion(
        @Header("Authorization") token: String,
        @Body body: UbicacionRequest
    ): OkMsgResponse

    @GET("/api/comunidad/cerca")
    suspend fun listarCerca(
        @Header("Authorization") token: String,
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radioKm") radioKm: Double = 15.0
    ): CercaResponse
}
