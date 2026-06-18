package com.alturya.fluenta

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alturya.fluenta.home.DailyMissions
import com.alturya.fluenta.home.DailyMissionsCard
import com.alturya.fluenta.network.UserProgress
import com.alturya.fluenta.ui.theme.FluentaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test de UI DIRIGIDO en DISPOSITIVO REAL (instrumentation, corre en Firebase Test
 * Lab). Cierra la brecha que el robo libre no puede: el crawl genérico no alcanza
 * pantallas con estado específico (Home cargó en shimmer; first-run onboarding no
 * aparece en cuenta existente). Esto navega A PROPÓSITO al componente y hace assert
 * de que las 3 misiones renderizan con su progreso real — sobre hardware real.
 */
@RunWith(AndroidJUnit4::class)
class DailyMissionsInstrumentedTest {

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

    @Test fun renders_three_missions_on_real_device() {
        render(todayXp = 0, cardsDueToday = 3)
        compose.onNodeWithText("Misiones de hoy").assertIsDisplayed()
        compose.onNodeWithText("Preséntate hoy — protege tu racha").assertIsDisplayed()
        compose.onNodeWithText("Alcanza tu meta diaria").assertIsDisplayed()
        compose.onNodeWithText("Despeja tu repaso (SRS)").assertIsDisplayed()
        compose.onNodeWithText("0/3").assertIsDisplayed()
    }

    @Test fun all_done_celebration_on_real_device() {
        render(todayXp = 60, cardsDueToday = 0)
        compose.onNodeWithText("3/3").assertIsDisplayed()
        compose.onNodeWithText("¡Completaste tus misiones! 🎉").assertIsDisplayed()
    }
}
