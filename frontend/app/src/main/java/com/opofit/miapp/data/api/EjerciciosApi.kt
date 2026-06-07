package com.opofit.miapp.data.api

import com.opofit.miapp.data.responsemodels.EjerciciosListResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface EjerciciosApi {
    @GET("/api/ejercicios/")
    suspend fun listarEjercicios(
        @Header("Authorization") token: String,
        @Query("busqueda") busqueda: String? = null,
        @Query("pilar") pilar: String? = null,
        @Query("categoria") categoria: String? = null,
        @Query("entorno") entorno: String? = null,
        @Query("grupo_muscular") grupoMuscular: String? = null
    ): EjerciciosListResponse
}
