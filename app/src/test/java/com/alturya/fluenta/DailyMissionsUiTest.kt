package com.alturya.fluenta

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.alturya.fluenta.home.DailyMissions
import com.alturya.fluenta.home.DailyMissionsCard
import com.alturya.fluenta.network.UserProgress
import com.alturya.fluenta.ui.theme.FluentaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Test de UI DIRIGIDO de las Misiones diarias (corre en JVM via Robolectric, sin
 * emulador). A diferencia del crawl genérico del robo (que solo confirma "no
 * crashea"), esto hace assert de que la PANTALLA CONCRETA renderiza las 3 misiones
 * con su progreso REAL, construidas por el mismo camino de producción
 * (UserProgress → DailyMissions.build → DailyMissionsCard). Si una regresión
 * rompe el render o la lógica de progreso, el build FALLA.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h2200dp-xxhdpi")
class DailyMissionsUiTest {

    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun progress(todayXp: Int, cardsDueToday: Int) = UserProgress(
        streakDays = 3, totalXp = 500, completedLessons = 10,
        todayXp = todayXp, dailyGoalXp = 50, dailyGoalPct = 0,
        cardsDueToday = cardsDueToday,
        l1 = "es", l2 = "en", level = "A2", levelSystem = "cefr",
    )

    private fun render(todayXp: Int, cardsDueToday: Int) {
        val missions = DailyMissions.build(
            progress = progress(todayXp, cardsDueToday),
            goalXp = 50,
            titleStreak = "Preséntate hoy — protege tu racha",
            titleGoal = "Alcanza tu meta diaria",
            titleReview = "Despeja tu repaso (SRS)",
        )
        compose.setContent { FluentaTheme { DailyMissionsCard(missions = missions, onMission = {}) } }
    }

    @Test fun renders_the_three_missions_with_header() {
        render(todayXp = 0, cardsDueToday = 3)
        compose.onNodeWithText("Misiones de hoy").assertIsDisplayed()
        compose.onNodeWithText("Preséntate hoy — protege tu racha").assertIsDisplayed()
        compose.onNodeWithText("Alcanza tu meta diaria").assertIsDisplayed()
        compose.onNodeWithText("Despeja tu repaso (SRS)").assertIsDisplayed()
    }

    @Test fun counter_reflects_real_progress_partial() {
        // Sin XP y con repaso pendiente: solo... ninguna hecha → 0/3.
        render(todayXp = 0, cardsDueToday = 3)
        compose.onNodeWithText("0/3").assertIsDisplayed()
    }

    @Test fun counter_one_done_when_review_clear() {
        // Sin XP pero repaso ya despejado (0 vencidas) → 1/3.
        render(todayXp = 0, cardsDueToday = 0)
        compose.onNodeWithText("1/3").assertIsDisplayed()
    }

    @Test fun all_done_shows_celebration() {
        // Meta superada + repaso limpio → 3/3 + mensaje de celebración.
        render(todayXp = 60, cardsDueToday = 0)
        compose.onNodeWithText("3/3").assertIsDisplayed()
        compose.onNodeWithText("¡Completaste tus misiones! 🎉").assertIsDisplayed()
    }
}
