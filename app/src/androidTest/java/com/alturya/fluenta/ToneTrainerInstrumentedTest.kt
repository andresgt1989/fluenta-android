package com.alturya.fluenta

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alturya.fluenta.tone.ToneTrainerScreen
import com.alturya.fluenta.tone.ToneWord
import com.alturya.fluenta.ui.theme.FluentaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verificación DIRIGIDA en DISPOSITIVO REAL (instrumentation, Firebase Test Lab) del
 * FLUJO DE TONO completo del «chino genio» (T2): presentación → quiz de tono → fase de
 * grabación por voz. Pasa un mazo fijo para ser determinista (evita la carga async del
 * SRS). Cierra la regla dura del proyecto: nada «hecho» sin evidencia en hardware real.
 *
 * Nota honesta: Test Lab no inyecta voz real, así que la rama grabar→score→SRS-grade
 * (que solo dispara con voz detectada) está cubierta por tests unitarios JVM
 * (ToneScorerTest, ToneSrsTest); aquí se verifica que TODA la UI del flujo renderiza y
 * navega en un dispositivo real sin fallos, incluida la pantalla de grabación.
 */
@RunWith(AndroidJUnit4::class)
class ToneTrainerInstrumentedTest {

    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val deck = listOf(ToneWord("妈", "ma", 1, "mamá"))

    private fun render() {
        compose.setContent { FluentaTheme { ToneTrainerScreen(words = deck) } }
    }

    @Test fun tone_flow_renders_present_quiz_and_speak_on_real_device() {
        render()

        // 1 · Presentación: hanzi + significado + acción de escuchar.
        compose.onNodeWithText("妈").assertIsDisplayed()
        compose.onNodeWithText("mamá").assertIsDisplayed()
        compose.onNodeWithText("Escuchar").assertIsDisplayed()

        // → al quiz de tono.
        compose.onNodeWithText("Continuar  →").performClick()
        compose.onNodeWithText("¿Qué tono escuchas?").assertIsDisplayed()

        // 2 · Acierto del tono (妈 = 1.er tono) abre la producción por voz.
        compose.onNodeWithText("1.ER TONO").performClick()
        compose.onNodeWithText("🎤 Repetir en voz alta").assertIsDisplayed()

        // 3 · Fase de grabación renderiza en el dispositivo.
        compose.onNodeWithText("🎤 Repetir en voz alta").performClick()
        compose.onNodeWithText("REPITE EL TONO").assertIsDisplayed()
        compose.onNodeWithText("Oír el modelo").assertIsDisplayed()
    }
}
