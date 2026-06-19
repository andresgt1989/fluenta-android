package com.alturya.fluenta

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.assertTouchWidthIsEqualTo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import com.alturya.fluenta.ui.SpeakerButton
import com.alturya.fluenta.ui.theme.FluentaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Test de UI DIRIGIDO de accesibilidad (Regla de oro: ejerce el componente REAL de
 * producción, sin objetos falsos). Verifica dos requisitos WCAG sobre el botón de audio
 * que usa la pantalla de Conversación:
 *  1. Tiene una etiqueta para lectores de pantalla (contentDescription).
 *  2. Su ÁREA TÁCTIL es ≥48dp (WCAG 2.5.5 / Material) — antes era 32dp.
 * Si una regresión vuelve a encoger el botón o le quita la etiqueta, el build FALLA.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h2200dp-xxhdpi")
class A11ySpeakerButtonTest {

    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun speakerButton_isLabeled_andHasMinTouchTarget() {
        compose.setContent {
            FluentaTheme {
                SpeakerButton(onClick = {}, contentDescription = "Escuchar")
            }
        }

        val node = compose.onNodeWithContentDescription("Escuchar")
        node.assertIsDisplayed()
        node.assertHasClickAction()
        // Mínimo táctil accesible: se mide el ÁREA TÁCTIL (no los bounds visuales). El
        // IconButton de Material la mantiene en 48dp aunque el ícono visual sea de 16dp
        // (vía minimumInteractiveComponentSize). Antes el Modifier.size(32.dp) la encogía.
        node.assertTouchWidthIsEqualTo(48.dp)
        node.assertTouchHeightIsEqualTo(48.dp)
    }
}
