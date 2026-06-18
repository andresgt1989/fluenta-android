package com.alturya.fluenta

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.alturya.fluenta.curriculum.CurriculumMapScreen
import com.alturya.fluenta.data.Analytics
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.data.TokenStore
import com.alturya.fluenta.diagnostic.DiagnosticScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.alturya.fluenta.exercises.MatchScreen
import com.alturya.fluenta.home.HomeScreen
import com.alturya.fluenta.conversation.ConversationScreen
import com.alturya.fluenta.languages.LanguageSelectorScreen
import com.alturya.fluenta.script.ScriptScreen
import com.alturya.fluenta.lesson.GuestLessonScreen
import com.alturya.fluenta.lesson.LessonPlayerScreen
import com.alturya.fluenta.login.LoginScreen
import com.alturya.fluenta.network.ApiClient
import com.alturya.fluenta.onboarding.OnboardingScreen
import kotlinx.coroutines.launch
import com.alturya.fluenta.profile.ProfileScreen
import com.alturya.fluenta.progress.ProgressScreen
import com.alturya.fluenta.pronunciation.PronunciationScreen
import com.alturya.fluenta.repaso.RepasoScreen
import com.alturya.fluenta.settings.SettingsScreen
import com.alturya.fluenta.ui.theme.FluentaTheme
import com.alturya.fluenta.upgrade.PaywallScreen
import com.alturya.fluenta.verbs.VerbsTodayScreen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.ui.graphics.vector.ImageVector

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
                val scope = rememberCoroutineScope()
                val startState by TokenStore.getStartState(context).collectAsState(initial = null)
                val token = startState?.first
                val deepLinkLesson by pendingLessonId

                // RTL: mirror the whole UI when the interface language is ar/fa/ur/he.
                val uiLang by I18nStore.currentLangFlow(context).collectAsState(initial = "es")
                val layoutDir = if (I18nStore.isRtl(uiLang))
                    androidx.compose.ui.unit.LayoutDirection.Rtl
                else androidx.compose.ui.unit.LayoutDirection.Ltr

                LaunchedEffect(token) { token?.let { ApiClient.setToken(it) } }

                // Señal de actividad diaria (D1/D7). Una vez por arranque.
                LaunchedEffect(Unit) { Analytics.track(context, Analytics.APP_OPEN) }

                // Respetar el toggle de sonido del usuario en toda la app.
                val sfxOn by com.alturya.fluenta.data.SettingsStore.sfxEnabled(context).collectAsState(initial = true)
                LaunchedEffect(sfxOn) { com.alturya.fluenta.audio.Sfx.enabled = sfxOn }

                // Capa 1 — Detección L1 + 30 pares: cargar strings UI en idioma nativo.
                // Si el usuario tiene perfil cargado, el L1 del perfil manda. Sin perfil:
                // device locale como fallback razonable. Backend cachea 30d, app cachea 24h.
                LaunchedEffect(token) {
                    val deviceLang = java.util.Locale.getDefault().language
                        .takeIf { it.length == 2 } ?: "es"
                    I18nStore.ensureLoaded(context, deviceLang)
                }

                // First-run routing, decided ONCE from a single atomic prefs read so
                // there's no token/onboarding race, then frozen (changing a NavHost
                // startDestination after composition would rebuild the graph).
                var startDest by remember { mutableStateOf<String?>(null) }
                LaunchedEffect(startState) {
                    if (startDest == null && startState != null) {
                        val (tok, onboardingDone) = startState!!
                        startDest = when {
                            tok != null -> "main"
                            onboardingDone -> "login"
                            else -> "onboarding"
                        }
                    }
                }

                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalLayoutDirection provides layoutDir
                ) {
                val dest = startDest
                if (dest == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                NavHost(
                    navController = rootNav,
                    startDestination = dest
                ) {
                    composable("onboarding") {
                        OnboardingScreen(onPicked = { l1, l2 ->
                            scope.launch { TokenStore.saveOnboardingChoice(context, l1, l2) }
                            // Cumple la promesa "practica hablando": el wedge de voz ES el
                            // primer contacto (hablar en <60s, sin cuenta), luego el quiz.
                            rootNav.navigate("guest_conversation/$l1/$l2")
                        })
                    }
                    composable("login") {
                        LoginScreen(
                            onSuccess = { isNewUser ->
                                if (isNewUser) {
                                    // New account → detect CEFR level so the whole curriculum
                                    // (lessons, coach, path) turns on at the right level.
                                    rootNav.navigate("level_test") { popUpTo("login") { inclusive = true } }
                                } else {
                                    rootNav.navigate("main") { popUpTo("login") { inclusive = true } }
                                }
                            },
                            onTryGuest = {
                                rootNav.navigate("onboarding")
                            },
                        )
                    }
                    composable("level_test") {
                        DiagnosticScreen(onDone = {
                            rootNav.navigate("main") { popUpTo("level_test") { inclusive = true } }
                        })
                    }
                    composable(
                        route = "guest_lesson/{l1}/{l2}",
                        arguments = listOf(
                            navArgument("l1") { type = NavType.StringType },
                            navArgument("l2") { type = NavType.StringType },
                        ),
                    ) { entry ->
                        val gl1 = entry.arguments?.getString("l1") ?: "es"
                        val gl2 = entry.arguments?.getString("l2") ?: "en"
                        GuestLessonScreen(
                            l1 = gl1,
                            l2 = gl2,
                            onSignUp = {
                                rootNav.navigate("login") { popUpTo("onboarding") { inclusive = true } }
                            },
                            onBack = {
                                rootNav.navigate("login") { popUpTo("onboarding") { inclusive = true } }
                            },
                        )
                    }
                    composable(
                        route = "guest_conversation/{l1}/{l2}",
                        arguments = listOf(
                            navArgument("l1") { type = NavType.StringType },
                            navArgument("l2") { type = NavType.StringType },
                        ),
                    ) { entry ->
                        val gl1 = entry.arguments?.getString("l1") ?: "es"
                        val gl2 = entry.arguments?.getString("l2") ?: "en"
                        ConversationScreen(
                            guest = true,
                            l1 = gl1,
                            l2 = gl2,
                            // Pico de valor tras hablar inglés → registro (conversión), no el quiz.
                            onDone = { rootNav.navigate("login") { popUpTo("onboarding") { inclusive = true } } },
                        )
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
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // App was already running and the user tapped a https://fluenta.alturya.com/app/lesson/<id>
        // link — surface the new id so the Compose tree navigates over.
        intent.lessonIdFromDeepLink()?.let { pendingLessonId.value = it }
    }
}

private data class Tab(
    val route: String,
    val labelKey: String,
    val defaultLabel: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val TABS = listOf(
    Tab("home",     "nav.home",     "Inicio",    Icons.Filled.Home,     Icons.Outlined.Home),
    Tab("map",      "nav.lessons",  "Lecciones", Icons.Filled.Map,      Icons.Outlined.Map),
    Tab("repaso",   "nav.review",   "Repasar",   Icons.Filled.Replay,   Icons.Outlined.Replay),
    Tab("progress", "nav.progress", "Progreso",  Icons.Filled.BarChart, Icons.Outlined.BarChart),
    Tab("profile",  "nav.profile",  "Perfil",    Icons.Filled.Person,   Icons.Outlined.Person),
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
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == tab.route) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.defaultLabel,
                            )
                        },
                        label = { Text(I18nStore.t(tab.labelKey, tab.defaultLabel)) }
                    )
                }
            }
        }
    ) { pad ->
        NavHost(
            navController = nav,
            startDestination = "home",
            modifier = Modifier.padding(pad),
            enterTransition = { slideInHorizontally(tween(280)) { it / 4 } + fadeIn(tween(220)) },
            exitTransition = { slideOutHorizontally(tween(220)) { -it / 4 } + fadeOut(tween(180)) },
            popEnterTransition = { slideInHorizontally(tween(280)) { -it / 4 } + fadeIn(tween(220)) },
            popExitTransition = { slideOutHorizontally(tween(220)) { it / 4 } + fadeOut(tween(180)) },
        ) {
            composable("home") {
                HomeScreen(
                    onSeeMap = { nav.navigate("map") { launchSingleTop = true } },
                    onPronunciation = { nav.navigate("pronunciation") { launchSingleTop = true } },
                    onPlayMatch = { nav.navigate("match") { launchSingleTop = true } },
                    onStartLesson = { lessonId -> nav.navigate("lesson/$lessonId") },
                    onChangeLanguage = { nav.navigate("languages") { launchSingleTop = true } },
                    onRepaso = { nav.navigate("repaso") { launchSingleTop = true } },
                    onLevelTest = { nav.navigate("diagnostic") { launchSingleTop = true } },
                    onConversation = { nav.navigate("conversation") { launchSingleTop = true } },
                    onConversationLesson = { lessonId -> nav.navigate("conversation_lesson/$lessonId") },
                    onScript = { l2 -> nav.navigate("script/$l2") { launchSingleTop = true } },
                    onUpgrade = { nav.navigate("paywall") },
                )
            }
            composable("paywall") {
                PaywallScreen(onDismiss = { nav.popBackStack() })
            }
            composable(
                route = "script/{l2}",
                arguments = listOf(navArgument("l2") { type = NavType.StringType }),
            ) { entry ->
                ScriptScreen(l2 = entry.arguments?.getString("l2") ?: "ja", onDone = { nav.popBackStack() })
            }
            composable("match") { MatchScreen(onDone = { nav.popBackStack() }) }
            composable(
                route = "lesson/{lessonId}",
                arguments = listOf(navArgument("lessonId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val lessonId = backStackEntry.arguments?.getString("lessonId") ?: ""
                LessonPlayerScreen(
                    lessonId = lessonId,
                    onDone = { nav.popBackStack() },
                    onConversation = { nav.navigate("conversation") { launchSingleTop = true } },
                )
            }
            composable("map") {
                CurriculumMapScreen(
                    onStartLesson = { lessonId -> nav.navigate("lesson/$lessonId") },
                    onConversationLesson = { lessonId -> nav.navigate("conversation_lesson/$lessonId") },
                )
            }
            composable("verbs") { VerbsTodayScreen() }
            composable("pronunciation") { PronunciationScreen() }
            composable("conversation") { ConversationScreen(onDone = { nav.popBackStack() }) }
            composable(
                route = "conversation_lesson/{lessonId}",
                arguments = listOf(navArgument("lessonId") { type = NavType.StringType }),
            ) { entry ->
                val lessonId = entry.arguments?.getString("lessonId") ?: ""
                ConversationScreen(lessonId = lessonId, onDone = { nav.popBackStack() })
            }
            composable("repaso") {
                RepasoScreen(onDone = {
                    nav.navigate("home") {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true; restoreState = true
                    }
                })
            }
            composable("progress") { ProgressScreen() }
            composable("profile") {
                ProfileScreen(
                    onChangeLanguage = { nav.navigate("languages") },
                    onLogout = onLogout,
                    onDiagnostic = { nav.navigate("diagnostic") },
                    onSettings = { nav.navigate("settings") },
                )
            }
            composable("settings") {
                SettingsScreen(onBack = { nav.popBackStack() })
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
