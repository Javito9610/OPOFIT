package com.opofit.miapp.ui.screens.comunidad

import com.opofit.miapp.ui.components.ElevatedCard
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Search
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

    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
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
                Tab(
                    selected = tab == 3,
                    onClick = { tab = 3 },
                    text = {
                        if (pendientes.isNotEmpty()) {
                            BadgedBox(
                                badge = { Badge { Text("${pendientes.size}") } }
                            ) {
                                Text("Amigos")
                            }
                        } else {
                            Text("Amigos")
                        }
                    }
                )
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
                            title = "Sin publicaciones aún",
                            message = "Cuando tú o tus amigos compartáis una actividad aparecerá aquí con stats, fotos y comentarios.",
                            modifier = Modifier.fillMaxWidth(),
                            icon = androidx.compose.material.icons.Icons.Outlined.Forum
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
                    amigos = amigos,  // ← para el bottom sheet de "Invitar amigos"
                    oposicionId = oposicionId,
                    esFitness = esFitness,
                    onCrearGrupo = { nombre, desc, tipo ->
                        scope.launch {
                            // try/catch global: antes una respuesta inesperada del
                            // backend o un id=0 hacía que la excepción saltara fuera
                            // del scope y la app se cerraba. Ahora capturamos y
                            // mostramos el error al usuario.
                            try {
                                val token = tokenManager.getToken().first() ?: ""
                                val r = RetrofitClient.comunidadApi.crearGrupo(
                                    "Bearer $token",
                                    CrearGrupoRequest(
                                        nombre = nombre,
                                        descripcion = desc,
                                        idOposicion = if (esFitness) null else oposicionId,
                                        tipo = tipo
                                    )
                                )
                                if (!r.ok) msg = r.msg ?: "No se pudo crear el grupo"
                                cargarGrupos()
                            } catch (e: Exception) {
                                msg = ApiErrorParser.message(e)
                            }
                        }
                    },
                    onUnirse = { id ->
                        scope.launch {
                            try {
                                val token = tokenManager.getToken().first() ?: ""
                                val r = RetrofitClient.comunidadApi.unirseGrupo("Bearer $token", id)
                                if (!r.ok) msg = r.msg ?: "No se pudo unir"
                                cargarGrupos()
                            } catch (e: Exception) {
                                msg = ApiErrorParser.message(e)
                            }
                        }
                    },
                    onSalir = { id ->
                        scope.launch {
                            try {
                                val token = tokenManager.getToken().first() ?: ""
                                val r = RetrofitClient.comunidadApi.salirGrupo("Bearer $token", id)
                                if (!r.ok) msg = r.msg ?: "No se pudo salir"
                                if (grupoSel?.id_grupo == id) grupoSel = null
                                cargarGrupos()
                            } catch (e: Exception) {
                                msg = ApiErrorParser.message(e)
                            }
                        }
                    },
                    onEliminar = { id ->
                        scope.launch {
                            try {
                                val token = tokenManager.getToken().first() ?: ""
                                val r = RetrofitClient.comunidadApi.eliminarGrupo("Bearer $token", id)
                                if (!r.ok) msg = r.msg ?: "No se pudo eliminar"
                                if (grupoSel?.id_grupo == id) grupoSel = null
                                cargarGrupos()
                            } catch (e: Exception) {
                                msg = ApiErrorParser.message(e)
                            }
                        }
                    },
                    onSeleccionar = { g ->
                        grupoSel = g
                        scope.launch {
                            try {
                                val token = tokenManager.getToken().first() ?: ""
                                mensajesGrupo = RetrofitClient.comunidadApi.mensajesGrupo("Bearer $token", g.id_grupo).data.orEmpty()
                                quedadas = RetrofitClient.comunidadApi.quedadasGrupo("Bearer $token", g.id_grupo).data.orEmpty()
                            } catch (e: Exception) {
                                msg = ApiErrorParser.message(e)
                            }
                        }
                    },
                    onEnviarMensaje = { id, texto ->
                        scope.launch {
                            try {
                                val token = tokenManager.getToken().first() ?: ""
                                RetrofitClient.comunidadApi.enviarMensajeGrupo(
                                    "Bearer $token", id, EnviarMensajeGrupoRequest(texto)
                                )
                                mensajesGrupo = RetrofitClient.comunidadApi.mensajesGrupo("Bearer $token", id).data.orEmpty()
                            } catch (e: Exception) {
                                msg = ApiErrorParser.message(e)
                            }
                        }
                    },
                    onCrearQuedada = { id, titulo, lugar ->
                        scope.launch {
                            try {
                                val token = tokenManager.getToken().first() ?: ""
                                RetrofitClient.comunidadApi.crearQuedada(
                                    "Bearer $token", id, CrearQuedadaRequest(titulo, lugar)
                                )
                                quedadas = RetrofitClient.comunidadApi.quedadasGrupo("Bearer $token", id).data.orEmpty()
                            } catch (e: Exception) {
                                msg = ApiErrorParser.message(e)
                            }
                        }
                    },
                    onInvitar = { idGrupo, idAmigo ->
                        scope.launch {
                            try {
                                val token = tokenManager.getToken().first() ?: ""
                                val r = RetrofitClient.comunidadApi.invitarAmigoAGrupo(
                                    "Bearer $token", idGrupo,
                                    com.opofit.miapp.data.responsemodels.InvitarAmigoRequest(idAmigo)
                                )
                                if (!r.ok) msg = r.msg ?: "No se pudo invitar"
                                else msg = "Invitación enviada"
                                cargarGrupos()
                            } catch (e: Exception) {
                                msg = ApiErrorParser.message(e)
                            }
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
                                    // Bug previo: tab = 3 dejaba al usuario en la pestaña
                                    // "Amigos" en vez de saltar a la pestaña Chat (5).
                                    tab = 5
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
                    // Input universal: el usuario puede escribir nombre o email.
                    // Detectamos email por la presencia de "@" y mostramos un
                    // hint visual + cambio de label.
                    val esEmail = busqueda.contains("@")
                    OutlinedTextField(
                        value = busqueda,
                        onValueChange = { busqueda = it },
                        label = { Text(if (esEmail) "Buscar por email" else "Buscar por nombre o email") },
                        supportingText = {
                            Text(
                                if (esEmail) "Buscando coincidencia exacta de email"
                                else "Escribe parte del nombre o pega un email completo",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        leadingIcon = {
                            androidx.compose.material3.Icon(
                                if (esEmail) androidx.compose.material.icons.Icons.Filled.Email
                                else androidx.compose.material.icons.Icons.Filled.Search,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = if (esEmail) androidx.compose.ui.text.input.KeyboardType.Email
                                           else androidx.compose.ui.text.input.KeyboardType.Text
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    val token = tokenManager.getToken().first() ?: ""
                                    // Mandamos por `q` (param nuevo). El backend detecta
                                    // automáticamente si es email y prioriza match exacto.
                                    val r = RetrofitClient.amigosApi.buscar(
                                        token = "Bearer $token",
                                        consulta = busqueda,
                                        nombre = null,
                                        idOposicion = if (esFitness) null else oposicionId
                                    )
                                    resultados = r.data.orEmpty()
                                } catch (e: Exception) {
                                    msg = ApiErrorParser.message(e)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = busqueda.isNotBlank()
                    ) { Text("Buscar") }
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
    com.opofit.miapp.ui.components.CoachMarkOverlay(
        screenKey = "comunidad_v1",
        steps = listOf(
            com.opofit.miapp.ui.components.CoachStep(
                title = "Tu comunidad opositora",
                text = "Aquí ves actividades de tus amigos, grupos por oposición, gente cerca de ti y chat directo."
            ),
            com.opofit.miapp.ui.components.CoachStep(
                title = "Pestañas: Actividad, Grupos, Cerca, Amigos, Buscar, Chat",
                text = "Cada pestaña tiene su función. La pestaña «Amigos» muestra un badge naranja si tienes solicitudes pendientes."
            ),
            com.opofit.miapp.ui.components.CoachStep(
                title = "Privacidad",
                text = "Solo te ven los amigos que aceptas. Puedes hacer posts visibles solo para amigos o públicos para tu oposición."
            )
        )
    )
    }
}
