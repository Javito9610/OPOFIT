package com.opofit.miapp.data.api

import com.opofit.miapp.data.responsemodels.ActualizarAjustesRequest
import com.opofit.miapp.data.responsemodels.ActualizarAjustesResponse
import com.opofit.miapp.data.responsemodels.ActualizarPerfilRequest
import com.opofit.miapp.data.responsemodels.ActualizarPerfilResponse
import com.opofit.miapp.data.responsemodels.PerfilUsuarioResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT

interface UsuarioApi {
    @GET("/api/user/perfil")
    suspend fun obtenerPerfil(
        @Header("Authorization") token: String
    ): PerfilUsuarioResponse

    @PUT("/api/user/perfil")
    suspend fun actualizarPerfil(
        @Header("Authorization") token: String,
        @Body body: ActualizarPerfilRequest
    ): ActualizarPerfilResponse

    @PUT("/api/user/settings")
    suspend fun actualizarAjustes(
        @Header("Authorization") token: String,
        @Body body: ActualizarAjustesRequest
    ): ActualizarAjustesResponse

    @DELETE("/api/user/cuenta")
    suspend fun eliminarCuenta(
        @Header("Authorization") token: String
    ): ActualizarAjustesResponse
}
