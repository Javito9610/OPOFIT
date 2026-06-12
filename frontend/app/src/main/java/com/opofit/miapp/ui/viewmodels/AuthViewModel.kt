package com.opofit.miapp.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.opofit.miapp.data.api.BackendAuthService
import com.opofit.miapp.notifications.FcmRegistrar
import com.opofit.miapp.data.local.SessionManager
import com.opofit.miapp.data.local.UserSession
import com.opofit.miapp.data.local.TokenManager
import com.opofit.miapp.data.responsemodels.AuthResponse
import com.opofit.miapp.domain.ValidationUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Base64

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    data class AuthUiState(
        val isLoading: Boolean = false,
        val error: String = "",
        val success: Boolean = false,
        val isLoggedIn: Boolean = false,
        val isSessionChecked: Boolean = false,
        val userId: Int? = null,
        val userEmail: String? = null,
        val userName: String? = null,
        val genero: String? = null,
        val oposicionId: Int? = null,
        val peso: Double? = null,
        val altura: Double? = null,
        val imc: Double? = null,
        val modoUso: String? = null,
        val avatarUrl: String? = null,
        val diasEntrenoSemana: Int? = null
    )

    private val backendService = BackendAuthService()
    private val tokenManager = TokenManager(application)
    private val sessionManager = SessionManager(tokenManager)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkExistingSession()
    }

    private fun checkExistingSession() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val session = sessionManager.getCurrentSession().first()
                val inferredUserId = session.userId.toIntOrNull()
                    ?: decodeJwtUserId(session.token)

                if (session.token.isBlank()) {
                    _uiState.update {
                        it.copy(isLoggedIn = false, isSessionChecked = true)
                    }
                    return@launch
                }

                // Mostrar la app al instante con la sesión guardada (no esperar a Railway).
                applyCachedSession(session, inferredUserId)

                refreshSessionFromServer(session.token)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error restoring session", e)
                _uiState.update { it.copy(isSessionChecked = true) }
            }
        }
    }

    private fun resolveModoUso(user: com.opofit.miapp.data.responsemodels.UsuarioData?): String? {
        val explicit = user?.modo_uso?.trim()
        if (!explicit.isNullOrBlank()) return explicit.uppercase()
        return if (user?.oposiciones_id_oposicion == null) "FITNESS" else "OPOSITOR"
    }

    private fun applyCachedSession(
        session: UserSession,
        inferredUserId: Int?
    ) {
        val opoId = session.oposicionId.toIntOrNull()
        _uiState.update { state ->
            state.copy(
                isLoggedIn = session.isLoggedIn,
                isSessionChecked = true,
                userId = inferredUserId,
                userEmail = session.email,
                userName = session.userName,
                genero = session.genero.ifEmpty { null },
                oposicionId = opoId,
                modoUso = if (opoId == null) "FITNESS" else "OPOSITOR",
                peso = session.peso.toDoubleOrNull(),
                altura = session.altura.toDoubleOrNull(),
                imc = session.imc.toDoubleOrNull()
            )
        }
    }

    private suspend fun refreshSessionFromServer(token: String) {
        try {
            val meResult = withTimeout(8_000) {
                backendService.me(token)
            }
            meResult.fold(
                onSuccess = { me ->
                    val user = me.user ?: return
                    sessionManager.saveSession(
                        token = token,
                        email = user.email,
                        userId = user.id_usuario.toString(),
                        userName = user.nombre,
                        genero = user.genero,
                        oposicionId = user.oposiciones_id_oposicion?.toString() ?: "",
                        peso = user.peso.toString(),
                        altura = user.altura.toString(),
                        imc = user.imc.toString()
                    )
                    _uiState.update { state ->
                        state.copy(
                            isLoggedIn = true,
                            userId = user.id_usuario,
                            userEmail = user.email,
                            userName = user.nombre,
                            genero = user.genero,
                            oposicionId = user.oposiciones_id_oposicion,
                            modoUso = resolveModoUso(user),
                            avatarUrl = user.avatar_url,
                            peso = user.peso,
                            altura = user.altura,
                            imc = user.imc
                        )
                    }
                    registerFcm(token)
                },
                onFailure = {
                    sessionManager.logout()
                    _uiState.update {
                        it.copy(
                            isLoggedIn = false,
                            error = "Tu sesión ha caducado o ya no es válida. Inicia sesión de nuevo."
                        )
                    }
                }
            )
        } catch (e: TimeoutCancellationException) {
            Log.w("AuthViewModel", "Validación de sesión en segundo plano (timeout); se usa caché local")
        } catch (e: Exception) {
            Log.w("AuthViewModel", "Validación de sesión en segundo plano fallida", e)
        }
    }

    private fun decodeJwtUserId(token: String): Int? {
        return try {
            if (token.isBlank()) return null
            val parts = token.split(".")
            if (parts.size < 2) return null
            val payload = parts[1]
                .replace('-', '+')
                .replace('_', '/')
                .let { p ->
                    
                    val pad = (4 - (p.length % 4)) % 4
                    p + "=".repeat(pad)
                }
            val json = String(Base64.getDecoder().decode(payload))
            val obj = JSONObject(json)
            obj.optInt("id").takeIf { it > 0 }
        } catch (_: Exception) {
            null
        }
    }

    fun login(email: String, password: String) {
        val validationError = ValidationUtils.validateCredentials(email, password)

        if (validationError.isNotEmpty()) {
            _uiState.update {it.copy(
                error = validationError,
                isLoading = false
            )}
            return
        }

        _uiState.update { it.copy(isLoading = true, error = "")}

        viewModelScope.launch {
            try {
                val result = backendService.login(email, password)

                result.fold(
                    onSuccess = { response ->
                        sessionManager.saveSession(
                            token = response.token,
                            email = email,
                            userId = response.user?.id_usuario?.toString() ?: "",
                            userName = response.user?.nombre ?: "",
                            genero = response.user?.genero ?: "",
                            oposicionId = response.user?.oposiciones_id_oposicion?.toString() ?: "",
                            peso = response.user?.peso?.toString() ?: "",
                            altura = response.user?.altura?.toString() ?: "",
                            imc = response.user?.imc?.toString() ?: ""
                        )

                        _uiState.update { it.copy(
                            isLoading = false,
                            success = true,
                            isLoggedIn = true,
                            userId = response.user?.id_usuario,
                            userEmail = response.user?.email,
                            userName = response.user?.nombre,
                            genero = response.user?.genero,
                            oposicionId = response.user?.oposiciones_id_oposicion,
                            modoUso = resolveModoUso(response.user),
                            avatarUrl = response.user?.avatar_url,
                            peso = response.user?.peso,
                            altura = response.user?.altura,
                            imc = response.user?.imc
                        )}
                        registerFcm(response.token)
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(
                            isLoading = false,
                            error = error.message ?: "Error desconocido",
                            success = false
                        )}
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    error = "Error al guardar sesión: ${e.message}",
                    success = false
                )}
            }
        }
    }

    fun register(
        nombre: String,
        email: String,
        password: String,
        genero: String,
        peso: Double,
        altura: Double,
        oposiciones_id: Int? = null,
        modoUso: String = if (oposiciones_id == null) "FITNESS" else "OPOSITOR"
    ) {
        val validationError = ValidationUtils.validateCredentials(email, password)

        if (validationError.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                error = validationError,
                isLoading = false
            )
            return
        }

        _uiState.update { it.copy(isLoading = true, error = "") }

        viewModelScope.launch {
            try {
                val result = backendService.register(
                    nombre, email, password, genero, peso, altura, oposiciones_id, modoUso = modoUso
                )

                result.fold(
                    onSuccess = { response ->
                        sessionManager.saveSession(
                            token = response.token,
                            email = response.user?.email ?: email,
                            userId = response.userId?.toString() ?: response.user?.id_usuario?.toString() ?: "",
                            userName = response.user?.nombre ?: nombre,
                            genero = response.user?.genero ?: genero,
                            oposicionId = response.user?.oposiciones_id_oposicion?.toString() ?: oposiciones_id.toString(),
                            peso = response.user?.peso?.toString() ?: peso.toString(),
                            altura = response.user?.altura?.toString() ?: altura.toString(),
                            imc = response.user?.imc?.toString() ?: ""
                        )

                        _uiState.update { it.copy(
                            isLoading = false,
                            success = true,
                            isLoggedIn = true,
                            userId = response.userId ?: response.user?.id_usuario,
                            userEmail = response.user?.email ?: email,
                            userName = response.user?.nombre ?: nombre,
                            genero = response.user?.genero ?: genero,
                            oposicionId = response.user?.oposiciones_id_oposicion ?: oposiciones_id,
                            modoUso = resolveModoUso(response.user) ?: modoUso,
                            avatarUrl = response.user?.avatar_url,
                            peso = response.user?.peso ?: peso,
                            altura = response.user?.altura ?: altura,
                            imc = response.user?.imc
                        )}
                        registerFcm(response.token)
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(
                            isLoading = false,
                            error = error.message ?: "Error desconocido",
                            success = false
                        )}
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    error = "Error al guardar sesión: ${e.message}",
                    success = false
                )}
            }
        }
    }

    private suspend fun handleGoogleBackendResult(
        email: String,
        name: String,
        result: Result<AuthResponse>,
        defaultError: String
    ) {
        result.fold(
            onSuccess = { response ->
                val resolvedName = response.user?.nombre ?: name
                val resolvedEmail = response.user?.email ?: email
                val resolvedUserId = response.userId ?: response.user?.id_usuario
                sessionManager.saveSession(
                    token = response.token,
                    email = resolvedEmail,
                    userId = resolvedUserId?.toString() ?: decodeJwtUserId(response.token ?: "")?.toString() ?: "",
                    userName = resolvedName,
                    genero = response.user?.genero ?: "",
                    oposicionId = response.user?.oposiciones_id_oposicion?.toString() ?: "",
                    peso = response.user?.peso?.toString() ?: "",
                    altura = response.user?.altura?.toString() ?: "",
                    imc = response.user?.imc?.toString() ?: ""
                )
                _uiState.update { it.copy(
                    isLoading = false,
                    success = true,
                    isLoggedIn = true,
                    userId = resolvedUserId,
                    userEmail = resolvedEmail,
                    userName = resolvedName,
                    genero = response.user?.genero,
                    oposicionId = response.user?.oposiciones_id_oposicion,
                    peso = response.user?.peso,
                    altura = response.user?.altura,
                    imc = response.user?.imc
                )}
                registerFcm(response.token)
            },
            onFailure = { error ->
                _uiState.update { it.copy(
                    isLoading = false,
                    error = error.message ?: defaultError,
                    success = false
                )}
            }
        )
    }

    fun loginWithGoogle(idToken: String, email: String, name: String) {
        _uiState.update { it.copy(isLoading = true, error = "") }
        viewModelScope.launch {
            val result = backendService.loginWithGoogle(idToken, email, name)
            handleGoogleBackendResult(email, name, result, "Error al sincronizar con backend")
        }
    }

    fun registerWithGoogle(idToken: String, email: String, name: String) {
        _uiState.update { it.copy(isLoading = true, error = "") }
        viewModelScope.launch {
            val result = backendService.registerWithGoogle(idToken, email, name)
            handleGoogleBackendResult(email, name, result, "Error al registrar con Google")
        }
    }

    fun loginWithFirebase(idToken: String) {
        _uiState.update { it.copy(isLoading = true, error = "") }
        viewModelScope.launch {
            val result = backendService.loginWithFirebase(idToken)
            handleGoogleBackendResult(
                email = "",
                name = "Usuario",
                result = result,
                defaultError = "Error al iniciar sesión con Firebase"
            )
        }
    }

    fun registerWithFirebase(
        idToken: String,
        nombre: String,
        genero: String,
        peso: Double,
        altura: Double,
        oposicionesId: Int
    ) {
        _uiState.update { it.copy(isLoading = true, error = "") }
        viewModelScope.launch {
            val result = backendService.registerWithFirebase(
                idToken, nombre, genero, peso, altura, oposicionesId
            )
            handleGoogleBackendResult(
                email = "",
                name = nombre,
                result = result,
                defaultError = "Error al completar el registro con Google"
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            sessionManager.logout()
            _uiState.value = AuthUiState(isSessionChecked = true)
        }
    }

    fun refreshSessionFromBackend() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val token = tokenManager.getToken().first().orEmpty()
                if (token.isBlank()) return@launch

                val meResult = backendService.me(token)
                meResult.fold(
                    onSuccess = { me ->
                        val user = me.user ?: return@fold
                        sessionManager.saveSession(
                            token = token,
                            email = user.email,
                            userId = user.id_usuario.toString(),
                            userName = user.nombre,
                            genero = user.genero,
                            oposicionId = user.oposiciones_id_oposicion?.toString() ?: "",
                            peso = user.peso.toString(),
                            altura = user.altura.toString(),
                            imc = user.imc.toString()
                        )
                        _uiState.update { state ->
                            state.copy(
                                userId = user.id_usuario,
                                userEmail = user.email,
                                userName = user.nombre,
                                genero = user.genero,
                                oposicionId = user.oposiciones_id_oposicion,
                                modoUso = resolveModoUso(user),
                                avatarUrl = user.avatar_url,
                                peso = user.peso,
                                altura = user.altura,
                                imc = user.imc
                            )
                        }
                    },
                    onFailure = {  }
                )
            } catch (_: Exception) { }
        }
    }

    fun setError(message: String) {
        _uiState.update { it.copy(error = message, isLoading = false) }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = "")
    }

    fun clearSuccessFlag() {
        _uiState.update { it.copy(success = false, error = "") }
    }

    fun resetState() {
        _uiState.value = AuthUiState()
    }

    private fun registerFcm(token: String?) {
        if (token.isNullOrBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            FcmRegistrar.registrarTokenEnBackend("Bearer $token")
        }
    }
}