package com.opofit.miapp.data.api

import com.opofit.miapp.data.responsemodels.AmigosListResponse
import com.opofit.miapp.data.responsemodels.BuscarUsuariosResponse
import com.opofit.miapp.data.responsemodels.ChatResponse
import com.opofit.miapp.data.responsemodels.FeedActividadResponse
import com.opofit.miapp.data.responsemodels.EnviarMensajeRequest
import com.opofit.miapp.data.responsemodels.GenericOkResponse
import com.opofit.miapp.data.responsemodels.ResponderAmistadRequest
import com.opofit.miapp.data.responsemodels.SolicitarAmistadRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AmigosApi {
    @GET("/api/amigos")
    suspend fun listar(@Header("Authorization") token: String): AmigosListResponse

    @GET("/api/amigos/feed")
    suspend fun feed(@Header("Authorization") token: String): FeedActividadResponse

    /**
     * Búsqueda unificada por nombre o email. El parámetro `q` lo recibe el
     * backend y detecta automáticamente si tiene "@" para tratarlo como email
     * (match exacto priorizado). Si no, hace LIKE en nombre y email.
     * Mantenemos `nombre` también porque es lo que envía el cliente viejo.
     */
    @GET("/api/amigos/buscar")
    suspend fun buscar(
        @Header("Authorization") token: String,
        @Query("q") consulta: String? = null,
        @Query("nombre") nombre: String? = null,
        @Query("idOposicion") idOposicion: Int? = null
    ): BuscarUsuariosResponse

    @POST("/api/amigos/solicitar")
    suspend fun solicitar(
        @Header("Authorization") token: String,
        @Body body: SolicitarAmistadRequest
    ): GenericOkResponse

    @POST("/api/amigos/responder")
    suspend fun responder(
        @Header("Authorization") token: String,
        @Body body: ResponderAmistadRequest
    ): GenericOkResponse

    @GET("/api/amigos/chat/{otroId}")
    suspend fun chat(
        @Header("Authorization") token: String,
        @Path("otroId") otroId: Int
    ): ChatResponse

    @POST("/api/amigos/mensaje")
    suspend fun enviarMensaje(
        @Header("Authorization") token: String,
        @Body body: EnviarMensajeRequest
    ): GenericOkResponse
}
