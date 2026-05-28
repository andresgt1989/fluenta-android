package com.alturya.fluenta

import android.content.Intent
import android.net.Uri
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
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.data.TokenStore
import com.alturya.fluenta.diagnostic.DiagnosticScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.alturya.fluenta.exercises.MatchScreen
import com.alturya.fluenta.home.HomeScreen
import com.alturya.fluenta.languages.LanguageSelectorScreen
import com.alturya.fluenta.lesson.LessonPlayerScreen
import com.alturya.fluenta.login.LoginScreen
import com.alturya.fluenta.network.ApiClient
import com.alturya.fluenta.profile.ProfileScreen
import com.alturya.fluenta.progress.ProgressScreen
import com.alturya.fluenta.pronunciation.PronunciationScreen
import com.alturya.fluenta.ui.theme.FluentaTheme
import com.alturya.fluenta.verbs.VerbsTodayScreen

/**
 * Pulls a lesson UUID out of either of the two deep-link URI shapes we accept:
 *   https://fluenta.alturya.com/app/lesson/<id>
 *   fluenta://lesson/<id>
 * Returns null when the URI is neither of those (e.g. cold launch from launcher).
 */
private fun Intent.lessonIdFromDeepLink(): String? {
    if (action != Intent.ACTION_VIEW) return null
    val uri: Uri = data ?: return null
    return when {
        uri.scheme == "https" && uri.host == "fluenta.alturya.com" && uri.pathSegments.size >= 2
            && uri.pathSegments[0] == "app" && uri.pathSegments[1] == "lesson" ->
            uri.pathSegments.getOrNull(2)
        uri.scheme == "fluenta" && uri.host == "lesson" ->
            uri.pathSegments.firstOrNull()
        else -> null
    }
}

class MainActivity : ComponentActivity() {
    // Single source of truth for the lesson id that should auto-open on launch.
    // State holder is read by the Compose tree below so onNewIntent can also drive it.
    private val pendingLessonId = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingLessonId.value = intent.lessonIdFromDeepLink()
        setContent {
            FluentaTheme {
                val context = LocalContext.current
                val rootNav = rememberNavController()
                val token by TokenStore.getToken(context).collectAsState(initial = null)
                val deepLinkLesson by pendingLessonId

                LaunchedEffect(token) { token?.let { ApiClient.setToken(it) } }

                // Capa 1 — Detección L1 + 30 pares: cargar strings UI en idioma nativo.
                // Si el usuario tiene perfil cargado, el L1 del perfil manda. Sin perfil:
                // device locale como fallback razonable. Backend cachea 30d, app cachea 24h.
                LaunchedEffect(token) {
                    val deviceLang = java.util.Locale.getDefault().language
                        .takeIf { it.length == 2 } ?: "es"
                    I18nStore.ensureLoaded(context, deviceLang)
                }

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
                        MainScaffold(
                            onLogout = {
                                rootNav.navigate("login") { popUpTo("main") { inclusive = true } }
                            },
                            pendingLessonId = deepLinkLesson,
                            onLessonConsumed = { pendingLessonId.value = null },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // App was already running and the user tapped a https://fluenta.alturya.com/app/lesson/<id>
        // link — surface the new id so the Compose tree navigates over.
        intent.lessonIdFromDeepLink()?.let { pendingLessonId.value = it }
    }
}

private data class Tab(val route: String, val label: String, val icon: String)

private val TABS = listOf(
    Tab("home", "Inicio", "🏠"),
    Tab("verbs", "Verbos", "📚"),
    Tab("map", "Mapa", "🗺"),
    Tab("progress", "Progreso", "📊"),
    Tab("profile", "Perfil", "👤"),
)

@Composable
private fun MainScaffold(
    onLogout: () -> Unit,
    pendingLessonId: String? = null,
    onLessonConsumed: () -> Unit = {},
) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.hierarchy?.firstOrNull()?.route

    // Auto-open the lesson when MainActivity received it via deep link.
    LaunchedEffect(pendingLessonId) {
        pendingLessonId?.let { id ->
            nav.navigate("lesson/$id")
            onLessonConsumed()
        }
    }

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
                    onPronunciation = { nav.navigate("pronunciation") { launchSingleTop = true } },
                    onPlayMatch = { nav.navigate("match") { launchSingleTop = true } },
                    onStartLesson = { lessonId -> nav.navigate("lesson/$lessonId") },
                )
            }
            composable("match") { MatchScreen(onDone = { nav.popBackStack() }) }
            composable(
                route = "lesson/{lessonId}",
                arguments = listOf(navArgument("lessonId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val lessonId = backStackEntry.arguments?.getString("lessonId") ?: ""
                LessonPlayerScreen(lessonId = lessonId, onDone = { nav.popBackStack() })
            }
            composable("map") {
                CurriculumMapScreen(onStartLesson = { lessonId -> nav.navigate("lesson/$lessonId") })
            }
            composable("verbs") { VerbsTodayScreen() }
            composable("pronunciation") { PronunciationScreen() }
            composable("progress") { ProgressScreen() }
            composable("profile") {
                ProfileScreen(
                    onChangeLanguage = { nav.navigate("languages") },
                    onLogout = onLogout,
                    onDiagnostic = { nav.navigate("diagnostic") }
                )
            }
            composable("languages") {
                LanguageSelectorScreen(onChanged = { nav.popBackStack() })
            }
            composable("diagnostic") {
                DiagnosticScreen(onDone = { nav.popBackStack() })
            }
        }
    }
}
