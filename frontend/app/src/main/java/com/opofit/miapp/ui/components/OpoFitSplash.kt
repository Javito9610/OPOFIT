package com.opofit.miapp.ui.components

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.opofit.miapp.ui.theme.AccentOrange

val SplashNavy = Color(0xFF1B2A4A)

@Composable
fun OpoFitSplashScreen() {
    val view = LocalView.current
    // Forzamos iconos claros (sobre navy) mientras dura el splash, y los
    // restauramos al modo claro normal cuando se desmonta. Sin esto, la app
    // se quedaba con la barra de estado/gestos con apariencia de splash
    // después de terminar el arranque.
    DisposableEffect(Unit) {
        val window = (view.context as Activity).window
        val controller = WindowCompat.getInsetsController(window, view)
        val prevLightStatus = controller.isAppearanceLightStatusBars
        val prevLightNav = controller.isAppearanceLightNavigationBars
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
        onDispose {
            controller.isAppearanceLightStatusBars = prevLightStatus
            controller.isAppearanceLightNavigationBars = prevLightNav
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SplashNavy),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            OpoFitLogo(size = 120.dp, onDarkBackground = true)
            Spacer(modifier = Modifier.height(28.dp))
            CircularProgressIndicator(
                color = AccentOrange,
                strokeWidth = 2.5.dp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "OpoFit",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 2.sp
            )
        }
    }
}
