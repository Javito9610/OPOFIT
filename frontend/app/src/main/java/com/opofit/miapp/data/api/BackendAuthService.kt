package com.opofit.miapp.data.api


import com.opofit.miapp.data.models.LoginRequest
import com.opofit.miapp.data.models.RegisterRequest
import com.opofit.miapp.data.models.GoogleLoginRequest
import com.opofit.miapp.data.models.MarcaInicial
import com.opofit.miapp.data.api.AuthApi
import com.opofit.miapp.data.responsemodels.AuthResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BackendAuthService {


    private val authApi = RetrofitClient.retrofit.create(AuthApi::class.java)

    suspend fun login(email: String, password: String): Result<AuthResponse> {
        return try {
            val response = withContext(Dispatchers.IO) {
                authApi.login(LoginRequest(email, password))
            }

            if (response.ok) {
                Result.success(response)
            } else {
                Result.failure(Exception(response.message ?: "Error en login"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error de conexión: ${e.message}"))
        }
    }

    suspend fun register(
        nombre: String,
        email: String,
        password: String,
        genero: String,
        peso: Double,
        altura: Double,
        oposiciones_id: Int,
        marcas: List<MarcaInicial> = emptyList()
    ): Result<AuthResponse> {
        return try {
            val response = withContext(Dispatchers.IO) {
                authApi.register(
                    RegisterRequest(
                        nombre = nombre,
                        email = email,
                        password = password,
                        genero = genero,
                        peso = peso,
                        altura = altura,
                        oposiciones_id_oposicion = oposiciones_id,
                        marcasIniciales = marcas
                    )
                )
            }

            if (response.ok) {
                Result.success(response)
            } else {
                Result.failure(Exception(response.message ?: "Error en registro"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error de conexión: ${e.message}"))
        }
    }

    suspend fun loginWithGoogle(
        googleToken: String,
        email: String,
        nombre: String
    ): Result<AuthResponse> {
        return try {
            val response = withContext(Dispatchers.IO) {
                authApi.loginWithGoogle(
                    GoogleLoginRequest(googleToken, email, nombre)
                )
            }

            if (response.ok) {
                Result.success(response)
            } else {
                Result.failure(Exception(response.message ?: "Error en login con Google"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error de conexión: ${e.message}"))
        }
    }
}