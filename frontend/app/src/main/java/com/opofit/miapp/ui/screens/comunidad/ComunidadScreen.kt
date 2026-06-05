package com.opofit.miapp.ui.screens.comunidad

import com.opofit.miapp.ui.components.ElevatedCard
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opofit.miapp.data.api.RetrofitClient
import com.opofit.miapp.data.local.TokenManager
import com.opofit.miapp.data.responsemodels.AmigoItem
import com.opofit.miapp.data.responsemodels.ActividadPost
import com.opofit.miapp.ui.components.EmptyState
import com.opofit.miapp.ui.components.PostFeedCard
import com.opofit.miapp.ui.components.ProfileAvatar
import com.opofit.miapp.data.responsemodels.CrearGrupoRequest
import com.opofit.miapp.data.responsemodels.CrearQuedadaRequest
import com.opofit.miapp.data.responsemodels.EnviarMensajeGrupoRequest
import com.opofit.miapp.data.responsemodels.EnviarMensajeRequest
import com.opofit.miapp.data.responsemodels.GrupoComunidad
import com.opofit.miapp.data.responsemodels.MensajeGrupo
import com.opofit.miapp.data.responsemodels.QuedadaGrupo
import com.opofit.miapp.data.responsemodels.ResponderAmistadRequest
import com.opofit.miapp.data.responsemodels.SolicitarAmistadRequest
import com.opofit.miapp.data.responsemodels.UbicacionRequest
import com.opofit.miapp.data.responsemodels.UsuarioCerca
import com.opofit.miapp.ui.viewmodels.AuthViewModel
import com.opofit.miapp.utils.ApiErrorParser
import com.opofit.miapp.utils.DateFormatUtil
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import retrofit2.HttpException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComunidadScreen(
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onOpenPost: (Int) -> Unit = {},
    onOpenSegmentos: () -> Unit = {}
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
    var feed by remember { mutableStateOf<List<ActividadPost>>(emptyList()) }
    var loadingFeed by remember { mutableStateOf(false) }
    var grupos by remember { mutableStateOf<List<GrupoComunidad>>(emptyList()) }
    var grupoSel by remember { mutableStateOf<GrupoComunidad?>(null) }
    var mensajesGrupo by remember { mutableStateOf<List<MensajeGrupo>>(emptyList()) }
    var quedadas by remember { mutableStateOf<List<QuedadaGrupo>>(emptyList()) }
    var cerca by remember { mutableStateOf<List<UsuarioCerca>>(emptyList()) }
    var ubicacionVisible by remember { mutableStateOf(false) }
    val esFitness = authState.modoUso?.equals("FITNESS", ignoreCase = true) == true

    fun cargarFeed() {
        scope.launch {
            loadingFeed = true
            try {
                val token = tokenManager.getToken().first() ?: ""
                val r = RetrofitClient.postsApi.feed("Bearer $token")
                feed = if (r.ok) r.data.orEmpty() else emptyList()
            } catch (e: Exception) {
                feed = emptyList()
                msg = ApiErrorParser.message(e)
            } finally {
                loadingFeed = false
            }
        }
    }

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

    fun cargarGrupos() {
        scope.launch {
            try {
                val token = tokenManager.getToken().first() ?: ""
                val r = RetrofitClient.comunidadApi.listarGrupos(
                    "Bearer $token",
                    if (esFitness) null else oposicionId
                )
                grupos = if (r.ok) r.data.orEmpty() else emptyList()
            } catch (e: Exception) {
                msg = ApiErrorParser.message(e)
            }
        }
    }

    LaunchedEffect(Unit) {
        cargarAmigos()
        cargarFeed()
        cargarGrupos()
    }

    // Limpiar mensaje de estado automaticamente tras 3s.
    LaunchedEffect(msg) {
        if (msg.isNotBlank()) {
            kotlinx.coroutines.delay(3000)
            msg = ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Comunidad") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSegmentos) {
                        Icon(Icons.Filled.EmojiEvents, contentDescription = "Segmentos")
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
            ScrollableTabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Actividad") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Grupos") })
                Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Cerca") })
                Tab(selected = tab == 3, onClick = { tab = 3 }, text = { Text("Amigos") })
                Tab(selected = tab == 4, onClick = { tab = 4 }, text = { Text("Buscar") })
                Tab(selected = tab == 5, onClick = { tab = 5 }, text = { Text("Chat") })
            }
            if (msg.isNotBlank()) {
                Text(msg, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp))
            }
            when (tab) {
                0 -> {
                    if (loadingFeed) {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (feed.isEmpty()) {
                        EmptyState(
                            emoji = "🏃",
                            title = "Sin publicaciones aún",
                            message = "Cuando tú o tus amigos compartáis una actividad aparecerá aquí con stats, fotos y comentarios.",
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        LazyColumn(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(feed, key = { it.idPost }) { post ->
                                PostFeedCard(
                                    post = post,
                                    onClick = { onOpenPost(post.idPost) },
                                    onLike = {
                                        scope.launch {
                                            try {
                                                val token = tokenManager.getToken().first() ?: ""
                                                val r = RetrofitClient.postsApi.toggleLike(
                                                    "Bearer $token",
                                                    post.idPost
                                                )
                                                if (r.ok) cargarFeed()
                                            } catch (_: Exception) { }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                1 -> ComunidadGruposTab(
                    grupos = grupos,
                    grupoSel = grupoSel,
                    mensajes = mensajesGrupo,
                    quedadas = quedadas,
                    oposicionId = oposicionId,
                    esFitness = esFitness,
                    onCrearGrupo = { nombre, desc ->
                        scope.launch {
                            val token = tokenManager.getToken().first() ?: ""
                            RetrofitClient.comunidadApi.crearGrupo(
                                "Bearer $token",
                                CrearGrupoRequest(nombre, desc, if (esFitness) null else oposicionId)
                            )
                            cargarGrupos()
                        }
                    },
                    onUnirse = { id ->
                        scope.launch {
                            val token = tokenManager.getToken().first() ?: ""
                            RetrofitClient.comunidadApi.unirseGrupo("Bearer $token", id)
                            cargarGrupos()
                        }
                    },
                    onSeleccionar = { g ->
                        grupoSel = g
                        scope.launch {
                            val token = tokenManager.getToken().first() ?: ""
                            mensajesGrupo = RetrofitClient.comunidadApi.mensajesGrupo("Bearer $token", g.id_grupo).data.orEmpty()
                            quedadas = RetrofitClient.comunidadApi.quedadasGrupo("Bearer $token", g.id_grupo).data.orEmpty()
                        }
                    },
                    onEnviarMensaje = { id, texto ->
                        scope.launch {
                            val token = tokenManager.getToken().first() ?: ""
                            RetrofitClient.comunidadApi.enviarMensajeGrupo(
                                "Bearer $token", id, EnviarMensajeGrupoRequest(texto)
                            )
                            mensajesGrupo = RetrofitClient.comunidadApi.mensajesGrupo("Bearer $token", id).data.orEmpty()
                        }
                    },
                    onCrearQuedada = { id, titulo, lugar ->
                        scope.launch {
                            val token = tokenManager.getToken().first() ?: ""
                            RetrofitClient.comunidadApi.crearQuedada(
                                "Bearer $token", id, CrearQuedadaRequest(titulo, lugar)
                            )
                            quedadas = RetrofitClient.comunidadApi.quedadasGrupo("Bearer $token", id).data.orEmpty()
                        }
                    }
                )
                2 -> ComunidadCercaTab(
                    usuarios = cerca,
                    visible = ubicacionVisible,
                    onToggleVisible = { v ->
                        ubicacionVisible = v
                        scope.launch {
                            val token = tokenManager.getToken().first() ?: ""
                            RetrofitClient.usuarioApi.actualizarPerfil(
                                "Bearer $token",
                                com.opofit.miapp.data.responsemodels.ActualizarPerfilRequest(
                                    userId = authState.userId ?: 0,
                                    ubicacionVisible = v
                                )
                            )
                        }
                    },
                    onBuscar = { lat, lng ->
                        scope.launch {
                            try {
                                val token = tokenManager.getToken().first() ?: ""
                                RetrofitClient.comunidadApi.actualizarUbicacion(
                                    "Bearer $token",
                                    UbicacionRequest(lat, lng, ubicacionVisible)
                                )
                                cerca = RetrofitClient.comunidadApi.listarCerca("Bearer $token", lat, lng).data.orEmpty()
                            } catch (e: Exception) {
                                msg = ApiErrorParser.message(e)
                            }
                        }
                    }
                )
                3 -> LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (pendientes.isNotEmpty()) {
                        item { Text("Solicitudes pendientes", fontWeight = FontWeight.Bold) }
                        items(pendientes) { s ->
                            ElevatedCard(Modifier.fillMaxWidth()) {
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
                    item { Text("Tus amigos", fontWeight = FontWeight.Bold) }
                    if (amigos.isEmpty()) item { Text("Busca compañeros en la pestaña Buscar.") }
                    items(amigos) { a ->
                        ElevatedCard(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    chatCon = a
                                    tab = 3
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
                4 -> Column(Modifier.padding(16.dp)) {
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
                                    val r = RetrofitClient.amigosApi.buscar(
                                        "Bearer $token",
                                        busqueda,
                                        if (esFitness) null else oposicionId
                                    )
                                    resultados = r.data.orEmpty()
                                } catch (e: Exception) {
                                    msg = ApiErrorParser.message(e)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (esFitness) "Buscar usuarios" else "Buscar en mi oposición") }
                    Spacer(Modifier.height(12.dp))
                    resultados.forEach { u ->
                        ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
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
                5 -> Column(Modifier.padding(16.dp)) {
                    if (chatCon == null) {
                        Text("Selecciona un amigo en la pestaña Amigos para chatear.")
                    } else {
                        Text("Chat con ${chatCon!!.amigo_nombre}", fontWeight = FontWeight.Bold)
                        LazyColumn(
                            modifier = Modifier.weight(1f, fill = false).heightIn(max = 320.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(mensajes) { m ->
                                val esMio = m.id_remitente == (authState.userId ?: -1)
                                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
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
                            Button(
                                enabled = textoMsg.trim().isNotEmpty(),
                                onClick = {
                                    val texto = textoMsg.trim()
                                    if (texto.isEmpty()) return@Button
                                    scope.launch {
                                        try {
                                            val token = tokenManager.getToken().first() ?: ""
                                            RetrofitClient.amigosApi.enviarMensaje(
                                                "Bearer $token",
                                                EnviarMensajeRequest(chatCon!!.amigo_id, texto)
                                            )
                                            textoMsg = ""
                                            val c = RetrofitClient.amigosApi.chat("Bearer $token", chatCon!!.amigo_id)
                                            mensajes = c.data.orEmpty()
                                        } catch (e: Exception) {
                                            msg = ApiErrorParser.message(e)
                                        }
                                    }
                                }
                            ) { Text("Enviar") }
                        }
                    }
                }
            }
        }
    }
}
