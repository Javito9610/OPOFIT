package com.opofit.miapp.utils

import com.opofit.miapp.data.responsemodels.BloqueRutina

/** Una rutina por enfoque (FUERZA / RESISTENCIA / VELOCIDAD), la que tenga más ejercicios. */
fun rutinasUnicasPorEnfoque(bloques: List<BloqueRutina>): List<BloqueRutina> {
    val orden = listOf("FUERZA", "RESISTENCIA", "VELOCIDAD")
    return orden.mapNotNull { enfoque ->
        bloques
            .filter { it.bloque.equals(enfoque, ignoreCase = true) }
            .maxWithOrNull(
                compareBy<BloqueRutina> { it.ejercicios.size }
                    .thenByDescending { it.id_rutina_opo ?: 0 }
            )
    }
}
