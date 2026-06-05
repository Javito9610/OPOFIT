package com.opofit.miapp.data.api


import com.opofit.miapp.data.models.LoginRequest
import com.opofit.miapp.data.models.RegisterRequest
import com.opofit.miapp.data.models.GoogleLoginRequest
import com.opofit.miapp.data.models.FirebaseLoginRequest
import com.opofit.miapp.data.models.FirebaseRegisterRequest
import com.opofit.miapp.data.models.MarcaInicial
import com.opofit.miapp.data.responsemodels.AuthResponse
import com.opofit.miapp.data.responsemodels.MeResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException

class BackendAuthService {

    private val authApi = RetrofitClient.retrofit.create(AuthApi::class.java)

    suspend fun login(email: String, password: String): Result<AuthResponse> {
        return try {
            val response = withContext(Dispatchers.IO) {
                authApi.login(LoginRequest(email, password))
            }
            if (response.ok) Result.success(response)
            else Result.failure(Exception(response.msg ?: response.message ?: "Error en login"))
        } catch (e: HttpException) {
            Result.failure(Exception(parseHttpError(e)))
        } catch (e: ConnectException) {
            Result.failure(Exception("No se puede conectar al servidor. Verifica que el servidor esté activo."))
        } catch (e: SocketTimeoutException) {
            Result.failure(Exception("La conexión tardó demasiado. Inténtalo de nuevo."))
        } catch (e: Exception) {
            Result.failure(Exception("Error de conexión: ${e.message ?: "Error desconocido"}"))
        }
    }

    suspend fun register(
        nombre: String,
        email: String,
        password: String,
        genero: String,
        peso: Double,
        altura: Double,
        oposiciones_id: Int? = null,
        marcas: List<MarcaInicial> = emptyList(),
        modoUso: String = if (oposiciones_id == null) "FITNESS" else "OPOSITOR"
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
                        modo_uso = modoUso,
                        marcasIniciales = marcas
                    )
                )
            }
            if (response.ok) Result.success(response)
            else Result.failure(Exception(response.msg ?: response.message ?: "Error en registro"))
        } catch (e: HttpException) {
            Result.failure(Exception(parseHttpError(e)))
        } catch (e: ConnectException) {
            Result.failure(Exception("No se puede conectar al servidor. Verifica que el servidor esté activo."))
        } catch (e: SocketTimeoutException) {
            Result.failure(Exception("La conexión tardó demasiado. Inténtalo de nuevo."))
        } catch (e: Exception) {
            Result.failure(Exception("Error de conexión: ${e.message ?: "Error desconocido"}"))
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
            if (response.ok) Result.success(response)
            else Result.failure(Exception(response.msg ?: response.message ?: "Error en login con Google"))
        } catch (e: HttpException) {
            Result.failure(Exception(parseHttpError(e)))
        } catch (e: ConnectException) {
            Result.failure(Exception("No se puede conectar al servidor. Verifica que el servidor esté activo."))
        } catch (e: SocketTimeoutException) {
            Result.failure(Exception("La conexión tardó demasiado. Inténtalo de nuevo."))
        } catch (e: Exception) {
            Result.failure(Exception("Error de conexión: ${e.message ?: "Error desconocido"}"))
        }
    }

    suspend fun registerWithGoogle(
        googleToken: String,
        email: String,
        nombre: String
    ): Result<AuthResponse> {
        return try {
            val response = withContext(Dispatchers.IO) {
                authApi.registerWithGoogle(
                    GoogleLoginRequest(googleToken, email, nombre)
                )
            }
            if (response.ok) Result.success(response)
            else Result.failure(Exception(response.msg ?: response.message ?: "Error al registrar con Google"))
        } catch (e: HttpException) {
            Result.failure(Exception(parseHttpError(e)))
        } catch (e: ConnectException) {
            Result.failure(Exception("No se puede conectar al servidor. Verifica que el servidor esté activo."))
        } catch (e: SocketTimeoutException) {
            Result.failure(Exception("La conexión tardó demasiado. Inténtalo de nuevo."))
        } catch (e: Exception) {
            Result.failure(Exception("Error de conexión: ${e.message ?: "Error desconocido"}"))
        }
    }

    suspend fun loginWithFirebase(idToken: String): Result<AuthResponse> {
        return try {
            val response = withContext(Dispatchers.IO) {
                authApi.loginWithFirebase(FirebaseLoginRequest(idToken))
            }
            if (response.ok) Result.success(response)
            else Result.failure(Exception(response.msg ?: response.message ?: "Error en login con Firebase"))
        } catch (e: HttpException) {
            Result.failure(Exception(parseHttpError(e)))
        } catch (e: ConnectException) {
            Result.failure(Exception("No se puede conectar al servidor. Verifica que el servidor esté activo."))
        } catch (e: SocketTimeoutException) {
            Result.failure(Exception("La conexión tardó demasiado. Inténtalo de nuevo."))
        } catch (e: Exception) {
            Result.failure(Exception("Error de conexión: ${e.message ?: "Error desconocido"}"))
        }
    }

    suspend fun registerWithFirebase(
        idToken: String,
        nombre: String,
        genero: String,
        peso: Double,
        altura: Double,
        oposicionesId: Int
    ): Result<AuthResponse> {
        return try {
            val response = withContext(Dispatchers.IO) {
                authApi.registerWithFirebase(
                    FirebaseRegisterRequest(
                        idToken = idToken,
                        nombre = nombre,
                        genero = genero,
                        peso = peso,
                        altura = altura,
                        oposiciones_id_oposicion = oposicionesId
                    )
                )
            }
            if (response.ok) Result.success(response)
            else Result.failure(Exception(response.msg ?: response.message ?: "Error al registrar con Firebase"))
        } catch (e: HttpException) {
            Result.failure(Exception(parseHttpError(e)))
        } catch (e: ConnectException) {
            Result.failure(Exception("No se puede conectar al servidor. Verifica que el servidor esté activo."))
        } catch (e: SocketTimeoutException) {
            Result.failure(Exception("La conexión tardó demasiado. Inténtalo de nuevo."))
        } catch (e: Exception) {
            Result.failure(Exception("Error de conexión: ${e.message ?: "Error desconocido"}"))
        }
    }

    suspend fun me(token: String): Result<MeResponse> {
        return try {
            val response = withContext(Dispatchers.IO) { authApi.me("Bearer $token") }
            if (response.ok) Result.success(response)
            else Result.failure(Exception(response.msg ?: "Sesión inválida"))
        } catch (e: HttpException) {
            Result.failure(Exception(parseHttpError(e)))
        } catch (e: ConnectException) {
            Result.failure(Exception("No se puede conectar al servidor. Verifica que el servidor esté activo."))
        } catch (e: SocketTimeoutException) {
            Result.failure(Exception("La conexión tardó demasiado. Inténtalo de nuevo."))
        } catch (e: Exception) {
            Result.failure(Exception("Error de conexión: ${e.message ?: "Error desconocido"}"))
        }
    }

    private fun parseHttpError(e: HttpException): String {
        val serverMsg = try {
            val rawBody = e.response()?.errorBody()?.string()
            if (!rawBody.isNullOrEmpty()) JSONObject(rawBody).optString("msg", "") else ""
        } catch (_: Exception) { "" }

        return when {
            serverMsg.isNotEmpty() -> serverMsg
            e.code() == 401 -> "Credenciales incorrectas"
            e.code() == 403 -> "Acceso denegado"
            e.code() == 404 -> "Recurso no encontrado"
            e.code() == 409 -> "Este correo electrónico ya está registrado"
            e.code() == 400 -> "Datos inválidos. Comprueba los campos"
            e.code() >= 500 -> "Error del servidor. Inténtalo más tarde"
            else -> "Error de conexión (${e.code()})"
        }
    }
}
