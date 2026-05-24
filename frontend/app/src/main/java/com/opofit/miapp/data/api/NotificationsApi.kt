package com.opofit.miapp.data.api

import com.opofit.miapp.data.responsemodels.FcmTokenRequest
import com.opofit.miapp.data.responsemodels.GenericOkResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface NotificationsApi {
    @POST("/api/notifications/token")
    suspend fun registrarToken(
        @Header("Authorization") token: String,
        @Body body: FcmTokenRequest
    ): GenericOkResponse
}
