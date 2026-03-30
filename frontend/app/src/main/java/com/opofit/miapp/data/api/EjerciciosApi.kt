package com.opofit.miapp.data.api

import com.opofit.miapp.data.responsemodels.EjerciciosListResponse
import retrofit2.http.GET
import retrofit2.http.Header

interface EjerciciosApi {
    @GET("/api/ejercicios/")
    suspend fun listarEjercicios(
        @Header("Authorization") token: String
    ): EjerciciosListResponse
}
