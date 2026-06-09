package com.opofit.miapp.ui.components

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

/**
 * Texto para usar dentro de un `Button` / `OutlinedButton` / `FilledTonalButton`.
 *
 * El motivo: en pantallas pequeñas (≤360 dp) los textos largos como
 * "Publicar en mi perfil OpoFit" o "Cambiar entorno (gym, casa, calistenia…)"
 * se partían en dos líneas, dejando la fila inferior con una sola palabra y
 * pinta poco profesional.
 *
 * Defaults seguros:
 *   - `maxLines = 1` y `softWrap = false` evitan saltos.
 *   - `overflow = Ellipsis` añade "…" si pese a todo no cabe.
 *   - `basicMarquee()` opcional para textos largos que el usuario quiera leer
 *     completos (lo activa el caller con `marqueeIfTruncated = true`).
 *
 * Si el texto cabe (la mayoría de casos), se ve igual que antes. Si NO cabe,
 * el botón mantiene su altura y no rompe el layout.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ButtonText(
    text: String,
    modifier: Modifier = Modifier,
    bold: Boolean = false,
    marqueeIfTruncated: Boolean = false,
    style: TextStyle = LocalTextStyle.current
) {
    val effectiveModifier = if (marqueeIfTruncated) {
        modifier.basicMarquee()
    } else modifier
    Text(
        text = text,
        modifier = effectiveModifier,
        style = style,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        fontWeight = if (bold) FontWeight.SemiBold else null
    )
}
