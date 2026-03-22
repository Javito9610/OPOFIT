package com.opofit.miapp.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.opofit.miapp.data.BackendAuthService
import com.opofit.miapp.data.MarcaInicial
import com.opofit.miapp.domain.ValidationUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


data class AuthUiState(
    val isLoading: Boolean = false,           // ¿Está cargando? (mostramos spinner)
    val error: String = "",                   // Mensaje de error (vacío = sin error)
    val success: Boolean = false,             // ¿Login exitoso?
    val userId: Int? = null,                  // ID del usuario autenticado
    val userEmail: String? = null,            // Email del usuario
    val userName: String? = null              // Nombre del usuario
)


class AuthViewModel : ViewModel() {

    private val backendService = BackendAuthService()
    private val firebaseAuth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(AuthUiState())

    val uiState= _uiState.asStateFlow()

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
                _uiState.value = AuthUiState(
                    isLoading = false,
                    success = true,
                    userId = response.user?.id_usuario,
                    userEmail = response.user?.email,
                    userName = response.user?.nombre
                )
            }

            result.onFailure { error ->
                _uiState.value = AuthUiState(
                    isLoading = false,
                    error = error.message ?: "Error desconocido",
                    success = false
                )
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
                _uiState.value = AuthUiState(
                    isLoading = false,
                    success = true,
                    userId = response.userId,
                    userEmail = response.user?.email,
                    userName = response.user?.nombre
                )
            }

            result.onFailure { error ->
                _uiState.value = AuthUiState(
                    isLoading = false,
                    error = error.message ?: "Error desconocido",
                    success = false
                )
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

                // PASO 2: Enviamos datos a TU BACKEND
                val googleToken = firebaseUser.uid
                val email = firebaseUser.email ?: ""
                val name = firebaseUser.displayName ?: "Usuario Google"

                val result = backendService.loginWithGoogle(googleToken, email, name)

                // PASO 3: Manejamos la respuesta del backend
                result.onSuccess { response ->
                    _uiState.value = AuthUiState(
                        isLoading = false,
                        success = true,
                        userId = response.userId,
                        userEmail = response.user?.email,
                        userName = response.user?.nombre
                    )
                }

                result.onFailure { error ->
                    _uiState.value = AuthUiState(
                        isLoading = false,
                        error = error.message ?: "Error al sincronizar con backend",
                        success = false
                    )
                }

            } catch (e: Exception) {
                _uiState.value = AuthUiState(
                    isLoading = false,
                    error = "Error: ${e.message}",
                    success = false
                )
            }
        }
    }


    fun logout() {
        firebaseAuth.signOut()
        _uiState.value = AuthUiState() // Reiniciamos estado
    }


    fun clearError() {
        _uiState.value = _uiState.value.copy(error = "")
    }


    fun resetState() {
        _uiState.value = AuthUiState()
    }
}