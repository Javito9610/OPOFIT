package com.opofit.miapp.ui.screens.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opofit.miapp.data.api.RetrofitClient
import com.opofit.miapp.data.local.TokenManager
import com.opofit.miapp.data.responsemodels.ActividadPost
import com.opofit.miapp.data.responsemodels.ComentarPostRequest
import com.opofit.miapp.ui.components.PostFeedCard
import com.opofit.miapp.ui.components.ProfileAvatar
import com.opofit.miapp.utils.DateFormatUtil
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetalleScreen(
    postId: Int,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokenManager = remember { TokenManager(context) }
    var post by remember { mutableStateOf<ActividadPost?>(null) }
    var loading by remember { mutableStateOf(true) }
    var textoComentario by remember { mutableStateOf("") }
    var enviando by remember { mutableStateOf(false) }

    fun cargar() {
        scope.launch {
            loading = true
            try {
                val token = tokenManager.getToken().first() ?: ""
                val r = RetrofitClient.postsApi.detalle("Bearer $token", postId)
                if (r.ok) post = r.data
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(postId) { cargar() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Publicación") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { pad ->
        when {
            loading -> Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            post == null -> Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Text("No se encontró la publicación")
            }
            else -> {
                val p = post!!
                LazyColumn(
                    Modifier.fillMaxSize().padding(pad).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        PostFeedCard(
                            post = p,
                            onLike = {
                                scope.launch {
                                    val token = tokenManager.getToken().first() ?: ""
                                    val r = RetrofitClient.postsApi.toggleLike("Bearer $token", postId)
                                    if (r.ok) {
                                        post = p.copy(
                                            yoDiLike = r.data?.liked == true,
                                            likes = p.likes + if (r.data?.liked == true) 1 else -1
                                        )
                                    }
                                }
                            }
                        )
                    }
                    item {
                        Text("Comentarios", fontWeight = FontWeight.SemiBold)
                    }
                    val comentarios = p.comentariosLista.orEmpty()
                    if (comentarios.isEmpty()) {
                        item {
                            Text(
                                "Sé el primero en comentar",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    } else {
                        items(comentarios) { c ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                ProfileAvatar(c.usuarioNombre ?: "?", sizeDp = 36, avatarUrl = c.avatarUrl)
                                Column {
                                    Text(c.usuarioNombre ?: "Usuario", fontWeight = FontWeight.Medium)
                                    Text(c.texto)
                                    c.creadoEn?.let {
                                        Text(
                                            DateFormatUtil.formatearFechaHora(it),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = textoComentario,
                                onValueChange = { textoComentario = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Escribe un comentario…") },
                                singleLine = true
                            )
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        enviando = true
                                        try {
                                            val token = tokenManager.getToken().first() ?: ""
                                            RetrofitClient.postsApi.comentar(
                                                "Bearer $token",
                                                postId,
                                                ComentarPostRequest(textoComentario.trim())
                                            )
                                            textoComentario = ""
                                            cargar()
                                        } finally {
                                            enviando = false
                                        }
                                    }
                                },
                                enabled = textoComentario.isNotBlank() && !enviando
                            ) {
                                Text("Enviar")
                            }
                        }
                    }
                }
            }
        }
    }
}
