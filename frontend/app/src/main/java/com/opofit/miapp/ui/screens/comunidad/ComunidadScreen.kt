package com.opofit.miapp.ui.screens.comunidad

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opofit.miapp.data.api.RetrofitClient
import com.opofit.miapp.data.local.TokenManager
import com.opofit.miapp.data.responsemodels.AmigoItem
import com.opofit.miapp.data.responsemodels.EnviarMensajeRequest
import com.opofit.miapp.data.responsemodels.ResponderAmistadRequest
import com.opofit.miapp.data.responsemodels.SolicitarAmistadRequest
import com.opofit.miapp.ui.viewmodels.AuthViewModel
import com.opofit.miapp.utils.ApiErrorParser
import com.opofit.miapp.utils.DateFormatUtil
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComunidadScreen(
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit
) {
    val authState by authViewModel.uiState.collectAsState()
    val oposicionId = authState.oposicionId ?: 1
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val scope = rememberCoroutineScope()

    var tab by remember { mutableIntStateOf(0) }
    var amigos by remember { mutableStateOf<List<AmigoItem>>(emptyList()) }
    var pendientes by remember { mutableStateOf<List<com.opofit.miapp.data.responsemodels.SolicitudAmistadItem>>(emptyList()) }
    var busqueda by remember { mutableStateOf("") }
    var resultados by remember { mutableStateOf<List<com.opofit.miapp.data.responsemodels.UsuarioBusqueda>>(emptyList()) }
    var chatCon by remember { mutableStateOf<AmigoItem?>(null) }
    var mensajes by remember { mutableStateOf<List<com.opofit.miapp.data.responsemodels.MensajeChat>>(emptyList()) }
    var textoMsg by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }

    fun cargarAmigos() {
        scope.launch {
            try {
                val token = tokenManager.getToken().first() ?: ""
                val r = RetrofitClient.amigosApi.listar("Bearer $token")
                if (r.ok && r.data != null) {
                    amigos = r.data.amigos.orEmpty()
                    pendientes = r.data.pendientes.orEmpty()
                }
            } catch (e: Exception) {
                msg = ApiErrorParser.message(e)
            }
        }
    }

    LaunchedEffect(Unit) { cargarAmigos() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Comunidad") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Amigos") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Buscar") })
                Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Chat") })
            }
            if (msg.isNotBlank()) {
                Text(msg, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp))
            }
            when (tab) {
                0 -> LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (pendientes.isNotEmpty()) {
                        item { Text("Solicitudes pendientes", fontWeight = FontWeight.Bold) }
                        items(pendientes) { s ->
                            Card(Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(s.solicitante_nombre)
                                    Row {
                                        TextButton(onClick = {
                                            scope.launch {
                                                val token = tokenManager.getToken().first() ?: ""
                                                RetrofitClient.amigosApi.responder(
                                                    "Bearer $token",
                                                    ResponderAmistadRequest(s.id_amistad, true)
                                                )
                                                cargarAmigos()
                                            }
                                        }) { Text("Aceptar") }
                                        TextButton(onClick = {
                                            scope.launch {
                                                val token = tokenManager.getToken().first() ?: ""
                                                RetrofitClient.amigosApi.responder(
                                                    "Bearer $token",
                                                    ResponderAmistadRequest(s.id_amistad, false)
                                                )
                                                cargarAmigos()
                                            }
                                        }) { Text("Rechazar") }
                                    }
                                }
                            }
                        }
                    }
                    item { Text("Tus amigos (misma oposición)", fontWeight = FontWeight.Bold) }
                    if (amigos.isEmpty()) item { Text("Busca compañeros en la pestaña Buscar.") }
                    items(amigos) { a ->
                        Card(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    chatCon = a
                                    tab = 2
                                    scope.launch {
                                        val token = tokenManager.getToken().first() ?: ""
                                        val c = RetrofitClient.amigosApi.chat("Bearer $token", a.amigo_id)
                                        mensajes = c.data.orEmpty()
                                    }
                                }
                        ) {
                            Text(a.amigo_nombre, Modifier.padding(16.dp), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                1 -> Column(Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = busqueda,
                        onValueChange = { busqueda = it },
                        label = { Text("Nombre del aspirante") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    val token = tokenManager.getToken().first() ?: ""
                                    val r = RetrofitClient.amigosApi.buscar("Bearer $token", busqueda, oposicionId)
                                    resultados = r.data.orEmpty()
                                } catch (e: Exception) {
                                    msg = ApiErrorParser.message(e)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Buscar en mi oposición") }
                    Spacer(Modifier.height(12.dp))
                    resultados.forEach { u ->
                        Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(u.nombre)
                                TextButton(onClick = {
                                    scope.launch {
                                        val token = tokenManager.getToken().first() ?: ""
                                        RetrofitClient.amigosApi.solicitar(
                                            "Bearer $token",
                                            SolicitarAmistadRequest(u.id_usuario)
                                        )
                                        msg = "Solicitud enviada"
                                        cargarAmigos()
                                    }
                                }) { Text("Añadir") }
                            }
                        }
                    }
                }
                2 -> Column(Modifier.padding(16.dp)) {
                    if (chatCon == null) {
                        Text("Selecciona un amigo en la pestaña Amigos para chatear.")
                    } else {
                        Text("Chat con ${chatCon!!.amigo_nombre}", fontWeight = FontWeight.Bold)
                        LazyColumn(
                            modifier = Modifier.weight(1f, fill = false).heightIn(max = 320.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(mensajes) { m ->
                                val esMio = m.id_remitente != chatCon!!.amigo_id
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (esMio) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(Modifier.padding(8.dp)) {
                                        Text(m.texto)
                                        Text(
                                            DateFormatUtil.formatearFechaHora(m.enviado_en),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = textoMsg,
                                onValueChange = { textoMsg = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Mensaje…") }
                            )
                            Button(onClick = {
                                scope.launch {
                                    val token = tokenManager.getToken().first() ?: ""
                                    RetrofitClient.amigosApi.enviarMensaje(
                                        "Bearer $token",
                                        EnviarMensajeRequest(chatCon!!.amigo_id, textoMsg)
                                    )
                                    textoMsg = ""
                                    val c = RetrofitClient.amigosApi.chat("Bearer $token", chatCon!!.amigo_id)
                                    mensajes = c.data.orEmpty()
                                }
                            }) { Text("Enviar") }
                        }
                    }
                }
            }
        }
    }
}
