package com.opofit.miapp.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.opofit.miapp.data.api.RetrofitClient
import com.opofit.miapp.data.local.TokenManager
import com.opofit.miapp.data.responsemodels.DashboardResumen
import com.opofit.miapp.data.responsemodels.FeedActividadItem
import com.opofit.miapp.data.responsemodels.NoticiaRss
import com.opofit.miapp.utils.ApiErrorParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(getApplication())

    data class HomeUiState(
        val loading: Boolean = true,
        val error: String = "",
        val resumen: DashboardResumen? = null,
        val feedAmigos: List<FeedActividadItem> = emptyList(),
        val noticiasRss: List<NoticiaRss> = emptyList()
    )

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun cargarResumen(oposicionId: Int, force: Boolean = false) {
        if (!force && _uiState.value.resumen != null && !_uiState.value.loading) return
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = "") }
            try {
                val token = tokenManager.getToken().first().orEmpty()
                if (token.isBlank()) {
                    _uiState.update { it.copy(loading = false, error = "Sesión no válida") }
                    return@launch
                }
                val resp = RetrofitClient.dashboardApi.resumen("Bearer $token", oposicionId)
                if (resp.ok && resp.data != null) {
                    var feed = emptyList<FeedActividadItem>()
                    var noticias = emptyList<NoticiaRss>()
                    try {
                        val fr = RetrofitClient.amigosApi.feed("Bearer $token")
                        if (fr.ok) feed = fr.data.orEmpty().take(5)
                    } catch (e: Exception) {
                        // Feed amigos es opcional: si falla la red, no
                        // bloqueamos el resto del home. Log en debug para
                        // detectar bugs reales.
                        com.opofit.miapp.utils.SafeLog.w("HomeViewModel", "cargar feed amigos", e)
                    }
                    try {
                        val rss = RetrofitClient.oposicionesApi.getNoticiasRss("Bearer $token", oposicionId)
                        if (rss.ok) {
                            // MISMO orden y filtro que Info para que coincidan
                            // las 3 destacadas en Home con las primeras de Info.
                            // Algoritmo: descarta >5 días, ordena por fecha DESC,
                            // urgente como desempate. Sin esto Home priorizaba
                            // por "relevancia" y descordinaba con Info.
                            val ahora = System.currentTimeMillis()
                            val cincoDiasMs = 5L * 24 * 60 * 60 * 1000
                            val formatos = listOf(
                                "yyyy-MM-dd'T'HH:mm:ss",
                                "yyyy-MM-dd HH:mm:ss",
                                "yyyy-MM-dd",
                                "EEE, d MMM yyyy HH:mm:ss z"
                            )
                            fun parseFecha(s: String): Long {
                                if (s.isBlank()) return Long.MAX_VALUE
                                for (f in formatos) {
                                    try {
                                        val sdf = java.text.SimpleDateFormat(f, java.util.Locale.ENGLISH)
                                        return sdf.parse(s)?.time ?: continue
                                    } catch (_: Exception) {}
                                }
                                return Long.MAX_VALUE
                            }
                            noticias = rss.data.orEmpty()
                                .filter {
                                    // Mantén las de últimos 5 días (fijas en Home)
                                    // o las que no se sepa la fecha.
                                    val t = parseFecha(it.fecha)
                                    t == Long.MAX_VALUE || ahora - t <= cincoDiasMs
                                }
                                .sortedWith(
                                    compareByDescending<NoticiaRss> { parseFecha(it.fecha) }
                                        .thenByDescending { it.urgente }
                                )
                                .take(3)
                        }
                    } catch (e: Exception) {
                        // RSS noticias también es opcional: si el feed no carga,
                        // el home funciona igual sin noticias.
                        com.opofit.miapp.utils.SafeLog.w("HomeViewModel", "cargar RSS noticias", e)
                    }
                    _uiState.update {
                        it.copy(loading = false, resumen = resp.data, feedAmigos = feed, noticiasRss = noticias)
                    }
                } else {
                    _uiState.update {
                        it.copy(loading = false, error = resp.msg ?: "No se pudo cargar el resumen")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(loading = false, error = ApiErrorParser.message(e))
                }
            }
        }
    }

    fun refresh(oposicionId: Int) = cargarResumen(oposicionId, force = true)
}
