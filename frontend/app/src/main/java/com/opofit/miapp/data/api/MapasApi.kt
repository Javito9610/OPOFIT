package com.opofit.miapp.data.api

import com.opofit.miapp.data.responsemodels.LugarEntreno
import com.opofit.miapp.data.responsemodels.MapasTiposResponse
import com.opofit.miapp.data.responsemodels.RutaEntreno
import com.opofit.miapp.data.responsemodels.RutaEntrenoResponse
import com.opofit.miapp.data.responsemodels.LugaresResponse
import com.opofit.miapp.data.responsemodels.RutaPersonalizadaBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface MapasApi {
    @GET("api/mapas/tipos")
    suspend fun tipos(@Header("Authorization") token: String): MapasTiposResponse

    @GET("api/mapas/lugares")
    suspend fun lugares(
        @Header("Authorization") token: String,
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("tipo") tipo: String,
        @Query("radio") radio: Int = 5000
    ): LugaresResponse

    @GET("api/mapas/rutas/sugerida")
    suspend fun rutaSugerida(
        @Header("Authorization") token: String,
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("distKm") distKm: Double,
        @Query("variacion") variacion: Int = 0
    ): RutaEntrenoResponse

    @POST("api/mapas/rutas/personalizada")
    suspend fun rutaPersonalizada(
        @Header("Authorization") token: String,
        @Body body: RutaPersonalizadaBody
    ): RutaEntrenoResponse
}
