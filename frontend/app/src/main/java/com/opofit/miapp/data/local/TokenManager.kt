package com.opofit.miapp.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "opofit_preferences")

class TokenManager(private val context: Context) {

    companion object {
        private val TOKEN_KEY = stringPreferencesKey("auth_token")
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val USER_NAME_KEY = stringPreferencesKey("user_name")
        private val USER_GENERO_KEY = stringPreferencesKey("user_genero")
        private val USER_OPOSICION_ID_KEY = stringPreferencesKey("user_oposicion_id")
        private val USER_PESO_KEY = stringPreferencesKey("user_peso")
        private val USER_ALTURA_KEY = stringPreferencesKey("user_altura")
        private val USER_IMC_KEY = stringPreferencesKey("user_imc")
        private val UNIT_PESO_KEY = stringPreferencesKey("unit_peso")
        private val UNIT_DISTANCIA_KEY = stringPreferencesKey("unit_distancia")
        private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
    }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
        }
    }

    fun getToken(): Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[TOKEN_KEY]
    }

    suspend fun saveUserEmail(email: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_EMAIL_KEY] = email
        }
    }

    fun getUserEmail(): Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_EMAIL_KEY]
    }

    suspend fun saveUserId(userId: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = userId
        }
    }

    fun getUserId(): Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_ID_KEY]
    }

    suspend fun saveUserName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_NAME_KEY] = name
        }
    }

    fun getUserName(): Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_NAME_KEY]
    }

    suspend fun saveGenero(genero: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_GENERO_KEY] = genero
        }
    }

    fun getGenero(): Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_GENERO_KEY]
    }

    suspend fun saveOposicionId(oposicionId: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_OPOSICION_ID_KEY] = oposicionId
        }
    }

    fun getOposicionId(): Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_OPOSICION_ID_KEY]
    }

    suspend fun savePeso(peso: String) {
        context.dataStore.edit { preferences -> preferences[USER_PESO_KEY] = peso }
    }

    fun getPeso(): Flow<String?> = context.dataStore.data.map { preferences -> preferences[USER_PESO_KEY] }

    suspend fun saveAltura(altura: String) {
        context.dataStore.edit { preferences -> preferences[USER_ALTURA_KEY] = altura }
    }

    fun getAltura(): Flow<String?> = context.dataStore.data.map { preferences -> preferences[USER_ALTURA_KEY] }

    suspend fun saveImc(imc: String) {
        context.dataStore.edit { preferences -> preferences[USER_IMC_KEY] = imc }
    }

    fun getImc(): Flow<String?> = context.dataStore.data.map { preferences -> preferences[USER_IMC_KEY] }

    suspend fun saveUnitPeso(unidad: String) {
        context.dataStore.edit { preferences -> preferences[UNIT_PESO_KEY] = unidad }
    }

    fun getUnitPeso(): Flow<String?> = context.dataStore.data.map { preferences -> preferences[UNIT_PESO_KEY] }

    suspend fun saveUnitDistancia(unidad: String) {
        context.dataStore.edit { preferences -> preferences[UNIT_DISTANCIA_KEY] = unidad }
    }

    fun getUnitDistancia(): Flow<String?> = context.dataStore.data.map { preferences -> preferences[UNIT_DISTANCIA_KEY] }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = enabled
        }
    }

    fun getDarkMode(): Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DARK_MODE_KEY] ?: false
    }

    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
