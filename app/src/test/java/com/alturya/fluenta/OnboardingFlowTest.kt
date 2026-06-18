package com.alturya.fluenta

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.alturya.fluenta.onboarding.OnboardingScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Test de UI de FLUJO CRÍTICO (corre en JVM, sin emulador). Conduce el Onboarding
 * real y verifica que pregunta el idioma de origen — el flujo que confirmamos en
 * Firebase Test Lab. Si una regresión rompe esto (p.ej. vuelve a saltarse la
 * pregunta de idioma), el build FALLA. Red de seguridad del primer minuto.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h2200dp-xxhdpi")
class OnboardingFlowTest {

    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun onboarding_asks_source_language() {
        compose.setContent { OnboardingScreen(onPicked = { _, _ -> }) }

        // Paso 0: bienvenida con CTA "Empezar"
        compose.onNodeWithText("Empezar").assertIsDisplayed()
        compose.onNodeWithText("Empezar").performClick()

        // Paso 1: DEBE preguntar el idioma de origen (es/en/pt) — no saltarse a chino
        compose.onNodeWithText("¿Qué idioma hablas?").assertIsDisplayed()
        compose.onNodeWithText("Español").assertIsDisplayed()
        compose.onNodeWithText("Inglés").assertIsDisplayed()
        compose.onNodeWithText("Portugués").assertIsDisplayed()
    }

    @Test fun onboarding_then_asks_target_language() {
        compose.setContent { OnboardingScreen(onPicked = { _, _ -> }) }
        compose.onNodeWithText("Empezar").performClick()
        compose.onNodeWithText("Español").performClick()

        // Paso 2: pregunta qué aprender (el default NO es chino — Inglés está disponible)
        compose.onNodeWithText("¿Qué idioma quieres aprender?").assertIsDisplayed()
        compose.onNodeWithText("Inglés").assertIsDisplayed()
    }
}
