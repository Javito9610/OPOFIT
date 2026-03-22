package com.opofit.miapp.domain

object ValidationUtils {


    fun isValidEmail(email: String): Boolean {
        val emailPattern = "^[A-Za-z0-9+_.-]+@(.+)$".toRegex()
        return email.matches(emailPattern)
    }


    fun isValidPassword(password: String): Boolean {
        return password.length >= 6 && password.length <= 20
    }


    fun isEmailNotEmpty(email: String): Boolean {
        return email.trim().isNotEmpty()
    }


    fun isPasswordNotEmpty(password: String): Boolean {
        return password.trim().isNotEmpty()
    }


    fun validateCredentials(email: String, password: String): String {

        if (!isEmailNotEmpty(email)) {
            return "El email no puede estar vacío"
        }

        if (!isPasswordNotEmpty(password)) {
            return "La contraseña no puede estar vacía"
        }


        if (!isValidEmail(email)) {
            return "El email no tiene un formato válido"
        }

        if (!isValidPassword(password)) {
            return "La contraseña debe tener entre 6 y 20 caracteres"
        }


        return ""
    }
}