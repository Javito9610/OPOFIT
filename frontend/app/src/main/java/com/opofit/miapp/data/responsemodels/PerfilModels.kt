package com.opofit.miapp.data.responsemodels

data class MarcaActualizar(
    val id_prueba: Int,
    val valor: Double
)

data class ActualizarPerfilRequest(
    val userId: Int,
    val peso: Double,
    val altura: Double,
    val oposicionId: Int,
    val nuevasMarcas: List<MarcaActualizar>
)

data class ActualizarPerfilResponse(
    val ok: Boolean,
    val nuevoNivel: String?,
    val nuevaNota: String?,
    val msg: String? = null,
    val message: String? = null
)

data class ActualizarAjustesRequest(
    val userId: Int,
    val unidadPeso: String,
    val unidadDistancia: String,
    val horaRecordatorio: Int? = null,
    val recordatorioActivo: Boolean? = null
)

data class ActualizarAjustesResponse(
    val ok: Boolean,
    val msg: String? = null,
    val message: String? = null
)
