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
import com.opofit.miapp.ui.theme.AccentOrange

private val SplashNavy = Color(0xFF1B2A4A)



@Composable

fun OpoFitSplashScreen(

    showLoading: Boolean = true,

    loadingMessage: String = "Cargando OpoFit…"

) {

    Box(

        modifier = Modifier

            .fillMaxSize()

            .background(SplashNavy),

        contentAlignment = Alignment.Center

    ) {

        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            OpoFitLogo(size = 140.dp, onDarkBackground = true)

            Spacer(modifier = Modifier.height(24.dp))

            Text(

                text = "OpoFit",

                fontSize = 34.sp,

                fontWeight = FontWeight.Bold,

                color = Color.White,

                letterSpacing = 2.sp

            )

            if (showLoading) {

                Spacer(modifier = Modifier.height(36.dp))

                CircularProgressIndicator(
                    color = AccentOrange,
                    strokeWidth = 2.5.dp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(

                    text = loadingMessage,

                    fontSize = 14.sp,

                    fontWeight = FontWeight.Medium,

                    color = Color.White.copy(alpha = 0.75f)

                )

            }

        }

    }

}

