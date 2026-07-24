package com.opofit.miapp.ui.screens.premium

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opofit.miapp.data.api.RetrofitClient
import com.opofit.miapp.data.local.TokenManager
import com.opofit.miapp.ui.theme.BrandGradientEnd
import com.opofit.miapp.ui.theme.BrandGradientMid
import com.opofit.miapp.ui.theme.BrandGradientStart
import com.opofit.miapp.ui.theme.PremiumGold
import com.opofit.miapp.ui.theme.PremiumGoldDeep
import com.opofit.miapp.utils.ApiErrorParser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Paywall Premium — pantalla de conversión.
 *
 * Estructura pensada para vender (patrón Strava/Runna/Freeletics):
 *   1. Hero con gradiente de marca + badge dorado → deseo.
 *   2. Features con iconos → valor concreto.
 *   3. Comparativa Gratis vs Premium → ancla ("lo que me pierdo").
 *   4. Oferta con CTA dorado + "cancela cuando quieras" → cierre sin fricción.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val scope = rememberCoroutineScope()

    var esPremium by remember { mutableStateOf(false) }
    var premiumHasta by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var activando by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }

    fun cargarEstado() {
        scope.launch {
            loading = true
            try {
                val token = tokenManager.getToken().first() ?: ""
                val resp = RetrofitClient.premiumApi.estado("Bearer $token")
                if (resp.ok && resp.data != null) {
                    esPremium = resp.data.esPremium
                    premiumHasta = resp.data.premiumHasta
                }
            } catch (e: Exception) {
                msg = ApiErrorParser.message(e)
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) { cargarEstado() }

    val cs = MaterialTheme.colorScheme

    Box(Modifier.fillMaxSize().background(cs.background)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ============ HERO con gradiente de marca ============
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(BrandGradientStart, BrandGradientMid, BrandGradientEnd)
                        )
                    )
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 24.dp)
                        .padding(top = 8.dp, bottom = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Back arrow alineada a la izquierda sobre el gradiente
                    Row(Modifier.fillMaxWidth()) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver",
                                tint = Color.White
                            )
                        }
                    }
                    // Badge estrella dorada
                    Box(
                        Modifier
                            .size(72.dp)
                            .background(PremiumGold.copy(alpha = 0.18f), CircleShape)
                            .border(1.5.dp, PremiumGold.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint = PremiumGold,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "OpoFit Premium",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Entrena con el plan de los que aprueban",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Column(
                Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (esPremium) {
                    // ============ Estado Premium activo ============
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = cs.surface,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, PremiumGold),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Filled.CheckCircle, null, tint = PremiumGold, modifier = Modifier.size(28.dp))
                            Column {
                                Text(
                                    "Premium activo",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                premiumHasta?.let {
                                    Text(
                                        "Hasta $it",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = cs.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        "Disfruta de todas las funciones desbloqueadas: planes avanzados, IA, baremos completos, historial y ranking sin límites.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant
                    )
                } else {
                    // ============ Features ============
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        PremiumFeatureRow(
                            Icons.Filled.FitnessCenter,
                            "Planes INTERMEDIO y AVANZADO",
                            "Progresa más allá del nivel básico en todas las oposiciones"
                        )
                        PremiumFeatureRow(
                            Icons.Filled.AutoAwesome,
                            "IA que diseña tu plan",
                            "Adaptado a tu material, tu nivel y tus puntos débiles"
                        )
                        PremiumFeatureRow(
                            Icons.Filled.EmojiEvents,
                            "Baremos oficiales completos",
                            "Todas las filas de puntuación de cada prueba, no solo 4"
                        )
                        PremiumFeatureRow(
                            Icons.Filled.Timer,
                            "Historial completo de simulacros",
                            "Analiza tu evolución simulacro a simulacro"
                        )
                        PremiumFeatureRow(
                            Icons.Filled.Leaderboard,
                            "Ranking completo entre aspirantes",
                            "Compárate con todos los opositores, no solo el top"
                        )
                        PremiumFeatureRow(
                            Icons.Filled.Star,
                            "Todas las oposiciones",
                            "Bomberos, Policía Local, Penitenciarías, Ejército y más"
                        )
                    }

                    // ============ Comparativa Gratis vs Premium ============
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = cs.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(Modifier.fillMaxWidth()) {
                                Text(
                                    "Qué incluye",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = cs.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    "Gratis",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = cs.onSurfaceVariant,
                                    modifier = Modifier.width(56.dp),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    "Premium",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = PremiumGold,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(64.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                            HorizontalDivider(color = cs.outlineVariant)
                            ComparisonRow("Plan de entrenamiento", "Básico", premium = true)
                            ComparisonRow("Simulacro con nota oficial", si = true, premium = true)
                            ComparisonRow("Comunidad y GPS", si = true, premium = true)
                            ComparisonRow("Niveles intermedio y avanzado", si = false, premium = true)
                            ComparisonRow("IA que diseña tu plan", si = false, premium = true)
                            ComparisonRow("Baremos y ranking completos", si = false, premium = true)
                        }
                    }

                    // ============ Oferta + CTA ============
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = cs.surface,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, PremiumGold),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                Modifier
                                    .background(
                                        Brush.horizontalGradient(listOf(PremiumGoldDeep, PremiumGold)),
                                        RoundedCornerShape(50)
                                    )
                                    .padding(horizontal = 14.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    "PRUEBA GRATUITA",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF231300),
                                    letterSpacing = 1.2.sp
                                )
                            }
                            Text(
                                "30 días gratis",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                "Después, menos de lo que cuesta un café al mes. Tu plaza vale mucho más.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = cs.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(4.dp))
                            // CTA con gradiente de marca
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(BrandGradientMid, BrandGradientEnd)
                                        ),
                                        MaterialTheme.shapes.large
                                    )
                                    .clickable(enabled = !activando) {
                                        activando = true
                                        msg = ""
                                        scope.launch {
                                            try {
                                                val token = tokenManager.getToken().first() ?: ""
                                                val resp = RetrofitClient.premiumApi.activarPrueba("Bearer $token")
                                                msg = resp.msg ?: if (resp.ok) "¡Premium activado!" else "No disponible"
                                                if (resp.ok) cargarEstado()
                                            } catch (e: Exception) {
                                                msg = ApiErrorParser.message(e)
                                            } finally {
                                                activando = false
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (activando) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color.White,
                                        strokeWidth = 2.5.dp
                                    )
                                } else {
                                    Text(
                                        "Empezar 30 días gratis",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                            Text(
                                "Cancela cuando quieras · Sin permanencia",
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        "Las funciones gratis siguen siendo completamente funcionales: plan básico, perfil, marcas, simulacro con nota, comunidad y GPS.",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (msg.isNotBlank()) {
                    Text(
                        msg,
                        color = cs.primary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (loading) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }
}

@Composable
private fun PremiumFeatureRow(icon: ImageVector, title: String, subtitle: String) {
    val cs = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            Modifier
                .size(42.dp)
                .background(cs.primary.copy(alpha = 0.14f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = cs.primary, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant
            )
        }
    }
}

/**
 * Fila de comparativa. [si] = incluido en gratis; si `null`, se muestra [gratis] como texto.
 */
@Composable
private fun ComparisonRow(
    funcion: String,
    gratis: String? = null,
    si: Boolean? = null,
    premium: Boolean = true
) {
    val cs = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            funcion,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Box(Modifier.width(56.dp), contentAlignment = Alignment.Center) {
            when {
                gratis != null -> Text(
                    gratis,
                    style = MaterialTheme.typography.labelMedium,
                    color = cs.onSurfaceVariant
                )
                si == true -> Icon(
                    Icons.Filled.CheckCircle, null,
                    tint = cs.onSurfaceVariant, modifier = Modifier.size(18.dp)
                )
                else -> Icon(
                    Icons.Filled.Close, null,
                    tint = cs.outline, modifier = Modifier.size(18.dp)
                )
            }
        }
        Box(Modifier.width(64.dp), contentAlignment = Alignment.Center) {
            if (premium) {
                Icon(
                    Icons.Filled.CheckCircle, null,
                    tint = PremiumGold, modifier = Modifier.size(18.dp)
                )
            } else {
                Icon(Icons.Filled.Close, null, tint = cs.outline, modifier = Modifier.size(18.dp))
            }
        }
    }
}
