package com.opofit.miapp.ui.screens.oposicion

import com.opofit.miapp.ui.components.ElevatedCard
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
import com.opofit.miapp.utils.FitnessMode
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
    val esFitness = FitnessMode.isFitness(authState.modoUso)
    val oposicionId = FitnessMode.planOposicionId(authState.oposicionId, authState.modoUso)
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

    val nombreOposicion = if (esFitness) "Modo fitness" else (oposicion?.nombre ?: "Info Oposición")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(nombreOposicion, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    com.opofit.miapp.ui.components.InfoTip(
                        title = "¿Qué hay en esta pantalla?",
                        text = "Información oficial de tu oposición:\n\n" +
                            "• Pruebas físicas: qué pruebas tienes que pasar y los baremos para sacar nota.\n" +
                            "• Noticias: convocatorias, plazos y novedades del BOE/RSS oficiales.\n" +
                            "• Fuentes: enlace a la convocatoria oficial.\n\n" +
                            "Los baremos se actualizan automáticamente y las noticias se refrescan cada 6 horas. " +
                            "Si ves algo incorrecto, contacta soporte."
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
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
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
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
            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
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
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
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
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
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
            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
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

    // Auto-purga: cualquier noticia con más de 60 días se descarta silenciosamente.
    // Antes la convocatoria de enero (4-5 meses) se quedaba fija arriba con la
    // estrellita ⭐ aunque ya no era novedad. El usuario lo reportaba como
    // "están ahí fijadas y no deberían estarlo".
    val rssOrdenadas = remember(noticiasRss) {
        val ahora = System.currentTimeMillis()
        val sieteDiasMs = 60L * 24L * 60L * 60L * 1000L
        // Parser robusto: si la fecha viene en cualquier formato (ISO, YYYY-MM-DD,
        // RFC1123…) lo intentamos; si no se puede, lo dejamos pasar como reciente.
        fun parseFecha(s: String): Long {
            if (s.isBlank()) return Long.MAX_VALUE
            val formatos = listOf(
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd",
                "EEE, d MMM yyyy HH:mm:ss z"
            )
            for (f in formatos) {
                try {
                    val sdf = java.text.SimpleDateFormat(f, java.util.Locale.ENGLISH)
                    return sdf.parse(s)?.time ?: continue
                } catch (_: Exception) { /* siguiente formato */ }
            }
            return Long.MAX_VALUE
        }
        noticiasRss
            .filter {
                // Mantenemos solo las de últimos 60 días + las que no se sepa
                // la fecha (mejor mostrarlas que perderlas).
                val t = parseFecha(it.fecha)
                t == Long.MAX_VALUE || ahora - t <= sieteDiasMs
            }
            // Ordenamos PRIMERO por fecha (descendente) — así las nuevas siempre
            // arriba. Antes el sort por "relevante" hacía que noticias antiguas
            // con tu oposición se quedaran fijas indefinidamente.
            .sortedWith(
                compareByDescending<NoticiaRss> { parseFecha(it.fecha) }
                    .thenByDescending { it.urgente }
            )
    }

    fun etiquetaCategoria(cat: String): String = when (cat) {
        "convocatoria" -> "📢 Convocatoria"
        "plazo" -> "⏰ Plazo"
        "noticia" -> "📰 Noticia"
        else -> "📋 Info"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        
        item {
            Text(
                text = "📡 Noticias de tu oposición",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "BOE y fuentes oficiales filtradas por convocatoria",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (noticiasRss.isEmpty()) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
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
                // Cards UNIFORMES. Antes las marcadas como relevantes usaban
                // primaryContainer (color destacado) y las normales secondary,
                // y se veían como dos diseños distintos. Ahora todas iguales
                // y la "relevancia" se indica con un chip discreto arriba.
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (noticia.enlace.isNotBlank()) {
                                Modifier.clickable { UrlOpener.open(context, noticia.enlace) }
                            } else Modifier
                        )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (relevante) {
                            // Chip pequeño, no estrellita gigante.
                            androidx.compose.material3.AssistChip(
                                onClick = {},
                                label = {
                                    Text(
                                        "Tu oposición",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        Text(
                            text = noticia.titulo,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 3,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (noticia.fecha.isNotBlank()) {
                                Text(
                                    text = "🗓 ${noticia.fecha.take(10)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (noticia.fuente.isNotBlank()) {
                                Text(
                                    text = "· ${noticia.fuente}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        val texto = noticia.resumen.ifBlank { noticia.descripcion }
                        if (texto.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = texto,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 4,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                        // Botón "Abrir" SIEMPRE visible si hay enlace, como en
                        // las tarjetas de abajo. Antes algunas no lo mostraban.
                        if (noticia.enlace.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            TextButton(onClick = {
                                UrlOpener.open(context, noticia.enlace)
                            }) {
                                Text(
                                    text = "Abrir",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        
    }
}

@Composable
private fun FitnessInfoContent(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Modo fitness",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Usas OpoFit como app de entrenamiento sin oposición. Tienes acceso al plan semanal, rutas GPS, rutinas libres, comunidad, grupos y gente cerca.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("💪 Plan y entrenos", fontWeight = FontWeight.SemiBold)
                    Text("Plan personalizado de fuerza, resistencia y velocidad sin pruebas oficiales.")
                    Text("🏃 Rutas GPS", fontWeight = FontWeight.SemiBold)
                    Text("Registra carreras y rutas con mapa y exportación.")
                    Text("👥 Comunidad", fontWeight = FontWeight.SemiBold)
                    Text("Grupos, chat, quedadas y buscar usuarios fitness cerca de ti.")
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
        val u = baremos.firstOrNull()?.unidad
        if (u == "s" || u == "reps") return u
        val mejorSiEsMenor = baremos.firstOrNull()?.mejor_si_es_menor
        return if (mejorSiEsMenor == 1) "s" else "reps"
    }

    fun nombrePruebaConConversion(nombre: String): String =
        Units.nombreConEquivalenciaDistancia(nombre, unitDist)

    fun formatNota(nota: Double?): String {
        if (nota == null) return "-"
        val entera = nota.toLong()
        return if (nota == entera.toDouble()) entera.toString() else String.format("%.1f", nota)
    }

    fun formatMarca(valor: Double?, unidad: String): String {
        if (valor == null) return "-"
        if (unidad != "s") return valor.toInt().toString()
        val entera = valor.toLong()
        return if (valor == entera.toDouble()) entera.toString()
        else String.format("%.2f", valor).trimEnd('0').trimEnd('.')
    }

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
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth()
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

                            baremos
                                .sortedBy { it.nota ?: 0.0 }
                                .forEach { baremo ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = formatMarca(baremo.marca_valor, unidad),
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = formatNota(baremo.nota),
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
