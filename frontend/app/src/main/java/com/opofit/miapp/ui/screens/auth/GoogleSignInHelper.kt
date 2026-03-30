package com.opofit.miapp.ui.screens.auth

import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException

object GoogleSignInHelper {

    private const val SIGN_IN_FAILED = 12500

    fun getErrorMessage(e: ApiException): String {
        return when (e.statusCode) {
            GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> "Inicio de sesión con Google cancelado"
            GoogleSignInStatusCodes.NETWORK_ERROR -> "Error de red. Comprueba tu conexión"
            GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS -> "Ya hay un inicio de sesión en curso"
            GoogleSignInStatusCodes.INVALID_ACCOUNT -> "Cuenta de Google no válida"
            SIGN_IN_FAILED -> "Error de configuración de Google Sign-In. Verifica la configuración de la app"
            else -> "Error en Google Sign-In (código: ${e.statusCode})"
        }
    }
}
