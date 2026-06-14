package com.opofit.miapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opofit.miapp.ui.theme.OpoRadii
import com.opofit.miapp.ui.theme.OpoSizes

/**
 * Familia de botones profesionales OpoFit.
 *
 * Inspirado en Caliber, Hevy y Apple Fitness+: alturas consistentes (52dp),
 * tipografía semibold, esquinas pro (14dp), estados de carga inline con
 * `CircularProgressIndicator` (no spinner sobre el texto — el botón cambia
 * su contenido), icono leading opcional y disabled coherente.
 *
 * Usar PrimaryButton para la acción ÚNICA principal de la pantalla.
 * SecondaryButton para acciones alternativas. GhostButton para acciones de
 * baja prioridad. DangerButton para acciones destructivas.
 */

@Composable
private fun ButtonContent(
    text: String,
    leadingIcon: ImageVector?,
    loading: Boolean,
    contentColor: Color
) {
    if (loading) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = contentColor,
            strokeWidth = 2.dp
        )
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            leadingIcon?.let {
                Icon(it, contentDescription = null, modifier = Modifier.size(OpoSizes.iconMd))
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
    fillMaxWidth: Boolean = true,
    haptic: Boolean = true
) {
    // Haptic feedback automático en el CTA principal: el usuario "siente"
    // que el botón respondió, no solo lo ve. Apple Watch / Strava style.
    val triggerHaptic = if (haptic) com.opofit.miapp.utils.rememberHaptics() else null
    Button(
        onClick = {
            triggerHaptic?.invoke(com.opofit.miapp.utils.Haptics.Type.MEDIUM)
            onClick()
        },
        enabled = enabled && !loading,
        modifier = (if (fillMaxWidth) modifier.fillMaxWidth() else modifier).height(OpoSizes.buttonHeight),
        shape = OpoRadii.md,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        contentPadding = PaddingValues(horizontal = 20.dp)
    ) {
        ButtonContent(text, leadingIcon, loading, MaterialTheme.colorScheme.onPrimary)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
    fillMaxWidth: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = (if (fillMaxWidth) modifier.fillMaxWidth() else modifier).height(OpoSizes.buttonHeight),
        shape = OpoRadii.md,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        contentPadding = PaddingValues(horizontal = 20.dp)
    ) {
        ButtonContent(text, leadingIcon, loading, MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun TertiaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
    fillMaxWidth: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = (if (fillMaxWidth) modifier.fillMaxWidth() else modifier).height(OpoSizes.buttonHeight),
        shape = OpoRadii.md,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        contentPadding = PaddingValues(horizontal = 20.dp)
    ) {
        ButtonContent(text, leadingIcon, loading, MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(OpoSizes.buttonHeightSmall),
        shape = OpoRadii.md
    ) {
        ButtonContent(text, leadingIcon, false, MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun DangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
    fillMaxWidth: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = (if (fillMaxWidth) modifier.fillMaxWidth() else modifier).height(OpoSizes.buttonHeight),
        shape = OpoRadii.md,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
        ),
        contentPadding = PaddingValues(horizontal = 20.dp)
    ) {
        ButtonContent(text, leadingIcon, loading, MaterialTheme.colorScheme.onError)
    }
}
