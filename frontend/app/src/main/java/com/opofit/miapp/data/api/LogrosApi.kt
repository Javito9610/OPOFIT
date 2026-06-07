package com.opofit.miapp.data.api

import com.opofit.miapp.data.responsemodels.LogrosResponse
import retrofit2.http.GET
import retrofit2.http.Header

interface LogrosApi {
    @GET("/api/logros")
    suspend fun misLogros(@Header("Authorization") token: String): LogrosResponse
}
