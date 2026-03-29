package com.opofit.miapp.data.api

import com.opofit.miapp.data.responsemodels.ActualizarAjustesRequest
import com.opofit.miapp.data.responsemodels.ActualizarAjustesResponse
import com.opofit.miapp.data.responsemodels.ActualizarPerfilRequest
import com.opofit.miapp.data.responsemodels.ActualizarPerfilResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.PUT

interface UsuarioApi {
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
}
