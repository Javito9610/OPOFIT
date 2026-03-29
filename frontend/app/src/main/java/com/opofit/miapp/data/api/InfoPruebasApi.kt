package com.opofit.miapp.data.api

import com.opofit.miapp.data.responsemodels.InfoPruebasResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface InfoPruebasApi {
    @GET("/api/info/{idOposicion}/{genero}")
    suspend fun getInfoPruebas(
        @Header("Authorization") token: String,
        @Path("idOposicion") idOposicion: Int,
        @Path("genero") genero: String
    ): InfoPruebasResponse
}
