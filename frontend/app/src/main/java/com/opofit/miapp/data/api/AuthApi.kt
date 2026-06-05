package com.opofit.miapp.data.api

import com.opofit.miapp.data.models.LoginRequest
import com.opofit.miapp.data.models.RegisterRequest
import com.opofit.miapp.data.models.GoogleLoginRequest
import com.opofit.miapp.data.models.FirebaseLoginRequest
import com.opofit.miapp.data.models.FirebaseRegisterRequest
import com.opofit.miapp.data.responsemodels.AuthResponse
import com.opofit.miapp.data.responsemodels.CambiarPasswordRequest
import com.opofit.miapp.data.responsemodels.MeResponse
import com.opofit.miapp.data.responsemodels.OkAuthResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("/api/auth/registrar")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("/api/auth/google")
    suspend fun loginWithGoogle(@Body request: GoogleLoginRequest): AuthResponse

    @POST("/api/auth/google/registrar")
    suspend fun registerWithGoogle(@Body request: GoogleLoginRequest): AuthResponse

    @POST("/api/auth/google_firebase")
    suspend fun loginWithFirebase(@Body request: FirebaseLoginRequest): AuthResponse

    @POST("/api/auth/google_firebase/registrar")
    suspend fun registerWithFirebase(@Body request: FirebaseRegisterRequest): AuthResponse

    @GET("/api/auth/me")
    suspend fun me(@Header("Authorization") token: String): MeResponse

    @POST("/api/auth/cambiar-password")
    suspend fun cambiarPassword(
        @Header("Authorization") token: String,
        @Body body: CambiarPasswordRequest
    ): OkAuthResponse
}