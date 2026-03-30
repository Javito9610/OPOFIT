package com.opofit.miapp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.opofit.miapp.ui.navigation.AppNavigation
import com.opofit.miapp.ui.theme.MiAppTheme
import com.opofit.miapp.ui.viewmodels.AuthViewModel

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            MiAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val navController = rememberNavController()

                    val uiState = authViewModel.uiState.collectAsState()

                    AppNavigation(
                        navController = navController,
                        isLoggedIn = uiState.value.isLoggedIn,
                        authViewModel = authViewModel
                    )
                }
            }
        }
    }
}
