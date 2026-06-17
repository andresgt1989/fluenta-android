package com.alturya.fluenta

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.alturya.fluenta.conversation.ConversationScreen
import com.alturya.fluenta.curriculum.CurriculumMapScreen
import com.alturya.fluenta.diagnostic.DiagnosticScreen
import com.alturya.fluenta.exercises.MatchScreen
import com.alturya.fluenta.home.HomeScreen
import com.alturya.fluenta.languages.LanguageSelectorScreen
import com.alturya.fluenta.login.LoginScreen
import com.alturya.fluenta.onboarding.OnboardingScreen
import com.alturya.fluenta.profile.ProfileScreen
import com.alturya.fluenta.progress.ProgressScreen
import com.alturya.fluenta.pronunciation.PronunciationScreen
import com.alturya.fluenta.repaso.RepasoScreen
import com.alturya.fluenta.script.ScriptScreen
import com.alturya.fluenta.ui.theme.FluentaTheme
import com.alturya.fluenta.verbs.VerbsTodayScreen
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h2200dp-xxhdpi")
class ScreenshotTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun shot(name: String, content: @Composable () -> Unit) {
        compose.setContent { FluentaTheme { content() } }
        compose.onRoot().captureRoboImage("build/screens/$name.png")
    }

    @Test fun login() = shot("login") { LoginScreen(onSuccess = {}) }
    @Test fun onboarding() = shot("onboarding") { OnboardingScreen(onPicked = { _, _ -> }) }
    @Test fun home() = shot("home") {
        HomeScreen(previewState = com.alturya.fluenta.home.HomeState(
            loading = false,
            coach = com.alturya.fluenta.network.CoachNext(
                message = "¡Vas muy bien! Hoy una lección corta te acerca a B1. ¿Seguimos?",
                goal = "b1", l2 = "en", l2Name = "English", currentLevel = "a2", progressPct = 35,
                action = com.alturya.fluenta.network.CoachAction("lesson", "Tu lección de hoy", "El siguiente paso hacia C1.", "home"),
            ),
        ))
    }
    @Test fun progress() = shot("progress") { ProgressScreen() }
    @Test fun profile() = shot("profile") { ProfileScreen() }
    @Test fun conversation() = shot("conversation") {
        ConversationScreen(onDone = {}, previewState = com.alturya.fluenta.conversation.ConvoUiState(
            phase = com.alturya.fluenta.conversation.ConvoPhase.YOUR_TURN,
            messages = listOf(
                com.alturya.fluenta.conversation.ConvoMessage(true, "Hi! Welcome to the café. What would you like?"),
                com.alturya.fluenta.conversation.ConvoMessage(false, "I want a coffee please",
                    correction = "I'd like a coffee, please.",
                    tip = "Más natural: usa 'I'd like' en vez de 'I want' al pedir algo."),
                com.alturya.fluenta.conversation.ConvoMessage(true, "Great choice! Small or large?"),
            ),
            suggestion = "A large coffee, please.",
        ))
    }
    @Test fun pronunciation() = shot("pronunciation") { PronunciationScreen() }
    @Test fun repaso() = shot("repaso") { RepasoScreen(onDone = {}) }
    @Test fun map() = shot("map") { CurriculumMapScreen() }
    @Test fun match() = shot("match") { MatchScreen() }
    @Test fun verbs() = shot("verbs") { VerbsTodayScreen() }
    @Test fun languages() = shot("languages") { LanguageSelectorScreen() }
    @Test fun diagnostic() = shot("diagnostic") { DiagnosticScreen() }
    @Test fun script() = shot("script") { ScriptScreen(l2 = "ja", onDone = {}) }
}
