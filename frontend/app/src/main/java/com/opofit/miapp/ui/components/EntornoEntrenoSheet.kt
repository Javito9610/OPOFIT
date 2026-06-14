package com.opofit.miapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opofit.miapp.data.responsemodels.EntornoEntrenoOpcion

/**
 * Entornos con material IMPLÍCITO: no preguntamos nada porque el sitio ya
 * define lo que hay (un gym tiene barras y máquinas; un box tiene KB, cuerdas
 * y anillas; un parque de calistenia tiene barras y paralelas).
 */
private val ENTORNOS_CON_MATERIAL_IMPLICITO = setOf("GYM", "CROSSFIT", "CALISTENIA")

/**
 * Catálogo de material preguntable POR entorno. Solo se muestran opciones
 * coherentes con el sitio: en PISTA no tiene sentido preguntar por banco de
 * press, y en CASA no preguntamos por ski-erg.
 */
private data class MaterialOpcion(val id: String, val label: String)

private val MATERIAL_POR_ENTORNO: Map<String, List<MaterialOpcion>> = mapOf(
    "CASA" to listOf(
        MaterialOpcion("MANCUERNAS", "Mancuernas"),
        MaterialOpcion("KB", "Kettlebell"),
        MaterialOpcion("GOMAS", "Gomas elásticas"),
        MaterialOpcion("BARRA_DOMINADAS", "Barra de dominadas (puerta)"),
        MaterialOpcion("TRX", "TRX / suspensión"),
        MaterialOpcion("COMBA", "Comba"),
        MaterialOpcion("BANCO", "Banco / step"),
        MaterialOpcion("FOAM", "Foam roller"),
        MaterialOpcion("CAJA", "Cajón pliométrico"),
        MaterialOpcion("BICI", "Bici estática / rodillo")
    ),
    "PISTA" to listOf(
        MaterialOpcion("COMBA", "Comba"),
        MaterialOpcion("GOMAS", "Gomas elásticas"),
        MaterialOpcion("KB", "Kettlebell"),
        MaterialOpcion("MANCUERNAS", "Mancuernas"),
        MaterialOpcion("BARRA_DOMINADAS", "Barras del parque"),
        MaterialOpcion("CAJA", "Cajón / banco"),
        MaterialOpcion("BICI", "Bici")
    ),
    "MIXTO" to listOf(
        MaterialOpcion("MANCUERNAS", "Mancuernas"),
        MaterialOpcion("KB", "Kettlebell"),
        MaterialOpcion("GOMAS", "Gomas elásticas"),
        MaterialOpcion("BARRA_DOMINADAS", "Barra de dominadas"),
        MaterialOpcion("TRX", "TRX"),
        MaterialOpcion("COMBA", "Comba"),
        MaterialOpcion("BANCO", "Banco"),
        MaterialOpcion("ANILLAS", "Anillas"),
        MaterialOpcion("BARRA_OLIMPICA", "Barra olímpica + discos"),
        MaterialOpcion("BICI", "Bici"),
        MaterialOpcion("PISCINA", "Piscina")
    )
)

/**
 * Sheet de selección de entorno en DOS pasos:
 *
 *   Paso 1 — ¿Dónde entrenas? (gym, casa, calistenia, pista, crossfit, mixto)
 *   Paso 2 — SOLO si el entorno no tiene material implícito (CASA, PISTA,
 *            MIXTO): ¿qué material tienes? Multi-selección con chips.
 *
 * La IA generadora de planes usa ambas respuestas: el entorno filtra el banco
 * y el material acota los ejercicios que se pueden prescribir. Así nunca más
 * un "Peso muerto con mochila" en un gimnasio ni un "Press banca" en casa
 * sin banco.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EntornoEntrenoSheet(
    visible: Boolean,
    opciones: List<EntornoEntrenoOpcion>,
    seleccionado: String?,
    onDismiss: () -> Unit,
    onConfirmar: (String) -> Unit,
    // Callback con material: si el entorno requiere preguntar, la lista llega
    // con la selección del usuario; si no (gym/box/calistenia), llega null.
    onConfirmarConMaterial: ((String, List<String>?) -> Unit)? = null
) {
    if (!visible) return
    var picked by remember(seleccionado, visible) { mutableStateOf(seleccionado) }
    var paso by remember(visible) { mutableStateOf(1) }
    var materialSel by remember(visible) { mutableStateOf(setOf<String>()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val necesitaMaterial = picked != null && picked !in ENTORNOS_CON_MATERIAL_IMPLICITO &&
        MATERIAL_POR_ENTORNO.containsKey(picked)

    // Sheet edge-to-edge: el container llega al borde, padding de gestos
    // dentro del Column (sin franja blanca).
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (paso == 1) {
                Text(
                    "¿Dónde entrenas?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Adaptamos ejercicios y material a tu entorno. Puedes cambiarlo cuando quieras.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    opciones.forEach { op ->
                        FilterChip(
                            selected = picked == op.id,
                            onClick = { picked = op.id },
                            label = { Text("${op.emoji ?: ""} ${op.etiqueta}") }
                        )
                    }
                }
                picked?.let { id ->
                    opciones.find { it.id == id }?.descripcion?.let { desc ->
                        Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    }
                }
                Button(
                    onClick = {
                        val p = picked ?: return@Button
                        if (necesitaMaterial && onConfirmarConMaterial != null) {
                            paso = 2
                        } else {
                            onConfirmarConMaterial?.invoke(p, null) ?: onConfirmar(p)
                        }
                    },
                    enabled = picked != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (necesitaMaterial && onConfirmarConMaterial != null) "Siguiente" else "Guardar y adaptar mi plan")
                }
            } else {
                // ----- Paso 2: material disponible -----
                val entornoLabel = opciones.find { it.id == picked }?.etiqueta ?: picked.orEmpty()
                Text(
                    "¿Qué material tienes en $entornoLabel?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Marca todo lo que tengas. El plan SOLO usará ejercicios posibles con tu material. " +
                        "Si no marcas nada, entrenarás con tu peso corporal.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MATERIAL_POR_ENTORNO[picked].orEmpty().forEach { m ->
                        FilterChip(
                            selected = m.id in materialSel,
                            onClick = {
                                materialSel = if (m.id in materialSel) materialSel - m.id
                                else materialSel + m.id
                            },
                            label = { Text(m.label) }
                        )
                    }
                }
                Text(
                    if (materialSel.isEmpty()) "Sin material: entrenos de peso corporal."
                    else "Seleccionado: ${materialSel.size} elemento(s).",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { paso = 1 },
                        modifier = Modifier.weight(1f)
                    ) { Text("Atrás") }
                    Button(
                        onClick = {
                            val p = picked ?: return@Button
                            val material = if (materialSel.isEmpty()) listOf("NADA") else materialSel.toList()
                            onConfirmarConMaterial?.invoke(p, material) ?: onConfirmar(p)
                        },
                        modifier = Modifier.weight(2f)
                    ) { Text("Guardar y adaptar mi plan") }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
