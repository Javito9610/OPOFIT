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
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
    SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = SplashNavy.toArgb()
        window.navigationBarColor = SplashNavy.toArgb()
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
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
