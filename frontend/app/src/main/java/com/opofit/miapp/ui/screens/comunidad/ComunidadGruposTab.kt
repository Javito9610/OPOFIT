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
    amigos: List<com.opofit.miapp.data.responsemodels.AmigoItem> = emptyList(),
    oposicionId: Int?,
    esFitness: Boolean,
    onCrearGrupo: (String, String?, String) -> Unit,
    onUnirse: (Int) -> Unit,
    onSalir: (Int) -> Unit,
    onEliminar: (Int) -> Unit,
    onSeleccionar: (GrupoComunidad) -> Unit,
    onEnviarMensaje: (Int, String) -> Unit,
    onCrearQuedada: (Int, String, String?) -> Unit,
    onInvitar: (Int, Int) -> Unit = { _, _ -> }
) {
    var nombreNuevo by remember { mutableStateOf("") }
    var descNueva by remember { mutableStateOf("") }
    var tipoNuevo by remember { mutableStateOf("COMUNIDAD") }
    var textoMsg by remember { mutableStateOf("") }
    var tituloQuedada by remember { mutableStateOf("") }
    var lugarQuedada by remember { mutableStateOf("") }
    var grupoAEliminar by remember { mutableStateOf<GrupoComunidad?>(null) }
    var grupoAAbandonar by remember { mutableStateOf<GrupoComunidad?>(null) }
    var grupoAInvitar by remember { mutableStateOf<GrupoComunidad?>(null) }

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
                    // Selector de tipo: COMUNIDAD pública (Strava clubs) vs PRIVADO
                    // estilo WhatsApp. El usuario pidió poder hacer las dos cosas.
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        androidx.compose.material3.FilterChip(
                            selected = tipoNuevo == "COMUNIDAD",
                            onClick = { tipoNuevo = "COMUNIDAD" },
                            label = { Text("Comunidad pública") }
                        )
                        androidx.compose.material3.FilterChip(
                            selected = tipoNuevo == "PRIVADO",
                            onClick = { tipoNuevo = "PRIVADO" },
                            label = { Text("Privado (solo invitados)") }
                        )
                    }
                    Text(
                        if (tipoNuevo == "PRIVADO")
                            "Solo los amigos que invites pueden ver y entrar."
                        else
                            "Cualquier usuario podrá descubrirlo y unirse libremente.",
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                                onCrearGrupo(nombreNuevo.trim(), descNueva.ifBlank { null }, tipoNuevo)
                                nombreNuevo = ""
                                descNueva = ""
                                tipoNuevo = "COMUNIDAD"
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
                    title = "Sin grupos aún",
                    message = "Crea el primero o únete cuando aparezcan grupos de tu comunidad.",
                    icon = androidx.compose.material.icons.Icons.AutoMirrored.Outlined.Chat
                )
            }
        }
        items(grupos) { g ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(g.nombre, fontWeight = FontWeight.Bold)
                                // Badge pequeño con el tipo del grupo.
                                androidx.compose.material3.AssistChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            if (g.tipo == "PRIVADO") "🔒 Privado" else "🌐 Comunidad",
                                            style = androidx.compose.material3.MaterialTheme.typography.labelSmall
                                        )
                                    }
                                )
                            }
                            g.descripcion?.let {
                                Text(it, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                "${g.miembros} miembros",
                                style = androidx.compose.material3.MaterialTheme.typography.labelSmall
                            )
                        }
                        if (!g.soy_miembro) {
                            if (g.tipo == "PRIVADO") {
                                Text(
                                    "Solo por invitación",
                                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                OutlinedButton(onClick = { onUnirse(g.id_grupo) }) { Text("Unirse") }
                            }
                        } else {
                            TextButton(onClick = { onSeleccionar(g) }) { Text("Abrir") }
                        }
                    }
                    // Acciones del miembro: salir (no creador) / eliminar (creador) / invitar (admin).
                    if (g.soy_miembro) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (g.soy_creador) {
                                // Solo en PRIVADO el "Invitar" tiene sentido — en
                                // COMUNIDAD pública cualquiera entra solo.
                                if (g.tipo == "PRIVADO") {
                                    androidx.compose.material3.TextButton(
                                        onClick = { grupoAInvitar = g }
                                    ) { Text("Invitar amigos") }
                                }
                                androidx.compose.material3.TextButton(
                                    onClick = { grupoAEliminar = g },
                                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                        contentColor = androidx.compose.material3.MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("Eliminar grupo")
                                }
                            } else {
                                androidx.compose.material3.TextButton(
                                    onClick = { grupoAAbandonar = g }
                                ) { Text("Salir") }
                            }
                        }
                    }
                }
            }
        }
        grupoSel?.let { sel ->
            item {
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Chat: ${sel.nombre}", fontWeight = FontWeight.Bold)
                    if (sel.soy_miembro && !sel.soy_creador) {
                        androidx.compose.material3.TextButton(onClick = { grupoAAbandonar = sel }) {
                            Text("Salir")
                        }
                    }
                }
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

    // Diálogos de confirmación para acciones destructivas / irreversibles.
    grupoAEliminar?.let { g ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { grupoAEliminar = null },
            title = { Text("¿Eliminar \"${g.nombre}\"?") },
            text = {
                Text(
                    "Se borrarán todos los mensajes, quedadas y miembros. " +
                        "Esta acción no se puede deshacer."
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    onEliminar(g.id_grupo)
                    grupoAEliminar = null
                }) {
                    Text(
                        "Eliminar",
                        color = androidx.compose.material3.MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { grupoAEliminar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
    grupoAAbandonar?.let { g ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { grupoAAbandonar = null },
            title = { Text("¿Salir de \"${g.nombre}\"?") },
            text = { Text("Dejarás de recibir mensajes y novedades. Podrás volver a entrar si el grupo es público.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    onSalir(g.id_grupo)
                    grupoAAbandonar = null
                }) { Text("Salir") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { grupoAAbandonar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
    // Bottom sheet "Invitar amigos" para grupos privados.
    grupoAInvitar?.let { g ->
        com.opofit.miapp.ui.components.InvitarAmigosSheet(
            amigos = amigos,
            // miembrosActuales lo ideal es traerlo aparte, pero por ahora vacio
            // (la UI lo gestiona localmente con "invitados").
            miembrosActuales = emptySet(),
            onInvitar = { idAmigo -> onInvitar(g.id_grupo, idAmigo) },
            onClose = { grupoAInvitar = null }
        )
    }
}
