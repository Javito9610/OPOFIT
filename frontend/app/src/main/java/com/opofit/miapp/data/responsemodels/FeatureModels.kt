package com.opofit.miapp.data.responsemodels

data class GenericOkResponse(
    val ok: Boolean,
    val msg: String? = null,
    val code: String? = null
)

data class PruebasSimulacroResponse(
    val ok: Boolean,
    val data: List<PruebaOficialSimulacro>? = null,
    val msg: String? = null,
    val code: String? = null
)

data class GuardarSimulacroApiResponse(
    val ok: Boolean,
    val data: GuardarSimulacroResponse? = null,
    val msg: String? = null,
    val code: String? = null
)

data class SimulacroHistorialResponse(
    val ok: Boolean,
    val data: List<SimulacroHistorialItem>? = null
)

data class RankingListResponse(
    val ok: Boolean,
    val data: List<RankingEntry>? = null
)

data class MiPosicionApiResponse(
    val ok: Boolean,
    val data: MiPosicionResponse? = null
)

data class PremiumEstadoResponse(
    val ok: Boolean,
    val data: PremiumEstado? = null,
    val msg: String? = null
)

data class PruebaOficialSimulacro(
    val id_pruebas_oficiales: Int,
    val nombre_prueba: String,
    val descripcion: String?,
    val mejor_si_es_menor: Int,
    val unidad: String,
    val unidadEtiqueta: String? = null,
    val tipo_baremo: String? = null,
    val convocatoria_ref: String? = null
)

data class ResultadoSimulacroItem(
    val id_prueba: Int,
    val valor: Double
)

data class GuardarSimulacroRequest(
    val idOposicion: Int,
    val resultados: List<ResultadoSimulacroItem>
)

data class DetalleSimulacroPrueba(
    val id_prueba: Int,
    val valor: Double,
    val nota: Int?
)

data class MejoraMarcaSimulacro(
    val idPrueba: Int,
    val nombrePrueba: String,
    val valorAnterior: Double? = null,
    val valorNuevo: Double,
    val notaNueva: Int? = null,
    val unidad: String? = null,
    val esNueva: Boolean = false
)

data class PerfilTrasSimulacro(
    val nivelActual: String? = null,
    val notaMediaActual: String? = null,
    val mejoras: List<MejoraMarcaSimulacro>? = null,
    val hayMejoras: Boolean = false,
    val nivelTrasSimulacro: String? = null,
    val notaMediaTrasSimulacro: String? = null,
    val subirNivel: Boolean = false,
    val totalPruebasOpo: Int? = null,
    val pruebasCompletadasPerfil: Int? = null
)

data class GuardarSimulacroResponse(
    val idSimulacro: Int? = null,
    val notaMedia: String? = null,
    val detalle: List<DetalleSimulacroPrueba>? = null,
    val perfil: PerfilTrasSimulacro? = null,
    val marcasActualizadas: Int? = null
)

data class SimulacroHistorialItem(
    val id_simulacro: Int,
    val fecha: String,
    val nota_media: String?
)

data class RankingEntry(
    val posicion: Int,
    val userId: Int,
    val nombre: String,
    val notaMedia: Double,
    val pruebasCompletadas: Int,
    val totalPruebasOpo: Int? = null
)

data class RankingDetallePrueba(
    val idPrueba: Int,
    val nombrePrueba: String,
    val valor: Double,
    val nota: Int,
    val unidad: String
)

data class RankingDetalleUsuario(
    val userId: Int,
    val nombre: String,
    val notaMedia: Double?,
    val pruebas: List<RankingDetallePrueba>?
)

data class RankingDetalleResponse(
    val ok: Boolean,
    val data: RankingDetalleUsuario?
)

data class MiPosicionResponse(
    val posicion: Int?,
    val total: Int,
    // El backend envia notaMedia: el modelo del front lo ignoraba y no se mostraba en la UI.
    val notaMedia: Double? = null
)

data class TogglePerfilPublicoRequest(val publico: Boolean)

data class PremiumEstado(
    val esPremium: Boolean,
    val premiumHasta: String?
)

data class FcmTokenRequest(val fcmToken: String)
