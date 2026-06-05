package com.opofit.miapp.ui.screens.social

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.opofit.miapp.data.api.RetrofitClient
import com.opofit.miapp.data.local.TokenManager
import com.opofit.miapp.data.responsemodels.CrearPostRequest
import com.opofit.miapp.data.responsemodels.EsfuerzoDesdeActividadRequest
import com.opofit.miapp.data.responsemodels.EsfuerzoSlug
import com.opofit.miapp.gps.service.ShareActivityContext
import com.opofit.miapp.gps.util.GpsMetrics
import com.opofit.miapp.utils.ImagePickerUtil
import com.opofit.miapp.utils.MediaUrlUtil
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompartirActividadScreen(
    onNavigateBack: () -> Unit,
    onPublicado: (Int) -> Unit = {}
) {
    val pending = remember { ShareActivityContext.consume() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokenManager = remember { TokenManager(context) }

    var titulo by remember { mutableStateOf(pending?.tituloSugerido.orEmpty()) }
    var texto by remember { mutableStateOf("") }
    var publico by remember { mutableStateOf(false) }
    var fotoPreview by remember { mutableStateOf<String?>(null) }
    var fotoBase64 by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    LaunchedEffect(pending) {
        if (pending == null) onNavigateBack()
    }
    if (pending == null) return

    val pickFoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val b64 = ImagePickerUtil.uriToJpegBase64(context, uri)
            if (b64 != null) {
                fotoBase64 = b64
                fotoPreview = b64
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compartir actividad") },
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
        Column(
            Modifier.fillMaxSize().padding(pad).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            pending.stats?.let { s ->
                val parts = buildList {
                    s.distanciaM?.takeIf { it > 0 }?.let { add(GpsMetrics.formatDistance(it)) }
                    s.duracionSec?.takeIf { it > 0 }?.let { add(GpsMetrics.formatDuration(it)) }
                    s.ritmoMedioSpkm?.takeIf { it > 0 }?.let { add("${GpsMetrics.formatPace(it)}/km") }
                    s.ejercicios?.takeIf { it > 0 }?.let { add("$it ejercicios") }
                    s.avgHrBpm?.let { add("♥ ${s.avgHrBpm} lpm") }
                }
                if (parts.isNotEmpty()) {
                    Text(
                        parts.joinToString("  ·  "),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            OutlinedTextField(
                value = titulo,
                onValueChange = { titulo = it },
                label = { Text("Título") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = texto,
                onValueChange = { texto = it },
                label = { Text("Comentario") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !publico, onClick = { publico = false }, label = { Text("Solo amigos") })
                FilterChip(selected = publico, onClick = { publico = true }, label = { Text("Público") })
            }
            OutlinedButton(onClick = {
                pickFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) {
                Text("Añadir foto")
            }
            fotoPreview?.let { prev ->
                val model = MediaUrlUtil.resolveAvatar(prev) ?: prev
                AsyncImage(model = model, contentDescription = null, modifier = Modifier.fillMaxWidth().height(180.dp))
            }
            if (error.isNotBlank()) {
                Text(error, color = MaterialTheme.colorScheme.error)
            }
            if (loading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        scope.launch {
                            loading = true
                            error = ""
                            try {
                                val token = tokenManager.getToken().first() ?: ""
                                val resp = RetrofitClient.postsApi.crear(
                                    "Bearer $token",
                                    CrearPostRequest(
                                        titulo = titulo.trim(),
                                        texto = texto.trim().ifBlank { null },
                                        visibilidad = if (publico) "PUBLICO" else "AMIGOS",
                                        fuente = pending.fuente,
                                        gpsUuid = pending.gpsUuid,
                                        idHistorialSesion = pending.idHistorialSesion,
                                        stats = pending.stats,
                                        imagenBase64 = fotoBase64
                                    )
                                )
                                if (resp.ok && resp.data != null) {
                                    pending.gpsUuid?.let { uuid ->
                                        if (pending.segmentSlugs.isNotEmpty()) {
                                            RetrofitClient.segmentosApi.desdeActividad(
                                                "Bearer $token",
                                                EsfuerzoDesdeActividadRequest(
                                                    gpsUuid = uuid,
                                                    esfuerzos = pending.segmentSlugs.map {
                                                        EsfuerzoSlug(it.first, it.second)
                                                    }
                                                )
                                            )
                                        }
                                    }
                                    onPublicado(resp.data.idPost)
                                } else {
                                    error = resp.msg ?: "No se pudo publicar"
                                }
                            } catch (e: Exception) {
                                error = e.message ?: "Error de conexión"
                            } finally {
                                loading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = titulo.isNotBlank()
                ) {
                    Text("Publicar en mi perfil")
                }
                OutlinedButton(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth()) {
                    Text("No publicar")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
