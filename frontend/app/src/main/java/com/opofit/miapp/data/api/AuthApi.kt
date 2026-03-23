package com.opofit.miapp.data.network

import com.opofit.miapp.data.models.LoginRequest
import com.opofit.miapp.data.models.RegisterRequest
import com.opofit.miapp.data.models.GoogleLoginRequest
import com.opofit.miapp.data.responsemodels.AuthResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("/api/auth/registrar")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("/api/auth/google")
    suspend fun loginWithGoogle(@Body request: GoogleLoginRequest): AuthResponse
}