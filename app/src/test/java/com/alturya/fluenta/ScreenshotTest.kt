package com.alturya.fluenta

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.alturya.fluenta.conversation.ConversationScreen
import com.alturya.fluenta.conversation.ConvoMessage
import com.alturya.fluenta.conversation.ConvoPhase
import com.alturya.fluenta.conversation.ConvoUiState
import com.alturya.fluenta.curriculum.CurriculumMapScreen
import com.alturya.fluenta.diagnostic.DiagnosticScreen
import com.alturya.fluenta.exercises.MatchScreen
import com.alturya.fluenta.home.HomeScreen
import com.alturya.fluenta.home.HomeState
import com.alturya.fluenta.languages.LanguageSelectorScreen
import com.alturya.fluenta.lesson.LessonPlayerScreen
import com.alturya.fluenta.lesson.LessonPlayerState
import com.alturya.fluenta.login.LoginScreen
import com.alturya.fluenta.network.Badge
import com.alturya.fluenta.network.CoachAction
import com.alturya.fluenta.network.CoachNext
import com.alturya.fluenta.network.ErrorItem
import com.alturya.fluenta.network.ExerciseCheckResponse
import com.alturya.fluenta.network.PlayableExercise
import com.alturya.fluenta.network.Skill
import com.alturya.fluenta.network.TeachItem
import com.alturya.fluenta.network.UserProfile
import com.alturya.fluenta.network.UserProgress
import com.alturya.fluenta.onboarding.OnboardingScreen
import com.alturya.fluenta.profile.ProfileScreen
import com.alturya.fluenta.profile.ProfileState
import com.alturya.fluenta.progress.ProgressScreen
import com.alturya.fluenta.progress.ProgressState
import com.alturya.fluenta.pronunciation.PronunciationScreen
import com.alturya.fluenta.repaso.RepasoScreen
import com.alturya.fluenta.repaso.RepasoState
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

    // ---------- AUTH FLOW ----------
    @Test fun login() = shot("login") { LoginScreen(onSuccess = {}) }
    @Test fun onboarding() = shot("onboarding") { OnboardingScreen(onPicked = { _, _ -> }) }

    // ---------- HOME ----------
    @Test fun home() = shot("home") {
        HomeScreen(previewState = HomeState(
            loading = false,
            coach = CoachNext(
                message = "¡Vas muy bien! Hoy una lección corta te acerca a B1. ¿Seguimos?",
                goal = "b1", l2 = "en", l2Name = "English", currentLevel = "a2", progressPct = 35,
                action = CoachAction("lesson", "Tu lección de hoy", "El siguiente paso hacia B1.", "home"),
            ),
        ))
    }

    // Reproduces the REAL broken first-run: returning account stuck on l2="zh" with
    // zero progress — what the frustrated user actually sees, not curated English data.
    @Test fun home_real_zh() = shot("home_real_zh") {
        HomeScreen(previewState = HomeState(
            loading = false,
            profile = UserProfile(id = "u1", phone = "device", l1 = "es", l2 = "zh",
                level = "hsk1", levelSystem = "hsk", goalTrack = "hsk3", plan = "free",
                streakDays = 0, totalXp = 0, currentState = "active"),
            progress = UserProgress(streakDays = 0, totalXp = 0, completedLessons = 0,
                todayXp = 0, dailyGoalXp = 100, dailyGoalPct = 0, cardsDueToday = 0,
                l1 = "es", l2 = "zh", level = "hsk1", levelSystem = "hsk"),
            coach = CoachNext(
                message = "你好！今天我们学习汉字。开始吧？",
                goal = "hsk3", l2 = "zh", l2Name = "中文", currentLevel = "hsk1", progressPct = 5,
                action = CoachAction("script", "学写汉字", "先学会读写。", "home"),
            ),
        ))
    }

    // ---------- LECCIÓN ----------
    @Test fun lesson_teach() = shot("lesson_teach") {
        LessonPlayerScreen(lessonId = "preview", onDone = {}, previewState = LessonPlayerState(
            loading = false, title = "Pedidos en un restaurante",
            teach = listOf(
                TeachItem("I'd like", "Quisiera", note = "Más educado que 'I want'"),
                TeachItem("Could I have", "¿Me podría traer?", note = "Para pedir algo en la mesa"),
                TeachItem("The bill, please", "La cuenta, por favor"),
            ),
            teachDone = false,
            exercises = listOf(PlayableExercise(0, "translate_l1_to_l2", prompt = "Quisiera un café")),
        ))
    }
    @Test fun lesson_quiz() = shot("lesson_quiz") {
        LessonPlayerScreen(lessonId = "preview", onDone = {}, previewState = LessonPlayerState(
            loading = false, title = "Pedidos en un restaurante",
            teach = listOf(
                TeachItem("I'd like", "Quisiera", note = "Más educado que 'I want'"),
                TeachItem("Could I have", "¿Me podría traer?", note = "Para pedir algo en la mesa"),
                TeachItem("The bill, please", "La cuenta, por favor"),
            ),
            teachDone = true, currentIndex = 0,
            exercises = listOf(
                PlayableExercise(0, "translate_l1_to_l2", prompt = "Quisiera un café, por favor"),
                PlayableExercise(1, "multiple_choice",
                    prompt = "¿Cómo pides la cuenta?",
                    options = listOf("The bill, please", "I want money", "Give me check", "Bill now")),
                PlayableExercise(2, "word_order",
                    prompt = "Ordena la frase",
                    tokens = listOf("I'd", "like", "a", "table", "for", "two")),
            ),
            hearts = 5,
            combo = 3,
        ))
    }
    @Test fun lesson_feedback_correct() = shot("lesson_feedback_correct") {
        LessonPlayerScreen(lessonId = "preview", onDone = {}, previewState = LessonPlayerState(
            loading = false, title = "Pedidos en un restaurante",
            teach = emptyList(), teachDone = true, currentIndex = 0,
            exercises = listOf(PlayableExercise(0, "translate_l1_to_l2", prompt = "Quisiera un café")),
            answers = mapOf(0 to "I'd like a coffee"),
            feedback = ExerciseCheckResponse(correct = true, expected = "I'd like a coffee", feedback = "¡Perfecto! Muy natural."),
            hearts = 3,
        ))
    }
    @Test fun lesson_feedback_wrong() = shot("lesson_feedback_wrong") {
        LessonPlayerScreen(lessonId = "preview", onDone = {}, previewState = LessonPlayerState(
            loading = false, title = "Pedidos en un restaurante",
            teach = emptyList(), teachDone = true, currentIndex = 0,
            exercises = listOf(PlayableExercise(0, "translate_l1_to_l2", prompt = "Quisiera un café")),
            answers = mapOf(0 to "I want a coffee"),
            feedback = ExerciseCheckResponse(correct = false, expected = "I'd like a coffee, please.", feedback = "Correcto pero informal. 'I'd like' suena más educado."),
            hearts = 2,
        ))
    }

    // ---------- CONVERSACIÓN ----------
    @Test fun conversation() = shot("conversation") {
        ConversationScreen(onDone = {}, previewState = ConvoUiState(
            phase = ConvoPhase.YOUR_TURN,
            messages = listOf(
                ConvoMessage(true, "Hi! Welcome to the café. What would you like?"),
                ConvoMessage(false, "I want a coffee please",
                    correction = "I'd like a coffee, please.",
                    tip = "Más natural: usa 'I'd like' en vez de 'I want' al pedir algo."),
                ConvoMessage(true, "Great choice! Small or large?"),
            ),
            suggestion = "A large coffee, please.",
        ))
    }

    // ---------- REPASO (SRS) ----------
    @Test fun repaso_card() = shot("repaso_card") {
        RepasoScreen(onDone = {}, previewState = RepasoState(
            loading = false,
            queue = listOf(
                ErrorItem(id = "e1", errorType = "grammar", errorCategory = "CONDICIONAL",
                    original = "If I will go to London, I visit Big Ben.",
                    corrected = "If I go to London, I'll visit Big Ben.",
                    priority = "high", source = "app",
                    reviewCount = 2, nextReviewAt = null, masteredAt = null, firstSeenAt = null),
                ErrorItem(id = "e2", errorType = "vocab", errorCategory = "PHRASAL VERB",
                    original = "I look for my keys every morning.",
                    corrected = "I look for my keys every morning. (aquí 'search for' sería más natural)",
                    priority = "medium", source = "wa",
                    reviewCount = 1, nextReviewAt = null, masteredAt = null, firstSeenAt = null),
            ),
            index = 0, revealed = true,
        ))
    }
    @Test fun repaso_empty() = shot("repaso_empty") {
        RepasoScreen(onDone = {}, previewState = RepasoState(loading = false, queue = emptyList()))
    }

    // ---------- PROGRESO ----------
    @Test fun progress() = shot("progress") {
        ProgressScreen(previewState = ProgressState(
            loading = false,
            profile = UserProfile(id = "u1", phone = "+52 55 1234 5678",
                l1 = "es", l2 = "en", level = "a2", levelSystem = "cefr",
                goalTrack = "b1", plan = "free", streakDays = 12, totalXp = 1840,
                currentState = "active"),
            progress = UserProgress(streakDays = 12, totalXp = 1840, completedLessons = 23,
                todayXp = 60, dailyGoalXp = 100, dailyGoalPct = 60,
                cardsDueToday = 5, l1 = "es", l2 = "en", level = "a2", levelSystem = "cefr"),
            skills = listOf(
                Skill("vocabulary", "Vocabulario", score = 68, open = 45, mastered = 120, total = 200),
                Skill("grammar", "Gramática", score = 54, open = 30, mastered = 80, total = 180),
                Skill("pronunciation", "Pronunciación", score = 72, open = 20, mastered = 95, total = 150),
            ),
            wpm = 0, taskSuccessRate = 78,
            errors = listOf(
                ErrorItem("e1", "grammar", "CONDICIONAL", "If I will go...", "If I go...",
                    "high", null, null, null, null, "app"),
            ),
        ))
    }

    // ---------- PERFIL ----------
    @Test fun profile() = shot("profile") {
        ProfileScreen(previewState = ProfileState(
            loading = false,
            profile = UserProfile(id = "u1", phone = "+52 55 1234 5678",
                l1 = "es", l2 = "en", level = "a2", levelSystem = "cefr",
                goalTrack = "b1", plan = "free", streakDays = 12, totalXp = 1840,
                currentState = "active"),
            badges = listOf(
                Badge(id = "first_lesson", icon = "🎓", title = "Primera lección", desc = "Completaste tu primera lección", earned = true),
                Badge(id = "streak_7", icon = "🔥", title = "7 días seguidos", desc = "Racha de 7 días", earned = true),
                Badge(id = "streak_30", icon = "💎", title = "30 días seguidos", desc = "Racha de 30 días", earned = false),
            ),
            badgesEarned = 2, badgesTotal = 3,
        ))
    }

    // ---------- MAPA DE CURRÍCULO ----------
    @Test fun map() = shot("map") {
        CurriculumMapScreen(previewState = com.alturya.fluenta.curriculum.CurriculumState(
            loading = false, l1 = "es", l2 = "en",
            units = listOf(
                com.alturya.fluenta.network.CurriculumUnit("u1", 1, "Saludos y presentaciones", "Primeras palabras en inglés",
                    listOf(
                        com.alturya.fluenta.network.Lesson("l1", 1, "Hola y adiós", "vocab", completed = true),
                        com.alturya.fluenta.network.Lesson("l2", 2, "¿Cómo te llamas?", "grammar", completed = true),
                        com.alturya.fluenta.network.Lesson("l3", 3, "Números del 1 al 10", "vocab", completed = false),
                        com.alturya.fluenta.network.Lesson("l4", 4, "Conversación: Conocer gente", "conversation", completed = false),
                    )),
                com.alturya.fluenta.network.CurriculumUnit("u2", 2, "En el restaurante", "Pedir comida y bebida",
                    listOf(
                        com.alturya.fluenta.network.Lesson("l5", 5, "La carta", "vocab", completed = false),
                        com.alturya.fluenta.network.Lesson("l6", 6, "Hacer un pedido", "grammar", completed = false),
                        com.alturya.fluenta.network.Lesson("l7", 7, "Conversación: Pedir en el café", "conversation", completed = false),
                    )),
                com.alturya.fluenta.network.CurriculumUnit("u3", 3, "Trabajo y rutina", "Hablar de tu día a día",
                    listOf(
                        com.alturya.fluenta.network.Lesson("l8", 8, "Profesiones", "vocab", completed = false),
                        com.alturya.fluenta.network.Lesson("l9", 9, "Presente simple", "grammar", completed = false),
                    )),
            ),
        ))
    }

    // ---------- PRONUNCIACIÓN ----------
    @Test fun pronunciation() = shot("pronunciation") {
        PronunciationScreen(previewState = com.alturya.fluenta.pronunciation.PronState(
            loading = false,
            drill = com.alturya.fluenta.network.PhonemeDrill(
                key = "th_voiced",
                label = "Th sonora",
                symbol = "ð",
                tipEs = "Pon la lengua entre los dientes y vibra como en 'the', 'this', 'that'.",
                phrases = listOf("This is the thing", "That dog is there", "The mother is over there"),
            ),
            source = "top_error",
        ))
    }

    // ---------- VERBOS DEL DÍA ----------
    @Test fun verbs() = shot("verbs") {
        VerbsTodayScreen(previewState = com.alturya.fluenta.verbs.VerbsTodayState(
            loading = false,
            target = 10, achievedToday = 3,
            stats = com.alturya.fluenta.network.VerbStats(total = 340, mastered = 87, exposed = 210),
            verbs = listOf(
                com.alturya.fluenta.network.DailyVerbCard(id = "v1", base = "to go", translation = "ir",
                    isReview = false, alreadyMastered = false,
                    example = "I go to school every day.",
                    variants = com.alturya.fluenta.network.VerbVariants(
                        present = listOf("go", "goes"), past = listOf("went"),
                        future = listOf("will go"), conditional = listOf("would go"),
                        gerund = "going", pastParticiple = "gone")),
                com.alturya.fluenta.network.DailyVerbCard(id = "v2", base = "to speak", translation = "hablar",
                    isReview = true, alreadyMastered = false,
                    example = "She speaks three languages.",
                    variants = com.alturya.fluenta.network.VerbVariants(
                        present = listOf("speak", "speaks"), past = listOf("spoke"),
                        future = listOf("will speak"), conditional = listOf("would speak"),
                        gerund = "speaking", pastParticiple = "spoken")),
                com.alturya.fluenta.network.DailyVerbCard(id = "v3", base = "to eat", translation = "comer",
                    isReview = false, alreadyMastered = true,
                    example = "We eat dinner at 8pm.",
                    variants = com.alturya.fluenta.network.VerbVariants(
                        present = listOf("eat", "eats"), past = listOf("ate"),
                        future = listOf("will eat"), conditional = listOf("would eat"),
                        gerund = "eating", pastParticiple = "eaten")),
            ),
        ))
    }

    // ---------- JUEGO (MATCH) ----------
    @Test fun match() = shot("match") {
        MatchScreen(previewState = com.alturya.fluenta.exercises.MatchState(
            loading = false,
            leftItems = listOf("I'd like", "Could I have", "The bill, please", "How much is it?"),
            rightItems = listOf("¿Me podría traer?", "Quisiera", "¿Cuánto cuesta?", "La cuenta, por favor"),
            answerKey = mapOf(
                "I'd like" to "Quisiera",
                "Could I have" to "¿Me podría traer?",
                "The bill, please" to "La cuenta, por favor",
                "How much is it?" to "¿Cuánto cuesta?",
            ),
            selectedLeft = "I'd like",
            matched = setOf("Could I have"),
            attempts = 2,
        ))
    }

    // ---------- SELECTOR DE IDIOMA ----------
    @Test fun languages() = shot("languages") {
        LanguageSelectorScreen(previewState = com.alturya.fluenta.languages.LanguagesState(
            loading = false,
            pairs = listOf(
                com.alturya.fluenta.network.LanguagePair("es", "en", "tier1", "cefr", null, true, null),
                com.alturya.fluenta.network.LanguagePair("es", "pt", "tier1", "cefr", null, true, null),
                com.alturya.fluenta.network.LanguagePair("es", "fr", "tier1", "cefr", null, true, null),
                com.alturya.fluenta.network.LanguagePair("es", "de", "tier2", "cefr", null, true, null),
                com.alturya.fluenta.network.LanguagePair("es", "it", "tier2", "cefr", null, true, null),
                com.alturya.fluenta.network.LanguagePair("es", "ja", "tier2", "jlpt", "hiragana+katakana+kanji", true, null),
                com.alturya.fluenta.network.LanguagePair("es", "zh", "tier2", "hsk", "hanzi", true, null),
                com.alturya.fluenta.network.LanguagePair("es", "ko", "tier3", "topik", "hangul", false, null),
                com.alturya.fluenta.network.LanguagePair("es", "ar", "tier3", "arabic", "arabic", false, null),
                com.alturya.fluenta.network.LanguagePair("en", "es", "tier1", "cefr", null, true, null),
                com.alturya.fluenta.network.LanguagePair("en", "pt", "tier1", "cefr", null, true, null),
                com.alturya.fluenta.network.LanguagePair("pt", "en", "tier1", "cefr", null, true, null),
            ),
        ))
    }

    // ---------- AJUSTES ----------
    @Test fun settings() = shot("settings") {
        com.alturya.fluenta.settings.SettingsScreen()
    }

    // ---------- DIAGNÓSTICO ----------
    @Test fun diagnostic() = shot("diagnostic") { DiagnosticScreen() }

    // ---------- SCRIPT (no-latinos) ----------
    @Test fun script_ja() = shot("script_ja") {
        ScriptScreen(l2 = "ja", onDone = {}, previewState = com.alturya.fluenta.script.ScriptUiState(
            phase = com.alturya.fluenta.script.ScriptPhase.LEARN,
            l2 = "ja",
            info = com.alturya.fluenta.network.ScriptInfo(l2 = "ja", nonLatin = true, needsScriptFirst = true,
                readingScore = 15, translitMode = "romaji"),
            lesson = com.alturya.fluenta.network.ScriptLesson(
                scriptKey = "hiragana", name = "Hiragana", family = "syllabary",
                writingSupported = true, rtl = false, tonal = false,
                intro = "El hiragana tiene 46 caracteres. Cada uno representa una sílaba. Es el primer script que aprenderás.",
                items = listOf(
                    com.alturya.fluenta.network.ScriptItem("あ", "a", "sonido A", 3, null, "Como la 'a' española", "あめ (ame) = lluvia"),
                    com.alturya.fluenta.network.ScriptItem("い", "i", "sonido I", 2, null, "Como la 'i' española", "いぬ (inu) = perro"),
                    com.alturya.fluenta.network.ScriptItem("う", "u", "sonido U", 2, null, "Como la 'u' española", "うみ (umi) = mar"),
                    com.alturya.fluenta.network.ScriptItem("え", "e", "sonido E", 2, null, "Como la 'e' española", "えき (eki) = estación"),
                    com.alturya.fluenta.network.ScriptItem("お", "o", "sonido O", 3, null, "Como la 'o' española", "おかあさん = mamá"),
                ),
            ),
        ))
    }
    @Test fun script_zh() = shot("script_zh") {
        ScriptScreen(l2 = "zh", onDone = {}, previewState = com.alturya.fluenta.script.ScriptUiState(
            phase = com.alturya.fluenta.script.ScriptPhase.LEARN,
            l2 = "zh",
            info = com.alturya.fluenta.network.ScriptInfo(l2 = "zh", nonLatin = true, needsScriptFirst = true,
                readingScore = 5, translitMode = "pinyin"),
            lesson = com.alturya.fluenta.network.ScriptLesson(
                scriptKey = "hanzi_basic", name = "Hanzi básicos", family = "logographic",
                writingSupported = true, rtl = false, tonal = true,
                intro = "El chino usa caracteres llamados 汉字 (hànzì). Aprenderás los más frecuentes primero.",
                items = listOf(
                    com.alturya.fluenta.network.ScriptItem("人", "rén", "persona", 2, null, "Parece una persona caminando", "这个人 = esta persona"),
                    com.alturya.fluenta.network.ScriptItem("大", "dà", "grande", 3, null, "Una persona con brazos extendidos", "大学 = universidad"),
                    com.alturya.fluenta.network.ScriptItem("小", "xiǎo", "pequeño", 3, null, "Algo pequeño en el centro", "小心 = cuidado"),
                ),
            ),
        ))
    }
    @Test fun script_ar() = shot("script_ar") {
        ScriptScreen(l2 = "ar", onDone = {}, previewState = com.alturya.fluenta.script.ScriptUiState(
            phase = com.alturya.fluenta.script.ScriptPhase.LEARN,
            l2 = "ar",
            info = com.alturya.fluenta.network.ScriptInfo(l2 = "ar", nonLatin = true, needsScriptFirst = true,
                readingScore = 0, translitMode = "transliteration"),
            lesson = com.alturya.fluenta.network.ScriptLesson(
                scriptKey = "arabic_basic", name = "Alfabeto árabe", family = "abjad",
                writingSupported = false, rtl = true, tonal = false,
                intro = "El árabe se escribe de derecha a izquierda. Tiene 28 letras, cada una con hasta 4 formas según su posición.",
                items = listOf(
                    com.alturya.fluenta.network.ScriptItem("ا", "alif / a", "letra A", 1, null, "La primera letra del alfabeto", "أب (ab) = padre"),
                    com.alturya.fluenta.network.ScriptItem("ب", "ba / b", "letra B", 2, null, "Un punto debajo", "باب (bab) = puerta"),
                    com.alturya.fluenta.network.ScriptItem("ت", "ta / t", "letra T", 2, null, "Dos puntos arriba", "تمر (tamr) = dátil"),
                ),
            ),
        ))
    }
}
