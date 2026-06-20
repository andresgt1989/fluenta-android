package com.alturya.fluenta

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.alturya.fluenta.lesson.LessonPlayerScreen
import com.alturya.fluenta.lesson.LessonPlayerState
import com.alturya.fluenta.network.PlayableExercise
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Test de UI de RETENCIÓN (JVM, sin emulador). El back-press a media lección es la
 * mayor fuga: debe pedir confirmación, NO abandonar al primer toque. Verifica que
 * el diálogo aparece, que "Seguir aprendiendo" lo cierra sin salir, y que "Salir"
 * navega fuera. Si una regresión vuelve a abandonar al primer back, el build falla.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h2200dp-xxhdpi")
class LessonExitConfirmTest {

    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val midLesson = LessonPlayerState(
        loading = false,
        teachDone = true,
        exercises = listOf(
            PlayableExercise(index = 0, kind = "multiple_choice", prompt = "hola"),
            PlayableExercise(index = 1, kind = "multiple_choice", prompt = "adios"),
        ),
        currentIndex = 1,
        result = null,
    )

    private fun back() = compose.runOnUiThread {
        compose.activity.onBackPressedDispatcher.onBackPressed()
    }

    @Test fun back_midlesson_shows_confirmation_instead_of_leaving() {
        var left = false
        compose.setContent {
            LessonPlayerScreen(lessonId = "l1", onDone = { left = true }, previewState = midLesson)
        }
        back()
        compose.onNodeWithText("¿Salir de la lección?").assertIsDisplayed()
        assertFalse("no debe salir al primer back", left)
    }

    @Test fun keep_learning_dismisses_and_stays() {
        var left = false
        compose.setContent {
            LessonPlayerScreen(lessonId = "l2", onDone = { left = true }, previewState = midLesson)
        }
        back()
        compose.onNodeWithText("Seguir aprendiendo").performClick()
        assertFalse(left)
    }

    @Test fun confirm_exit_leaves_the_lesson() {
        var left = false
        compose.setContent {
            LessonPlayerScreen(lessonId = "l3", onDone = { left = true }, previewState = midLesson)
        }
        back()
        compose.onNodeWithText("Salir").performClick()
        assertTrue("confirmar debe navegar fuera", left)
    }
}
