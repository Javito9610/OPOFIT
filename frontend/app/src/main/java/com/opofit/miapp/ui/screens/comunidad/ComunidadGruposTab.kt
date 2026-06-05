package com.opofit.miapp.ui.screens.comunidad

import com.opofit.miapp.ui.components.ElevatedCard
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opofit.miapp.data.responsemodels.CrearGrupoRequest
import com.opofit.miapp.data.responsemodels.CrearQuedadaRequest
import com.opofit.miapp.data.responsemodels.EnviarMensajeGrupoRequest
import com.opofit.miapp.data.responsemodels.GrupoComunidad
import com.opofit.miapp.data.responsemodels.MensajeGrupo
import com.opofit.miapp.data.responsemodels.QuedadaGrupo
import com.opofit.miapp.ui.components.EmptyState

@Composable
fun ComunidadGruposTab(
    grupos: List<GrupoComunidad>,
    grupoSel: GrupoComunidad?,
    mensajes: List<MensajeGrupo>,
    quedadas: List<QuedadaGrupo>,
    oposicionId: Int?,
    esFitness: Boolean,
    onCrearGrupo: (String, String?) -> Unit,
    onUnirse: (Int) -> Unit,
    onSeleccionar: (GrupoComunidad) -> Unit,
    onEnviarMensaje: (Int, String) -> Unit,
    onCrearQuedada: (Int, String, String?) -> Unit
) {
    var nombreNuevo by remember { mutableStateOf("") }
    var descNueva by remember { mutableStateOf("") }
    var textoMsg by remember { mutableStateOf("") }
    var tituloQuedada by remember { mutableStateOf("") }
    var lugarQuedada by remember { mutableStateOf("") }

    LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(
                if (esFitness) "Grupos de entrenamiento" else "Grupos de tu oposición",
                fontWeight = FontWeight.Bold
            )
            Text(
                "Chatea, comparte rutinas y organiza quedadas.",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall
            )
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Crear grupo", fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = nombreNuevo,
                        onValueChange = { nombreNuevo = it },
                        label = { Text("Nombre del grupo") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = descNueva,
                        onValueChange = { descNueva = it },
                        label = { Text("Descripción (opcional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (nombreNuevo.isNotBlank()) {
                                onCrearGrupo(nombreNuevo.trim(), descNueva.ifBlank { null })
                                nombreNuevo = ""
                                descNueva = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = nombreNuevo.isNotBlank()
                    ) { Text("Crear grupo") }
                }
            }
        }
        if (grupos.isEmpty()) {
            item {
                EmptyState(
                    "💬",
                    "Sin grupos aún",
                    "Crea el primero o únete cuando aparezcan grupos de tu comunidad."
                )
            }
        }
        items(grupos) { g ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(g.nombre, fontWeight = FontWeight.Bold)
                            g.descripcion?.let {
                                Text(it, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                            }
                            Text("${g.miembros} miembros", style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                        }
                        if (!g.soy_miembro) {
                            OutlinedButton(onClick = { onUnirse(g.id_grupo) }) { Text("Unirse") }
                        } else {
                            TextButton(onClick = { onSeleccionar(g) }) { Text("Abrir") }
                        }
                    }
                }
            }
        }
        grupoSel?.let { sel ->
            item {
                Spacer(Modifier.height(8.dp))
                Text("Chat: ${sel.nombre}", fontWeight = FontWeight.Bold)
            }
            items(mensajes) { m ->
                Text(
                    "${m.nombre_usuario ?: "Usuario"}: ${m.texto}",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = textoMsg,
                        onValueChange = { textoMsg = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Escribe un mensaje…") },
                        singleLine = true
                    )
                    Button(onClick = {
                        if (textoMsg.isNotBlank()) {
                            onEnviarMensaje(sel.id_grupo, textoMsg.trim())
                            textoMsg = ""
                        }
                    }) { Text("Enviar") }
                }
            }
            item {
                Text("Quedadas", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                quedadas.forEach { q ->
                    Text("• ${q.titulo}${q.lugar?.let { " — $it" } ?: ""}")
                }
                OutlinedTextField(
                    value = tituloQuedada,
                    onValueChange = { tituloQuedada = it },
                    label = { Text("Nueva quedada") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = lugarQuedada,
                    onValueChange = { lugarQuedada = it },
                    label = { Text("Lugar") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = {
                        if (tituloQuedada.isNotBlank()) {
                            onCrearQuedada(sel.id_grupo, tituloQuedada.trim(), lugarQuedada.ifBlank { null })
                            tituloQuedada = ""
                            lugarQuedada = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Proponer quedada") }
            }
        }
    }
}
