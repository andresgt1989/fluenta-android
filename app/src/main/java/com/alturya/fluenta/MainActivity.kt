package com.alturya.fluenta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.alturya.fluenta.data.TokenStore
import com.alturya.fluenta.home.HomeScreen
import com.alturya.fluenta.login.LoginScreen
import com.alturya.fluenta.network.ApiClient
import com.alturya.fluenta.ui.theme.FluentaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FluentaTheme {
                val context = LocalContext.current
                val navController = rememberNavController()
                val token by TokenStore.getToken(context).collectAsState(initial = null)

                LaunchedEffect(token) {
                    token?.let { ApiClient.setToken(it) }
                }

                val start = if (token != null) "home" else "login"

                NavHost(navController = navController, startDestination = start) {
                    composable("login") {
                        LoginScreen(onSuccess = {
                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true }
                            }
                        })
                    }
                    composable("home") {
                        HomeScreen(onLogout = {
                            navController.navigate("login") {
                                popUpTo("home") { inclusive = true }
                            }
                        })
                    }
                }
            }
        }
    }
}
