package com.opofit.miapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.opofit.miapp.R

/**
 * Logo OpoFit — imagen de referencia oficial (escudo + O/F varsity + rayo).
 */
@Composable
fun OpoFitLogo(
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    onDarkBackground: Boolean = false
) {
    Image(
        painter = painterResource(R.drawable.ic_opofit_logo),
        contentDescription = "OpoFit",
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit
    )
}
