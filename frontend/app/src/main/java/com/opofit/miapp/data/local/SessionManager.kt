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
    val peso: String = "",
    val altura: String = "",
    val imc: String = "",
    val isLoggedIn: Boolean = false
)

class SessionManager(private val tokenManager: TokenManager) {

    fun getCurrentSession(): Flow<UserSession> =
        combine(
            listOf(
                tokenManager.getToken(),
                tokenManager.getUserEmail(),
                tokenManager.getUserId(),
                tokenManager.getUserName(),
                tokenManager.getGenero(),
                tokenManager.getPeso(),
                tokenManager.getAltura(),
                tokenManager.getImc(),
                tokenManager.getOposicionId()
            )
        ) { values ->
            val token = values.getOrNull(0) as String?
            val email = values.getOrNull(1) as String?
            val userId = values.getOrNull(2) as String?
            val name = values.getOrNull(3) as String?
            val genero = values.getOrNull(4) as String?
            val peso = values.getOrNull(5) as String?
            val altura = values.getOrNull(6) as String?
            val imc = values.getOrNull(7) as String?
            val oposicionId = values.getOrNull(8) as String?

            UserSession(
                token = token ?: "",
                email = email ?: "",
                userId = userId ?: "",
                userName = name ?: "",
                genero = genero ?: "",
                oposicionId = oposicionId ?: "",
                peso = peso ?: "",
                altura = altura ?: "",
                imc = imc ?: "",
                isLoggedIn = !token.isNullOrEmpty()
            )
        }

    suspend fun saveSession(
        token: String?,
        email: String,
        userId: String,
        userName: String,
        genero: String = "",
        oposicionId: String = "",
        peso: String = "",
        altura: String = "",
        imc: String = ""
    ) {
        tokenManager.apply {
            saveToken(token ?: "")
            saveUserEmail(email)
            saveUserId(userId)
            saveUserName(userName)
            if (genero.isNotEmpty()) saveGenero(genero)
            if (oposicionId.isNotEmpty()) saveOposicionId(oposicionId)
            if (peso.isNotEmpty()) savePeso(peso)
            if (altura.isNotEmpty()) saveAltura(altura)
            if (imc.isNotEmpty()) saveImc(imc)
        }
    }

    suspend fun logout() {
        tokenManager.clearAll()
    }
}
