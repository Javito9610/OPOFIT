package com.opofit.miapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val OpoFitBlue = Color(0xFF0D47A1)

@Composable
fun OpoFitSplashScreen(
    showLoading: Boolean = true,
    loadingMessage: String = "Cargando OpoFit…"
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OpoFitBlue),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            OpoFitLogo(size = 176.dp, onDarkBackground = true)
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "OpoFit",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 1.sp
            )
            if (showLoading) {
                Spacer(modifier = Modifier.height(32.dp))
                CircularProgressIndicator(color = Color.White)
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = loadingMessage,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.92f)
                )
            }
        }
    }
}
