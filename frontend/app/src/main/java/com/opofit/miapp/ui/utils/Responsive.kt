package com.opofit.miapp.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/** Pantallas estrechas / móviles antiguos (~< 400dp). */
@Composable
fun isCompactScreen(): Boolean =
    LocalConfiguration.current.screenWidthDp < 400

@Composable
fun isVeryCompactScreen(): Boolean =
    LocalConfiguration.current.screenWidthDp < 360
