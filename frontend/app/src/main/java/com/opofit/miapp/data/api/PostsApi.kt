package com.opofit.miapp.data.api

import com.opofit.miapp.data.responsemodels.ComentarPostRequest
import com.opofit.miapp.data.responsemodels.ComentarPostResponse
import com.opofit.miapp.data.responsemodels.CrearPostRequest
import com.opofit.miapp.data.responsemodels.LikePostResponse
import com.opofit.miapp.data.responsemodels.PostDetailResponse
import com.opofit.miapp.data.responsemodels.PostsListResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface PostsApi {
    @GET("/api/posts/feed")
    suspend fun feed(@Header("Authorization") token: String): PostsListResponse

    @GET("/api/posts/usuario/{userId}")
    suspend fun porUsuario(
        @Header("Authorization") token: String,
        @Path("userId") userId: Int
    ): PostsListResponse

    @GET("/api/posts/{idPost}")
    suspend fun detalle(
        @Header("Authorization") token: String,
        @Path("idPost") idPost: Int
    ): PostDetailResponse

    @POST("/api/posts")
    suspend fun crear(
        @Header("Authorization") token: String,
        @Body body: CrearPostRequest
    ): PostDetailResponse

    @POST("/api/posts/{idPost}/like")
    suspend fun toggleLike(
        @Header("Authorization") token: String,
        @Path("idPost") idPost: Int
    ): LikePostResponse

    @POST("/api/posts/{idPost}/comentarios")
    suspend fun comentar(
        @Header("Authorization") token: String,
        @Path("idPost") idPost: Int,
        @Body body: ComentarPostRequest
    ): ComentarPostResponse
}
