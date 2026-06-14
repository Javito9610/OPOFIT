package com.opofit.miapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opofit.miapp.ui.theme.OpoRadii
import com.opofit.miapp.ui.theme.OpoSpacing
import kotlinx.coroutines.delay

/**
 * Snackbar profesional con 4 variantes semánticas + icono + acción opcional.
 *
 * Antes la app mostraba mensajes flotantes planos del Snackbar default de M3,
 * sin distinción visual entre éxito, error y aviso. Caliber/Strava muestran
 * un chip semántico: verde para éxito, rojo para error, ámbar para warning,
 * azul info para neutro.
 *
 * Uso típico:
 *   val (msg, mostrar) = rememberProSnackbar()
 *   ProSnackbarHost(state = msg)
 *
 *   onClick = { mostrar(SnackbarType.SUCCESS, "Sesión guardada") }
 */

enum class SnackbarType { SUCCESS, ERROR, WARNING, INFO }

data class SnackbarState(
    val type: SnackbarType,
    val message: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)

@Composable
fun ProSnackbarHost(
    state: SnackbarState?,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {}
) {
    LaunchedEffect(state) {
        if (state != null && state.actionLabel == null) {
            delay(3000)
            onDismiss()
        }
    }
    Box(modifier = modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = state != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            state?.let { Pill(it) }
        }
    }
}

@Composable
private fun Pill(state: SnackbarState) {
    val cs = MaterialTheme.colorScheme
    val (icon, accent, container) = when (state.type) {
        SnackbarType.SUCCESS -> Triple(Icons.Outlined.CheckCircle, Color(0xFF1B5E20), Color(0xFFE8F5E9))
        SnackbarType.ERROR -> Triple(Icons.Outlined.ErrorOutline, cs.error, cs.errorContainer)
        SnackbarType.WARNING -> Triple(Icons.Outlined.WarningAmber, Color(0xFFE65100), Color(0xFFFFF3E0))
        SnackbarType.INFO -> Triple(Icons.Outlined.Info, cs.primary, cs.primaryContainer.copy(alpha = 0.4f))
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = OpoSpacing.lg, vertical = OpoSpacing.sm),
        shape = OpoRadii.md,
        color = container,
        tonalElevation = 4.dp,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = OpoSpacing.md, vertical = OpoSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(icon as ImageVector, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(OpoSpacing.sm))
            Text(
                state.message,
                style = MaterialTheme.typography.bodyMedium,
                color = accent,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            if (state.actionLabel != null) {
                Spacer(Modifier.width(OpoSpacing.sm))
                TextButton(onClick = { state.onAction?.invoke() }) {
                    Text(
                        state.actionLabel,
                        color = accent,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

/** Hook helper: gestiona el estado y devuelve una lambda para mostrar el snackbar. */
@Composable
fun rememberProSnackbar(): Pair<SnackbarState?, (SnackbarType, String, String?, (() -> Unit)?) -> Unit> {
    var state by remember { mutableStateOf<SnackbarState?>(null) }
    val show: (SnackbarType, String, String?, (() -> Unit)?) -> Unit = { type, msg, action, onAction ->
        state = SnackbarState(type, msg, action, onAction)
    }
    return state to show
}
