package com.opofit.miapp.ui.screens.oposicion

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.opofit.miapp.data.api.RetrofitClient
import com.opofit.miapp.data.local.TokenManager
import com.opofit.miapp.data.responsemodels.InfoPrueba
import com.opofit.miapp.data.responsemodels.NoticiaOposicion
import com.opofit.miapp.data.responsemodels.NoticiaRss
import com.opofit.miapp.data.responsemodels.Oposicion
import com.opofit.miapp.data.responsemodels.PruebaOposicion
import kotlinx.coroutines.flow.first
import com.opofit.miapp.ui.viewmodels.AuthViewModel
import com.opofit.miapp.utils.UrlOpener
import com.opofit.miapp.utils.Units
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OposicionInfoScreen(
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onNavigatePremium: () -> Unit = {}
) {
    val authState by authViewModel.uiState.collectAsState()
    val oposicionId = authState.oposicionId ?: 1
    val genero = authState.genero ?: "HOMBRE"

    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }

    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var oposicion by remember { mutableStateOf<Oposicion?>(null) }
    var pruebas by remember { mutableStateOf<List<PruebaOposicion>>(emptyList()) }
    var noticias by remember { mutableStateOf<List<NoticiaOposicion>>(emptyList()) }
    var noticiasRss by remember { mutableStateOf<List<NoticiaRss>>(emptyList()) }
    var infoPruebas by remember { mutableStateOf<List<InfoPrueba>>(emptyList()) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs = listOf("📋 Info", "💡 Trucos", "📰 Noticias", "📊 Baremos")

    LaunchedEffect(oposicionId) {
        isLoading = true
        error = ""
        try {
            val token = tokenManager.getToken().first() ?: ""
            val bearerToken = "Bearer $token"

            
            try {
                val detalleResponse = RetrofitClient.oposicionesApi.getInfoOposicion(bearerToken, oposicionId)
                if (detalleResponse.ok) {
                    oposicion = detalleResponse.oposicion
                    pruebas = detalleResponse.pruebas ?: emptyList()
                    noticias = detalleResponse.noticias ?: emptyList()
                }
            } catch (e: Exception) {
                error = "Error al cargar pruebas: ${e.message ?: "Error de conexión"}"
            }

            
            try {
                val infoResponse = RetrofitClient.infoPruebasApi.getInfoPruebas(bearerToken, oposicionId, genero)
                if (infoResponse.ok) {
                    infoPruebas = infoResponse.data ?: emptyList()
                }
            } catch (e: Exception) {
                if (error.isEmpty()) {
                    error = "Error al cargar baremos: ${e.message ?: "Error de conexión"}"
                }
            }

            
            try {
                val rssResponse = RetrofitClient.oposicionesApi.getNoticiasRss(bearerToken, oposicionId)
                if (rssResponse.ok) {
                    noticiasRss = rssResponse.data ?: emptyList()
                }
            } catch (_: Exception) {
                
            }

            isLoading = false
        } catch (e: Exception) {
            error = e.message ?: "Error al cargar información"
            isLoading = false
        }
    }

    val nombreOposicion = oposicion?.nombre ?: "Info Oposición"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(nombreOposicion, maxLines = 1) },
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                error.isNotEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                else -> {
                    val avisoConvocatoria = oposicion?.convocatoria_ref?.takeIf { it.isNotBlank() }
                        ?: oposicion?.notas_usuario?.takeIf { it.isNotBlank() }
                    if (avisoConvocatoria != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Text(
                                avisoConvocatoria,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        edgePadding = 8.dp
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { Text(title) }
                            )
                        }
                    }

                    when (selectedTab) {
                        0 -> InfoTab(nombreOposicion, oposicionId, pruebas)
                        1 -> TrucosTab(pruebas, infoPruebas)
                        2 -> NoticiasTab(noticias, noticiasRss, context)
                        3 -> BaremosTab(infoPruebas, genero)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoTab(
    nombreOposicion: String,
    oposicionId: Int,
    pruebas: List<PruebaOposicion>
) {
    val descripcion = if (oposicionId == 1) {
        "La oposición a la Escala Básica de la Policía Nacional es una de las más demandadas en España. " +
                "Los aspirantes deben superar pruebas físicas, teóricas y psicotécnicas. " +
                "Las pruebas físicas incluyen circuito de agilidad, dominadas (hombres) o suspensión en barra (mujeres) " +
                "y carrera de 1.000 metros. Es necesario obtener una calificación mínima en cada prueba para no ser eliminado."
    } else {
        "La oposición de acceso libre a la Guardia Civil ofrece plazas para incorporarse al cuerpo como Guardia Civil. " +
                "Las pruebas físicas son exigentes e incluyen carrera de 2.000 metros, circuito de agilidad, " +
                "flexiones de brazos y natación de 50 metros. Todas las pruebas son eliminatorias: " +
                "no alcanzar la marca mínima supone la exclusión del proceso selectivo."
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🏛️ Sobre la oposición",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = descripcion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        
        if (pruebas.isNotEmpty()) {
            item {
                Text(
                    text = "📋 Pruebas Físicas Oficiales",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(pruebas) { prueba ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = prueba.nombre_prueba,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (!prueba.descripcion.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = prueba.descripcion,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        
        if (pruebas.isEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("ℹ️", style = MaterialTheme.typography.displayMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No hay información disponible para tu oposición.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TrucosTab(
    pruebas: List<PruebaOposicion>,
    infoPruebas: List<InfoPrueba>
) {
    
    val trucosFromPruebas = pruebas
        .filter { !it.trucos.isNullOrBlank() }
        .map { it.nombre_prueba to it.trucos!! }

    
    val trucosFromInfo = infoPruebas
        .filter { !it.trucos.isNullOrBlank() }
        .distinctBy { it.nombre_prueba }
        .map { it.nombre_prueba to it.trucos!! }

    
    val allTrucos = mutableMapOf<String, String>()
    trucosFromInfo.forEach { (nombre, truco) -> allTrucos[nombre] = truco }
    trucosFromPruebas.forEach { (nombre, truco) -> allTrucos[nombre] = truco }

    
    val consejosGenerales = listOf(
        "🏃 Calentamiento" to "Dedica al menos 10-15 minutos a calentar antes de cada entrenamiento y especialmente el día de la prueba. Incluye carrera suave, movilidad articular y estiramientos dinámicos.",
        "🍎 Nutrición" to "Mantén una dieta equilibrada rica en proteínas, carbohidratos complejos y grasas saludables. Hidrátate correctamente: al menos 2 litros de agua al día.",
        "😴 Descanso" to "Duerme entre 7-9 horas diarias. El descanso es fundamental para la recuperación muscular y el rendimiento. Evita entrenar los mismos grupos musculares en días consecutivos.",
        "📅 Planificación" to "Organiza tus entrenamientos alternando entre fuerza, resistencia y velocidad. Incluye al menos un día de descanso activo a la semana.",
        "🧠 Mentalidad" to "Visualiza las pruebas antes del día del examen. Practica con las condiciones reales (pista, circuito, piscina). La confianza se construye con la preparación."
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        
        if (allTrucos.isNotEmpty()) {
            item {
                Text(
                    text = "💡 Trucos por Prueba",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(allTrucos.toList()) { (nombrePrueba, truco) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🎯 $nombrePrueba",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = truco,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }

        
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "🏋️ Consejos Generales para las Físicas",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        items(consejosGenerales) { (titulo, consejo) ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = titulo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = consejo,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun NoticiasTab(
    noticias: List<NoticiaOposicion>,
    noticiasRss: List<NoticiaRss>,
    context: android.content.Context
) {
    fun esRelevanteParaOposicion(item: NoticiaRss): Boolean {
        val text = ("${item.titulo} ${item.descripcion}").lowercase()
        
        
        return text.contains("guardia civil") ||
            text.contains("dirección general de la guardia civil") ||
            text.contains("policía") ||
            text.contains("policia") ||
            text.contains("dirección general de la policía") ||
            text.contains("cuerpo nacional de policía") ||
            text.contains("ministerio del interior")
    }

    val rssOrdenadas = remember(noticiasRss) {
        noticiasRss.sortedWith(
            compareByDescending<NoticiaRss> { esRelevanteParaOposicion(it) }
                .thenByDescending { it.fecha }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        
        item {
            Text(
                text = "📡 Noticias RSS",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Fuentes oficiales actualizadas automáticamente",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (noticiasRss.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "No se pudieron cargar noticias RSS ahora mismo.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(onClick = {
                            UrlOpener.open(context, "https://www.boe.es/rss/")
                        }) {
                            Text("Abrir fuentes oficiales")
                        }
                    }
                }
            }
        } else {
            items(rssOrdenadas) { noticia ->
                val relevante = esRelevanteParaOposicion(noticia)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (noticia.enlace.isNotBlank()) {
                                Modifier.clickable { UrlOpener.open(context, noticia.enlace) }
                            } else Modifier
                        ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (relevante) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (relevante) {
                            Text(
                                text = "⭐ Relevante para tu oposición",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        Text(
                            text = noticia.titulo,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (relevante) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        if (noticia.fecha.isNotBlank()) {
                            Text(
                                text = "🗓 ${noticia.fecha.take(10)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (relevante) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        if (noticia.fuente.isNotBlank()) {
                            Text(
                                text = "📌 ${noticia.fuente}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (relevante) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        if (noticia.descripcion.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = noticia.descripcion,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (relevante) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 3
                            )
                        }
                        if (noticia.enlace.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            TextButton(onClick = {
                                UrlOpener.open(context, noticia.enlace)
                            }) {
                                Text(
                                    text = "Abrir",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        
        if (noticiasRss.isEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("📰", style = MaterialTheme.typography.displayMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No hay noticias disponibles en este momento.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun BaremosTab(
    infoPruebas: List<InfoPrueba>,
    genero: String
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    var unitDist by remember { mutableStateOf("km") }
    LaunchedEffect(Unit) {
        tokenManager.getUnitDistancia().collectLatest { u ->
            if (!u.isNullOrBlank()) unitDist = u
        }
    }

    fun unidadParaPrueba(baremos: List<InfoPrueba>): String {
        
        val mejorSiEsMenor = baremos.firstOrNull()?.mejor_si_es_menor
        return if (mejorSiEsMenor == 1) "s" else "reps"
    }

    fun nombrePruebaConConversion(nombre: String): String =
        Units.nombreConEquivalenciaDistancia(nombre, unitDist)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (infoPruebas.isNotEmpty()) {
            item {
                Text(
                    text = "📊 Baremos de Puntuación",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Género: $genero",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            
            val grouped = infoPruebas.groupBy { it.nombre_prueba }
            grouped.forEach { (nombrePrueba, baremos) ->
                item {
                    val unidad = unidadParaPrueba(baremos)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = nombrePruebaConConversion(nombrePrueba),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))

                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (unidad == "s") "Marca (segundos)" else "Marca (repeticiones)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "Nota",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            baremos.forEach { baremo ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (unidad == "s") {
                                            
                                            "${baremo.marca_valor ?: "-"}"
                                        } else {
                                            
                                            baremo.marca_valor?.toInt()?.toString() ?: "-"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = baremo.nota?.let { String.format("%.1f", it) } ?: "-",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("📊", style = MaterialTheme.typography.displayMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No hay baremos disponibles.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
