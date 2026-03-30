package com.opofit.miapp.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class UserSession(
    val token: String = "",
    val email: String = "",
    val userId: String = "",
    val userName: String = "",
    val isLoggedIn: Boolean = false
)

class SessionManager(private val tokenManager: TokenManager) {

    fun getCurrentSession(): Flow<UserSession> = combine(
        tokenManager.getToken(),
        tokenManager.getUserEmail(),
        tokenManager.getUserId(),
        tokenManager.getUserName()
    ) { token, email, userId, name ->
        UserSession(
            token = token ?: "",
            email = email ?: "",
            userId = userId ?: "",
            userName = name ?: "",
            isLoggedIn = !token.isNullOrEmpty()
        )
    }

    suspend fun saveSession(
        token: String?,
        email: String,
        userId: String,
        userName: String
    ) {
        tokenManager.apply {
            saveToken(token ?: "")
            saveUserEmail(email)
            saveUserId(userId)
            saveUserName(userName)
        }
    }

    suspend fun logout() {
        tokenManager.clearAll()
    }
}
