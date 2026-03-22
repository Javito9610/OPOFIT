package com.opofit.miapp.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val nombre: String,
    val email: String,
    val password: String,
    val genero: String,
    val peso: Double,
    val altura: Double,
    val oposiciones_id_oposicion: Int,
    val marcasIniciales: List<MarcaInicial> = emptyList()
)

data class MarcaInicial(
    val id_prueba: Int,
    val valor: Double
)

data class GoogleLoginRequest(
    val googleToken: String,
    val email: String,
    val nombre: String
)



data class AuthResponse(
    val ok: Boolean,
    val userId: Int? = null,
    val message: String? = null,
    val user: UsuarioData? = null,
    val token: String? = null
)

data class UsuarioData(
    val id_usuario: Int,
    val nombre: String,
    val email: String,
    val genero: String,
    val peso: Double,
    val altura: Double,
    val imc: Double
)


interface AuthApi {

    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("/api/auth/registrar")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("/api/auth/google")
    suspend fun loginWithGoogle(@Body request: GoogleLoginRequest): AuthResponse
}


class BackendAuthService {


    private val baseUrl = "http://10.0.2.2:3000/"


    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()


    private val authApi = retrofit.create(AuthApi::class.java)


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