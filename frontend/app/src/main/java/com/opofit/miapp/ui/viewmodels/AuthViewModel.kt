package com.opofit.miapp.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.opofit.miapp.data.api.BackendAuthService
import com.opofit.miapp.data.local.SessionManager
import com.opofit.miapp.data.local.TokenManager
import com.opofit.miapp.domain.ValidationUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    data class AuthUiState(
        val isLoading: Boolean = false,
        val error: String = "",
        val success: Boolean = false,
        val isLoggedIn: Boolean = false,
        val userId: Int? = null,
        val userEmail: String? = null,
        val userName: String? = null
    )

    private val backendService = BackendAuthService()
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val tokenManager = TokenManager(application)
    private val sessionManager = SessionManager(tokenManager)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkExistingSession()
    }

    private fun checkExistingSession() {
        viewModelScope.launch {
            sessionManager.getCurrentSession().collect { session ->
                _uiState.update { state ->
                    state.copy(
                        isLoggedIn = session.isLoggedIn,
                        userId = session.userId.toIntOrNull(),
                        userEmail = session.email,
                        userName = session.userName
                    )
                }
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
            val result = backendService.login(email, password)

            result.onSuccess { response ->
                viewModelScope.launch {
                    try {
                        sessionManager.saveSession(
                            token = response.token,
                            email = email,
                            userId = response.user?.id_usuario?.toString() ?: "",
                            userName = response.user?.nombre ?: ""
                        )

                        _uiState.update { it.copy(
                            isLoading = false,
                            success = true,
                            isLoggedIn = true,
                            userId = response.user?.id_usuario,
                            userEmail = response.user?.email,
                            userName = response.user?.nombre
                        )}
                    } catch (e: Exception) {
                        _uiState.update { it.copy(
                            isLoading = false,
                            error = "Error al guardar sesión: ${e.message}",
                            success = false
                        )}
                    }
                }
            }

            result.onFailure { error ->
                _uiState.update { it.copy(
                    isLoading = false,
                    error = error.message ?: "Error desconocido",
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
            val result = backendService.register(
                nombre, email, password, genero, peso, altura, oposiciones_id
            )

            result.onSuccess { response ->
                viewModelScope.launch {
                    try {
                        sessionManager.saveSession(
                            token = response.token,
                            email = email,
                            userId = response.userId?.toString() ?: "",
                            userName = nombre
                        )

                        _uiState.update { it.copy(
                            isLoading = false,
                            success = true,
                            isLoggedIn = true,
                            userId = response.userId,
                            userEmail = response.user?.email,
                            userName = response.user?.nombre
                        )}
                    } catch (e: Exception) {
                        _uiState.update { it.copy(
                            isLoading = false,
                            error = "Error al guardar sesión: ${e.message}",
                            success = false
                        )}
                    }
                }
            }

            result.onFailure { error ->
                _uiState.update { it.copy(
                    isLoading = false,
                    error = error.message ?: "Error desconocido",
                    success = false
                )}
            }
        }
    }

    fun loginWithGoogle(idToken: String) {
        _uiState.update { _uiState.value.copy(isLoading = true, error = "") }

        viewModelScope.launch {
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = firebaseAuth.signInWithCredential(credential).await()
                val firebaseUser = authResult.user

                if (firebaseUser == null) {
                    _uiState.value = AuthUiState(
                        isLoading = false,
                        error = "Error al autenticar con Google",
                        success = false
                    )
                    return@launch
                }

                val googleToken = firebaseUser.uid
                val email = firebaseUser.email ?: ""
                val name = firebaseUser.displayName ?: "Usuario Google"

                val result = backendService.loginWithGoogle(googleToken, email, name)

                result.onSuccess { response ->
                    viewModelScope.launch {
                        try {
                            sessionManager.saveSession(
                                token = response.token,
                                email = email,
                                userId = response.userId?.toString() ?: "",
                                userName = name
                            )

                            _uiState.update { it.copy(
                                isLoading = false,
                                success = true,
                                isLoggedIn = true,
                                userId = response.userId,
                                userEmail = response.user?.email,
                                userName = response.user?.nombre
                            )}
                        } catch (e: Exception) {
                            _uiState.update { it.copy(
                                isLoading = false,
                                error = "Error al guardar sesión: ${e.message}",
                                success = false
                            )}
                        }
                    }
                }

                result.onFailure { error ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = error.message ?: "Error al sincronizar con backend",
                        success = false
                    )}
                }

            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    error = "Error: ${e.message}",
                    success = false
                )}
            }
        }
    }

    fun logout() {
        firebaseAuth.signOut()

        viewModelScope.launch {
            sessionManager.logout()
            _uiState.value = AuthUiState()
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = "")
    }

    fun resetState() {
        _uiState.value = AuthUiState()
    }
}