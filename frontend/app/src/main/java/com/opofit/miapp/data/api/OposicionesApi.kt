package com.opofit.miapp.data.api

import com.opofit.miapp.data.responsemodels.NoticiasRssResponse
import com.opofit.miapp.data.responsemodels.OposicionDetalleResponse
import com.opofit.miapp.data.responsemodels.OposicionesListResponse
import com.opofit.miapp.data.responsemodels.RequisitosResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface OposicionesApi {
    @GET("/api/oposiciones/")
    suspend fun getOposiciones(
        @Header("Authorization") token: String
    ): OposicionesListResponse

    @GET("/api/oposiciones/{id}")
    suspend fun getInfoOposicion(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): OposicionDetalleResponse

    @GET("/api/oposiciones/requisitos/{id}/{genero}")
    suspend fun getRequisitos(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Path("genero") genero: String
    ): RequisitosResponse

    @GET("/api/oposiciones/rss/{id}")
    suspend fun getNoticiasRss(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): NoticiasRssResponse
}
