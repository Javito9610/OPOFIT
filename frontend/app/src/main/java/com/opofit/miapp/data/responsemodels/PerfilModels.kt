package com.opofit.miapp.data.responsemodels

data class MarcaActualizar(
    val id_prueba: Int,
    val valor: Double
)

data class PerfilUsuarioData(
    val nombre: String? = null,
    val email: String? = null,
    val peso: Double? = null,
    val altura: Double? = null,
    val imc: Double? = null,
    val avatarUrl: String? = null,
    val modoUso: String? = null,
    val oposicionId: Int? = null,
    val oposicionNombre: String? = null,
    val ubicacionVisible: Boolean = false
)

data class PerfilUsuarioResponse(
    val ok: Boolean,
    val data: PerfilUsuarioData? = null,
    val msg: String? = null
)

data class ActualizarPerfilRequest(
    val userId: Int,
    val peso: Double? = null,
    val altura: Double? = null,
    val oposicionId: Int? = null,
    val nuevasMarcas: List<MarcaActualizar> = emptyList(),
    val nombre: String? = null,
    val avatarUrl: String? = null,
    val modoUso: String? = null,
    val ubicacionVisible: Boolean? = null
)

data class CambiarPasswordRequest(
    val passwordActual: String,
    val passwordNueva: String
)

data class SubirAvatarRequest(val imagenBase64: String)

data class SubirAvatarResponse(val ok: Boolean, val avatarUrl: String? = null, val msg: String? = null)

data class OkAuthResponse(val ok: Boolean, val msg: String? = null)

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
    val recordatorioActivo: Boolean? = null,
    // Lista de códigos: ["BARRA_DOMINADAS","KB","TRX",...]. Si la app aún no
    // soporta esta sección o el usuario no ha tocado nada, dejarlo null para
    // que el backend no resetee el campo.
    val materialDisponible: List<String>? = null
)

data class ActualizarAjustesResponse(
    val ok: Boolean,
    val msg: String? = null,
    val message: String? = null
)

data class AjustesData(
    val unidadPeso: String = "kg",
    val unidadDistancia: String = "km",
    val horaRecordatorio: Int = 18,
    val recordatorioActivo: Boolean = true,
    val materialDisponible: List<String> = emptyList()
)

data class AjustesResponse(
    val ok: Boolean,
    val msg: String? = null,
    val data: AjustesData? = null
)

/** Catálogo de material disponible que devuelve el backend para los checkboxes. */
data class MaterialDisponibleItem(
    val id: String,
    val label: String,
    val icono: String? = null
)

data class MaterialDisponibleResponse(
    val ok: Boolean,
    val data: List<MaterialDisponibleItem> = emptyList()
)
