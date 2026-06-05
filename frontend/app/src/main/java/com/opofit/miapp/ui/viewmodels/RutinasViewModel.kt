package com.opofit.miapp.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.opofit.miapp.data.api.RetrofitClient
import com.opofit.miapp.data.local.TokenManager
import com.opofit.miapp.data.api.EntornoBody
import com.opofit.miapp.data.responsemodels.BloqueRutina
import com.opofit.miapp.data.responsemodels.EntornoEntrenoOpcion
import com.opofit.miapp.data.responsemodels.Oposicion
import com.opofit.miapp.data.responsemodels.PlanCalendario
import com.opofit.miapp.data.responsemodels.PlanSemanal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import com.opofit.miapp.utils.ApiErrorParser
import kotlinx.coroutines.launch

class RutinasViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(getApplication())

    data class RutinasUiState(
        val isLoading: Boolean = false,
        val error: String = "",
        val notaActual: String = "",
        val nivelAsignado: String = "",
        val rutinaCompleta: List<BloqueRutina> = emptyList(),
        val planSemanal: PlanSemanal? = null,
        val calendario: PlanCalendario? = null,
        val oposiciones: List<Oposicion> = emptyList(),
        val oposicionesLoading: Boolean = false,
        val pruebasFaltantes: Int? = null,
        val totalPruebas: Int? = null,
        val pruebasCompletadas: Int? = null,
        val msgPremium: String? = null,
        val nivelPremiumBloqueado: Boolean = false,
        val entornoEntreno: String? = null,
        val entornosOpciones: List<EntornoEntrenoOpcion> = emptyList(),
        val mostrarSheetEntorno: Boolean = false,
        val regenerandoPlan: Boolean = false,
        val regenerandoDiaId: Int? = null,
        val msgExito: String? = null,
        val msgAviso: String? = null
    )

    private val _uiState = MutableStateFlow(RutinasUiState())
    val uiState: StateFlow<RutinasUiState> = _uiState.asStateFlow()

    fun cargarRutina(userId: Int, oposicionId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = "") }
            try {
                val token = tokenManager.getToken().first() ?: ""
                cargarEntornoPrefs(token)
                val response = RetrofitClient.rutinasApi.getMiEntrenamiento(
                    "Bearer $token", userId, oposicionId
                )
                if (response.ok && response.data != null) {
                    val data = response.data
                    val bloqueadoPorPruebas = (data.pruebasFaltantes ?: 0) > 0
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            notaActual = data.notaActual,
                            nivelAsignado = data.nivelAsignado,
                            rutinaCompleta = data.rutinaCompleta,
                            planSemanal = data.planSemanal,
                            pruebasFaltantes = data.pruebasFaltantes,
                            totalPruebas = data.totalPruebas,
                            pruebasCompletadas = data.pruebasCompletadas,
                            msgPremium = data.msgPremium,
                            nivelPremiumBloqueado = data.nivelPremiumBloqueado == true,
                            mostrarSheetEntorno = !bloqueadoPorPruebas && it.entornoEntreno.isNullOrBlank(),
                            error = if (bloqueadoPorPruebas) "" else (response.msg ?: "")
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = response.msg ?: "No se pudo cargar la rutina") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = ApiErrorParser.message(e)) }
            }
        }
    }

    private suspend fun cargarEntornoPrefs(token: String) {
        try {
            val entornos = RetrofitClient.planesApi.getEntornos("Bearer $token")
            val usuario = RetrofitClient.planesApi.getEntornoUsuario("Bearer $token")
            _uiState.update {
                it.copy(
                    entornosOpciones = entornos.data ?: emptyList(),
                    entornoEntreno = usuario.data?.entorno
                )
            }
        } catch (_: Exception) { }
    }

    fun abrirSheetEntorno() {
        _uiState.update { it.copy(mostrarSheetEntorno = true) }
    }

    fun cerrarSheetEntorno() {
        _uiState.update { it.copy(mostrarSheetEntorno = false) }
    }

    fun guardarEntreno(userId: Int, oposicionId: Int, entorno: String) {
        viewModelScope.launch {
            try {
                val token = tokenManager.getToken().first() ?: ""
                val res = RetrofitClient.planesApi.putEntornoUsuario("Bearer $token", EntornoBody(entorno))
                if (res.ok) {
                    _uiState.update {
                        it.copy(entornoEntreno = entorno, mostrarSheetEntorno = false)
                    }
                    cargarRutina(userId, oposicionId)
                } else {
                    _uiState.update { it.copy(error = res.msg ?: "No se pudo guardar el entorno") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = ApiErrorParser.message(e)) }
            }
        }
    }

    fun consumirMsgExito() = _uiState.update { it.copy(msgExito = null) }

    fun consumirMsgAviso() = _uiState.update { it.copy(msgAviso = null) }

    private fun aviso(msg: String) = _uiState.update { it.copy(msgAviso = msg, error = "") }

    private fun contarCambiosPlan(anterior: PlanSemanal?, nuevo: PlanSemanal?): Int {
        if (anterior == null || nuevo == null) return 0
        return anterior.semana.zip(nuevo.semana).sumOf { (a, b) ->
            val prev = a.ejercicios.map { it.nombre }.toSet()
            val next = b.ejercicios.map { it.nombre }.toSet()
            (next - prev).size
        }
    }

    fun regenerarDia(userId: Int, oposicionId: Int, idPlanDia: Int) {
        viewModelScope.launch {
            val planAnterior = _uiState.value.planSemanal
            _uiState.update { it.copy(regenerandoDiaId = idPlanDia, error = "", msgExito = null) }
            try {
                val token = tokenManager.getToken().first() ?: ""
                if (_uiState.value.entornoEntreno.isNullOrBlank()) {
                    _uiState.update {
                        it.copy(
                            regenerandoDiaId = null,
                            mostrarSheetEntorno = true,
                            msgAviso = "Elige primero dónde entrenas"
                        )
                    }
                    return@launch
                }
                val res = RetrofitClient.planesApi.regenerarDia("Bearer $token", oposicionId, idPlanDia)
                if (res.ok && res.data != null) {
                    val dia = res.data.semana.find { it.id_plan_dia == idPlanDia }
                    val anterior = planAnterior?.semana?.find { it.id_plan_dia == idPlanDia }
                    val nombresAnt = anterior?.ejercicios?.map { it.nombre } ?: emptyList()
                    val nombresNue = dia?.ejercicios?.map { it.nombre } ?: emptyList()
                    val cambiosNombres = nombresNue.zip(nombresAnt).count { (n, a) -> n != a } +
                        (nombresNue.size - nombresAnt.size).coerceAtLeast(0)
                    val tituloCambio = dia?.titulo != anterior?.titulo
                    val msg = when {
                        cambiosNombres > 0 -> "Día actualizado: $cambiosNombres ejercicio(s) nuevo(s)"
                        tituloCambio -> "Día actualizado con otra sesión"
                        else -> "Sin otra variante distinta — pulsa de nuevo o prueba «Generar otra semana»"
                    }
                    _uiState.update {
                        it.copy(regenerandoDiaId = null, planSemanal = res.data, msgExito = msg)
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            regenerandoDiaId = null,
                            msgAviso = res.msg ?: "No se pudo cambiar este día"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(regenerandoDiaId = null, msgAviso = ApiErrorParser.message(e))
                }
            }
        }
    }

    fun regenerarPlan(userId: Int, oposicionId: Int) {
        viewModelScope.launch {
            val planAnterior = _uiState.value.planSemanal
            _uiState.update { it.copy(regenerandoPlan = true, error = "", msgExito = null) }
            try {
                val token = tokenManager.getToken().first() ?: ""
                if (_uiState.value.entornoEntreno.isNullOrBlank()) {
                    _uiState.update {
                        it.copy(
                            regenerandoPlan = false,
                            mostrarSheetEntorno = true,
                            msgAviso = "Elige primero dónde entrenas"
                        )
                    }
                    return@launch
                }
                val res = RetrofitClient.planesApi.regenerarPlan("Bearer $token", oposicionId)
                if (res.ok && res.data != null) {
                    val cambios = contarCambiosPlan(planAnterior, res.data)
                    val msg = if (cambios > 0) {
                        "Nueva semana generada: $cambios ejercicio(s) distintos"
                    } else {
                        "Semana regenerada — revisa cada día, puede haber variaciones en series o prescripción"
                    }
                    _uiState.update {
                        it.copy(regenerandoPlan = false, planSemanal = res.data, msgExito = msg)
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            regenerandoPlan = false,
                            msgAviso = res.msg ?: "No se pudo regenerar el plan"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(regenerandoPlan = false, msgAviso = ApiErrorParser.message(e))
                }
            }
        }
    }

    fun cargarCalendario(userId: Int, oposicionId: Int, year: Int, month: Int) {
        viewModelScope.launch {
            try {
                val token = tokenManager.getToken().first() ?: ""
                val response = RetrofitClient.planesApi.getCalendario(
                    "Bearer $token", oposicionId, year, month
                )
                if (response.ok) {
                    _uiState.update { it.copy(calendario = response.data) }
                }
            } catch (_: Exception) { }
        }
    }

    fun cargarOposiciones() {
        viewModelScope.launch {
            _uiState.update { it.copy(oposicionesLoading = true) }
            try {
                val token = tokenManager.getToken().first() ?: ""
                val response = RetrofitClient.oposicionesApi.getOposiciones("Bearer $token")
                if (response.ok && response.data != null) {
                    _uiState.update { it.copy(oposicionesLoading = false, oposiciones = response.data) }
                } else {
                    _uiState.update { it.copy(oposicionesLoading = false, error = "No se pudieron cargar las oposiciones") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(oposicionesLoading = false, error = e.message ?: "Error al cargar oposiciones") }
            }
        }
    }
}
