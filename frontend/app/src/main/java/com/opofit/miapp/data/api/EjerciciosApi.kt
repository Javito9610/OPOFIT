package com.opofit.miapp.data.api

import com.opofit.miapp.data.responsemodels.EjerciciosListResponse
import com.opofit.miapp.data.responsemodels.MaterialDisponibleResponse
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
        @Query("grupo_muscular") grupoMuscular: String? = null,
        @Query("modalidad") modalidad: String? = null,
        @Query("material") material: String? = null
    ): EjerciciosListResponse

    /** Devuelve el catálogo de material soportado para mostrar checkboxes en Ajustes. */
    @GET("/api/ejercicios/material")
    suspend fun listarMaterial(
        @Header("Authorization") token: String
    ): MaterialDisponibleResponse
}
