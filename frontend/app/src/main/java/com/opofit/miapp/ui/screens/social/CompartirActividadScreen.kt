package com.opofit.miapp.ui.screens.social

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.Switch
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
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opofit.miapp.data.api.RetrofitClient
import com.opofit.miapp.data.local.TokenManager
import com.opofit.miapp.data.responsemodels.CrearPostRequest
import com.opofit.miapp.data.responsemodels.EsfuerzoDesdeActividadRequest
import com.opofit.miapp.data.responsemodels.EsfuerzoSlug
import com.opofit.miapp.gps.service.ShareActivityContext
import com.opofit.miapp.ui.components.ShareCardPreview
import com.opofit.miapp.utils.ImagePickerUtil
import com.opofit.miapp.utils.ShareCardExport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    var fotoUri by remember { mutableStateOf<Uri?>(null) }
    var fotoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var fotoBase64 by remember { mutableStateOf<String?>(null) }
    var usarFoto by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var sharing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    LaunchedEffect(pending) {
        if (pending == null) onNavigateBack()
    }
    if (pending == null) return

    val pickFoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val bitmap = ImagePickerUtil.uriToBitmap(context, uri)
            val b64 = bitmap?.let { ImagePickerUtil.bitmapToJpegBase64(it) }
            if (bitmap != null && b64 != null) {
                fotoUri = uri
                fotoBitmap = bitmap
                fotoBase64 = b64
                usarFoto = true
                error = ""
            } else {
                error = "No se pudo cargar la foto. Prueba con otra imagen (JPG o PNG)."
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
            Text(
                "Vista previa (estilo Strava)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            ShareCardPreview(
                titulo = titulo,
                stats = pending.stats,
                fotoFondoUri = fotoUri,
                fotoFondoBitmap = null,
                usarFoto = usarFoto,
                routePoints = pending.routePoints,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Foto de fondo")
                Switch(checked = usarFoto, onCheckedChange = { usarFoto = it }, enabled = fotoUri != null)
            }
            OutlinedButton(
                onClick = {
                    pickFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (fotoUri == null) "Elegir foto de fondo" else "Cambiar foto")
            }

            OutlinedTextField(
                value = titulo,
                onValueChange = { titulo = it },
                label = { Text("Título en la tarjeta") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = texto,
                onValueChange = { texto = it },
                label = { Text("Comentario (solo perfil OpoFit)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !publico, onClick = { publico = false }, label = { Text("Solo amigos") })
                FilterChip(selected = publico, onClick = { publico = true }, label = { Text("Público") })
            }

            Text(
                "Compartir en redes",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            if (sharing) {
                CircularProgressIndicator(Modifier.size(28.dp))
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        scope.launch {
                            sharing = true
                            try {
                                val bitmap = withContext(Dispatchers.Default) {
                                    ShareCardExport.renderBitmap(context) {
                                        ShareCardPreview(
                                            titulo = titulo,
                                            stats = pending.stats,
                                            fotoFondoUri = fotoUri,
                                            fotoFondoBitmap = fotoBitmap,
                                            usarFoto = usarFoto,
                                            routePoints = pending.routePoints,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                                withContext(Dispatchers.Main) {
                                    if (!ShareCardExport.shareInstagramStory(context, bitmap)) {
                                        ShareCardExport.shareGeneric(context, bitmap, titulo)
                                    }
                                }
                            } catch (e: Exception) {
                                error = e.message ?: "Error al compartir"
                            } finally {
                                sharing = false
                            }
                        }
                    }) { Text("Instagram Story") }

                    OutlinedButton(onClick = {
                        scope.launch {
                            sharing = true
                            try {
                                val bitmap = withContext(Dispatchers.Default) {
                                    ShareCardExport.renderBitmap(context) {
                                        ShareCardPreview(
                                            titulo = titulo,
                                            stats = pending.stats,
                                            fotoFondoUri = fotoUri,
                                            fotoFondoBitmap = fotoBitmap,
                                            usarFoto = usarFoto,
                                            routePoints = pending.routePoints,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                                withContext(Dispatchers.Main) {
                                    if (!ShareCardExport.shareWhatsApp(context, bitmap, titulo)) {
                                        ShareCardExport.shareGeneric(context, bitmap, titulo)
                                    }
                                }
                            } catch (e: Exception) {
                                error = e.message ?: "Error al compartir"
                            } finally {
                                sharing = false
                            }
                        }
                    }) { Text("WhatsApp") }

                    Button(onClick = {
                        scope.launch {
                            sharing = true
                            try {
                                val bitmap = withContext(Dispatchers.Default) {
                                    ShareCardExport.renderBitmap(context) {
                                        ShareCardPreview(
                                            titulo = titulo,
                                            stats = pending.stats,
                                            fotoFondoUri = fotoUri,
                                            fotoFondoBitmap = fotoBitmap,
                                            usarFoto = usarFoto,
                                            routePoints = pending.routePoints,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                                withContext(Dispatchers.Main) {
                                    ShareCardExport.shareGeneric(context, bitmap, titulo)
                                }
                            } catch (e: Exception) {
                                error = e.message ?: "Error al compartir"
                            } finally {
                                sharing = false
                            }
                        }
                    }) {
                        Icon(Icons.Filled.Share, null, Modifier.size(18.dp))
                        Text("  Más apps (Snapchat, Facebook…)")
                    }
                }
            }

            if (error.isNotBlank()) {
                Text(error, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(4.dp))
            Text(
                "También en OpoFit",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
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
                    Text("Publicar en mi perfil OpoFit")
                }
                OutlinedButton(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth()) {
                    Text("Cerrar sin publicar")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
