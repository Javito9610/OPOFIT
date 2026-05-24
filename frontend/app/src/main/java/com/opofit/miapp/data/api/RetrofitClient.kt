package com.opofit.miapp.data.api

import com.opofit.miapp.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private val BASE_URL = BuildConfig.BASE_URL

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun <T> createService(serviceClass: Class<T>): T {
        return retrofit.create(serviceClass)
    }

    val rutinasApi: RutinasApi by lazy { retrofit.create(RutinasApi::class.java) }
    val oposicionesApi: OposicionesApi by lazy { retrofit.create(OposicionesApi::class.java) }
    val rutinasLibresApi: RutinasLibresApi by lazy { retrofit.create(RutinasLibresApi::class.java) }
    val progresoApi: ProgresoApi by lazy { retrofit.create(ProgresoApi::class.java) }
    val usuarioApi: UsuarioApi by lazy { retrofit.create(UsuarioApi::class.java) }
    val infoPruebasApi: InfoPruebasApi by lazy { retrofit.create(InfoPruebasApi::class.java) }
    val ejerciciosApi: EjerciciosApi by lazy { retrofit.create(EjerciciosApi::class.java) }
    val simulacroApi: SimulacroApi by lazy { retrofit.create(SimulacroApi::class.java) }
    val rankingApi: RankingApi by lazy { retrofit.create(RankingApi::class.java) }
    val premiumApi: PremiumApi by lazy { retrofit.create(PremiumApi::class.java) }
    val notificationsApi: NotificationsApi by lazy { retrofit.create(NotificationsApi::class.java) }
    val amigosApi: AmigosApi by lazy { retrofit.create(AmigosApi::class.java) }
    val dashboardApi: DashboardApi by lazy { retrofit.create(DashboardApi::class.java) }
}