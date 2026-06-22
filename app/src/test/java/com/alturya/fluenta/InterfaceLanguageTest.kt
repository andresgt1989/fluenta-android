package com.alturya.fluenta

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.data.InterfaceLanguages
import com.alturya.fluenta.settings.SettingsScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Test dirigido del SELECTOR de idioma de interfaz (req: cambio manual). Corre en JVM.
 * 1) El registro es coherente (códigos de 2 letras, L1 reales presentes).
 * 2) La pantalla REAL de Ajustes renderiza el selector con los nombres nativos.
 * 3) setLang persiste la elección (la lee currentLangFlow) — el override manual funciona.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h2200dp-xxhdpi")
class InterfaceLanguageTest {

    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun registry_is_consistent() {
        val codes = InterfaceLanguages.SUPPORTED.map { it.code }
        assertTrue("L1 reales presentes", codes.containsAll(listOf("es", "en", "pt")))
        InterfaceLanguages.SUPPORTED.forEach {
            assertEquals("código ISO de 2 letras: ${it.code}", 2, it.code.length)
            assertTrue("nombre nativo no vacío", it.nativeName.isNotBlank())
        }
        assertNotNull(InterfaceLanguages.byCode("EN"))            // case-insensitive
        assertEquals("pt", InterfaceLanguages.byCode("pt-BR")?.code) // tolera región
    }

    @Test fun settings_screen_renders_language_picker() {
        compose.setContent { SettingsScreen() }
        // Kit Claude Design: el idioma vive en la fila "Idioma de la app"; se despliega al tocarla.
        compose.onNodeWithText("Idioma de la app").assertIsDisplayed()
        compose.onNodeWithText("Idioma de la app").performClick()
        compose.onNodeWithText("Español").assertIsDisplayed()
        compose.onNodeWithText("English").assertIsDisplayed()
        compose.onNodeWithText("Português").assertIsDisplayed()
    }

    @Test fun set_lang_persists_manual_choice() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        I18nStore.setLang(ctx, "en")
        assertEquals("en", I18nStore.currentLangFlow(ctx).first())
        I18nStore.setLang(ctx, "pt")
        assertEquals("pt", I18nStore.currentLangFlow(ctx).first())
    }
}
