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
 * SIN FRICCIÓN (mock Claude Design) y verifica el camino que ataca la fuga #1:
 * idioma destino con CHINO destacado/preseleccionado → nivel SALTABLE. Sin paso de
 * idioma-origen ni de motivación (esos añadían fricción antes de la 1ª lección).
 * Si una regresión reintroduce fricción, el build FALLA. Red de seguridad del 1er minuto.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h2200dp-xxhdpi")
class OnboardingFlowTest {

    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun onboarding_starts_with_target_language_chinese_featured() {
        compose.setContent { OnboardingScreen(onPicked = { _, _ -> }) }

        // Paso 1 (sin fricción): arranca DIRECTO en "¿qué quieres aprender?", con
        // chino DESTACADO y preseleccionado — sin preguntar primero el idioma de origen.
        compose.onNodeWithText("¿Qué quieres aprender?").assertIsDisplayed()
        compose.onNodeWithText("DESTACADO").assertIsDisplayed()
    }

    @Test fun onboarding_then_asks_level_and_is_skippable() {
        compose.setContent { OnboardingScreen(onPicked = { _, _ -> }) }
        // Chino ya está preseleccionado → el CTA confirma chino en 1 toque.
        compose.onNodeWithText("Continuar con", substring = true).performClick()

        // Paso 2: nivel de partida, SALTABLE (link "Saltar"). Sin paso de motivación.
        compose.onNodeWithText("¿Cuánto chino sabes?").assertIsDisplayed()
        compose.onNodeWithText("Desde cero").assertIsDisplayed()
        compose.onNodeWithText("Saltar").assertIsDisplayed()
    }
}
