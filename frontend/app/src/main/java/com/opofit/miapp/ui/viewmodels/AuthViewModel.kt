package com.opofit.miapp.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.opofit.miapp.data.api.BackendAuthService
import com.opofit.miapp.data.local.SessionManager
import com.opofit.miapp.data.local.TokenManager
import com.opofit.miapp.data.responsemodels.AuthResponse
import com.opofit.miapp.domain.ValidationUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
        val oposicionId: Int? = null
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
                _uiState.update { state ->
                    state.copy(
                        isLoggedIn = session.isLoggedIn,
                        isSessionChecked = true,
                        userId = session.userId.toIntOrNull(),
                        userEmail = session.email,
                        userName = session.userName,
                        genero = session.genero.ifEmpty { null },
                        oposicionId = session.oposicionId.toIntOrNull()
                    )
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error restoring session", e)
                _uiState.update { it.copy(isSessionChecked = true) }
            }
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
                            oposicionId = response.user?.oposiciones_id_oposicion?.toString() ?: ""
                        )

                        _uiState.update { it.copy(
                            isLoading = false,
                            success = true,
                            isLoggedIn = true,
                            userId = response.user?.id_usuario,
                            userEmail = response.user?.email,
                            userName = response.user?.nombre,
                            genero = response.user?.genero,
                            oposicionId = response.user?.oposiciones_id_oposicion
                        )}
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
        oposiciones_id: Int
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
                    nombre, email, password, genero, peso, altura, oposiciones_id
                )

                result.fold(
                    onSuccess = { response ->
                        sessionManager.saveSession(
                            token = response.token,
                            email = response.user?.email ?: email,
                            userId = response.userId?.toString() ?: response.user?.id_usuario?.toString() ?: "",
                            userName = response.user?.nombre ?: nombre,
                            genero = response.user?.genero ?: genero,
                            oposicionId = response.user?.oposiciones_id_oposicion?.toString() ?: oposiciones_id.toString()
                        )

                        _uiState.update { it.copy(
                            isLoading = false,
                            success = true,
                            isLoggedIn = true,
                            userId = response.userId ?: response.user?.id_usuario,
                            userEmail = response.user?.email ?: email,
                            userName = response.user?.nombre ?: nombre,
                            genero = response.user?.genero ?: genero,
                            oposicionId = response.user?.oposiciones_id_oposicion ?: oposiciones_id
                        )}
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
                sessionManager.saveSession(
                    token = response.token,
                    email = email,
                    userId = response.userId?.toString() ?: "",
                    userName = resolvedName,
                    genero = response.user?.genero ?: "",
                    oposicionId = response.user?.oposiciones_id_oposicion?.toString() ?: ""
                )
                _uiState.update { it.copy(
                    isLoading = false,
                    success = true,
                    isLoggedIn = true,
                    userId = response.userId,
                    userEmail = response.user?.email,
                    userName = resolvedName,
                    genero = response.user?.genero,
                    oposicionId = response.user?.oposiciones_id_oposicion
                )}
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

    fun logout() {
        viewModelScope.launch {
            sessionManager.logout()
            _uiState.value = AuthUiState(isSessionChecked = true)
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
}