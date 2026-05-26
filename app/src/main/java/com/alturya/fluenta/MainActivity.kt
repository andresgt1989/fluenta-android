package com.alturya.fluenta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.alturya.fluenta.curriculum.CurriculumMapScreen
import com.alturya.fluenta.data.TokenStore
import com.alturya.fluenta.home.HomeScreen
import com.alturya.fluenta.languages.LanguageSelectorScreen
import com.alturya.fluenta.login.LoginScreen
import com.alturya.fluenta.network.ApiClient
import com.alturya.fluenta.profile.ProfileScreen
import com.alturya.fluenta.progress.ProgressScreen
import com.alturya.fluenta.pronunciation.PronunciationScreen
import com.alturya.fluenta.ui.theme.FluentaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FluentaTheme {
                val context = LocalContext.current
                val rootNav = rememberNavController()
                val token by TokenStore.getToken(context).collectAsState(initial = null)

                LaunchedEffect(token) { token?.let { ApiClient.setToken(it) } }

                NavHost(
                    navController = rootNav,
                    startDestination = if (token != null) "main" else "login"
                ) {
                    composable("login") {
                        LoginScreen(onSuccess = {
                            rootNav.navigate("main") { popUpTo("login") { inclusive = true } }
                        })
                    }
                    composable("main") {
                        MainScaffold(onLogout = {
                            rootNav.navigate("login") { popUpTo("main") { inclusive = true } }
                        })
                    }
                }
            }
        }
    }
}

private data class Tab(val route: String, val label: String, val icon: String)

private val TABS = listOf(
    Tab("home", "Inicio", "🏠"),
    Tab("map", "Mapa", "🗺"),
    Tab("progress", "Progreso", "📊"),
    Tab("profile", "Perfil", "👤"),
)

@Composable
private fun MainScaffold(onLogout: () -> Unit) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.hierarchy?.firstOrNull()?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                TABS.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            nav.navigate(tab.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Text(tab.icon) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { pad ->
        NavHost(
            navController = nav,
            startDestination = "home",
            modifier = Modifier.padding(pad)
        ) {
            composable("home") {
                HomeScreen(
                    onSeeMap = { nav.navigate("map") { launchSingleTop = true } },
                    onPronunciation = { nav.navigate("pronunciation") { launchSingleTop = true } }
                )
            }
            composable("map") { CurriculumMapScreen() }
            composable("pronunciation") { PronunciationScreen() }
            composable("progress") { ProgressScreen() }
            composable("profile") {
                ProfileScreen(
                    onChangeLanguage = { nav.navigate("languages") },
                    onLogout = onLogout
                )
            }
            composable("languages") {
                LanguageSelectorScreen(onChanged = { nav.popBackStack() })
            }
        }
    }
}
