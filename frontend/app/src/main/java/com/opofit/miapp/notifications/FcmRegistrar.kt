package com.opofit.miapp.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.opofit.miapp.data.api.RetrofitClient
import com.opofit.miapp.data.responsemodels.FcmTokenRequest
import kotlinx.coroutines.tasks.await

object FcmRegistrar {
    private const val TAG = "FcmRegistrar"

    suspend fun registrarTokenEnBackend(bearerToken: String) {
        try {
            val fcmToken = FirebaseMessaging.getInstance().token.await()
            if (fcmToken.isNullOrBlank()) return
            RetrofitClient.notificationsApi.registrarToken(
                bearerToken,
                FcmTokenRequest(fcmToken)
            )
            Log.d(TAG, "FCM token registrado")
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo registrar FCM: ${e.message}")
        }
    }
}
