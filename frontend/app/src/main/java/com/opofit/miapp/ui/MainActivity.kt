package com.opofit.miapp.ui

import android.Manifest
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.opofit.miapp.integraciones.GoogleFitManager
import com.opofit.miapp.ui.components.OpoFitSplashScreen
import androidx.navigation.compose.rememberNavController
import com.opofit.miapp.data.local.TokenManager
import com.opofit.miapp.ui.navigation.AppNavigation
import com.opofit.miapp.ui.theme.MiAppTheme
import com.opofit.miapp.ui.viewmodels.AuthViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private var onGoogleFitPermissionsResult: ((Boolean) -> Unit)? = null
    private var keepSystemSplash = true

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition { keepSystemSplash }
        super.onCreate(savedInstanceState)

        // Sistema 100% transparente sin scrim. Si no se pasa SystemBarStyle
        // explícito, enableEdgeToEdge() añade un velo blanco translúcido sobre
        // la barra de gestos cuando el contenido es claro (la "franja blanca"
        // reportada). Forzamos transparente puro y dejamos que la app dibuje
        // edge-to-edge sin retoques del OS.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                AndroidColor.TRANSPARENT,
                AndroidColor.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                AndroidColor.TRANSPARENT,
                AndroidColor.TRANSPARENT
            )
        )
        setContent {
            val darkMode = TokenManager(this).getDarkMode().collectAsState(initial = false).value
            MiAppTheme(darkTheme = darkMode) {
                val uiState by authViewModel.uiState.collectAsState()
                var minSplashDone by remember { mutableStateOf(false) }

                SideEffect {
                    keepSystemSplash = false
                }

                LaunchedEffect(Unit) {
                    delay(1600)
                    minSplashDone = true
                }

                val showSplash = !uiState.isSessionChecked || !minSplashDone

                LaunchedEffect(uiState.isLoggedIn) {
                    if (uiState.isLoggedIn && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                if (showSplash) {
                    OpoFitSplashScreen()
                } else {
                    val navController = rememberNavController()
                    AppNavigation(
                        navController = navController,
                        isLoggedIn = uiState.isLoggedIn,
                        authViewModel = authViewModel
                    )
                }
            }
        }
    }

    fun requestGoogleFitPermissions(onResult: (Boolean) -> Unit) {
        val gf = GoogleFitManager.get(this)
        if (gf.hasPermissions()) {
            onResult(true)
            return
        }
        val account = GoogleSignIn.getAccountForExtension(this, gf.fitnessOptions)
        onGoogleFitPermissionsResult = onResult
        @Suppress("DEPRECATION")
        GoogleSignIn.requestPermissions(
            this,
            GOOGLE_FIT_PERMISSIONS_REQUEST,
            account,
            gf.fitnessOptions
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GOOGLE_FIT_PERMISSIONS_REQUEST) {
            val granted = GoogleFitManager.get(this).hasPermissions()
            onGoogleFitPermissionsResult?.invoke(granted)
            onGoogleFitPermissionsResult = null
        }
    }

    companion object {
        private const val GOOGLE_FIT_PERMISSIONS_REQUEST = 9102
    }
}
