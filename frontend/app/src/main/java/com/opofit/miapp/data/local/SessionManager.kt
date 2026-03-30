package com.opofit.miapp.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class UserSession(
    val token: String = "",
    val email: String = "",
    val userId: String = "",
    val userName: String = "",
    val genero: String = "",
    val oposicionId: String = "",
    val isLoggedIn: Boolean = false
)

class SessionManager(private val tokenManager: TokenManager) {

    fun getCurrentSession(): Flow<UserSession> = combine(
        tokenManager.getToken(),
        tokenManager.getUserEmail(),
        tokenManager.getUserId(),
        tokenManager.getUserName(),
        tokenManager.getGenero()
    ) { token, email, userId, name, genero ->
        UserSession(
            token = token ?: "",
            email = email ?: "",
            userId = userId ?: "",
            userName = name ?: "",
            genero = genero ?: "",
            isLoggedIn = !token.isNullOrEmpty()
        )
    }.combine(tokenManager.getOposicionId()) { session, oposicionId ->
        session.copy(oposicionId = oposicionId ?: "")
    }

    suspend fun saveSession(
        token: String?,
        email: String,
        userId: String,
        userName: String,
        genero: String = "",
        oposicionId: String = ""
    ) {
        tokenManager.apply {
            saveToken(token ?: "")
            saveUserEmail(email)
            saveUserId(userId)
            saveUserName(userName)
            if (genero.isNotEmpty()) saveGenero(genero)
            if (oposicionId.isNotEmpty()) saveOposicionId(oposicionId)
        }
    }

    suspend fun logout() {
        tokenManager.clearAll()
    }
}
