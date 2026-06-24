package com.alturya.fluenta.lesson

// Play-first onboarding: a full lesson playable WITHOUT logging in, in the exact
// language pair the user picked during onboarding. Calls GET /api/guest/lesson
// (no auth). The guest endpoint does NOT return correct answers, so we do NOT
// fake a "correct/wrong" verdict — this is an honest guided taste of the product.
// After it, postLessonCta drives registration. Max 3 lessons per IP/hour (server).
//
// Diseño: kit Claude Design (GuestLesson.dc.html) — barra ✕ + progreso, prompt CJK
// grande con pinyin ámbar, panel de acierto con mascota, muro de registro con
// gradiente hero. Cableado (GuestLessonViewModel) intacto.

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alturya.fluenta.R
import com.alturya.fluenta.data.Analytics
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.network.ApiClient
import com.alturya.fluenta.network.GuestPostLessonCta
import com.alturya.fluenta.network.LessonHeader
import com.alturya.fluenta.network.PlayableExercise
import com.alturya.fluenta.ui.FluentaButton
import com.alturya.fluenta.ui.FluentaButtonStyle
import com.alturya.fluenta.ui.theme.FluentaTokens
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GuestLessonState(
    val loading: Boolean = true,
    val error: String? = null,
    val lesson: LessonHeader? = null,
    val exercises: List<PlayableExercise> = emptyList(),
    val introMessage: String? = null,
    val cta: GuestPostLessonCta? = null,
    val currentIndex: Int = 0,
    val answered: Boolean = false,
    val done: Boolean = false,
)

class GuestLessonViewModel(
    private val l1: String,
    private val l2: String,
) : ViewModel() {
    private val _state = MutableStateFlow(GuestLessonState())
    val state = _state.asStateFlow()

    init { loadLesson() }

    private fun loadLesson() {
        viewModelScope.launch {
            try {
                val res = ApiClient.apiNoAuth.getGuestLesson(l1 = l1, l2 = l2)
                _state.value = GuestLessonState(
                    loading = false,
                    lesson = res.lesson,
                    exercises = res.exercises,
                    introMessage = res.introMessage,
                    cta = res.postLessonCta,
                )
            } catch (e: Exception) {
                _state.value = GuestLessonState(
                    loading = false,
                    error = I18nStore.t("guest.loadError", "No se pudo cargar la lección. Verifica tu conexión."),
                )
            }
        }
    }

    fun retry() {
        _state.value = GuestLessonState(loading = true)
        loadLesson()
    }

    // No server-side grading for guests (the endpoint doesn't expose answers), so
    // we simply advance — the value is accepted as the user's attempt.
    fun submit(@Suppress("UNUSED_PARAMETER") value: String) {
        _state.value = _state.value.copy(answered = true)
    }

    fun continueLesson() {
        val s = _state.value
        val next = s.currentIndex + 1
        _state.value = if (next >= s.exercises.size) s.copy(answered = false, done = true)
        else s.copy(answered = false, currentIndex = next)
    }

    class Factory(private val l1: String, private val l2: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            GuestLessonViewModel(l1, l2) as T
    }
}

@Composable
fun GuestLessonScreen(l1: String, l2: String, onSignUp: () -> Unit, onBack: () -> Unit) {
    val vm: GuestLessonViewModel = viewModel(
        key = "guest_${l1}_$l2",
        factory = GuestLessonViewModel.Factory(l1, l2),
    )
    val state by vm.state.collectAsState()

    // fluenta_events — ACTIVACIÓN (fuga #1 del funnel, ~4% llega a lección). Antes
    // esta pantalla NO emitía nada, así que "llegar a la 1ª lección" era invisible.
    // lesson_start (guest) = el numerador real de la activación; lesson_complete al
    // terminar la prueba. Props l1/l2 para segmentar por par (es→zh = flagship).
    val ctx = LocalContext.current
    LaunchedEffect(Unit) {
        Analytics.track(ctx, Analytics.LESSON_START, mapOf("guest" to "true", "l1" to l1, "l2" to l2))
    }
    LaunchedEffect(state.done) {
        if (state.done) Analytics.track(ctx, Analytics.LESSON_COMPLETE, mapOf("guest" to "true", "l1" to l1, "l2" to l2))
    }

    Box(Modifier.fillMaxSize().background(FluentaTokens.Surface)) {
        when {
            state.loading -> LoadingView()
            state.error != null -> ErrorView(message = state.error!!, onRetry = vm::retry, onBack = onBack)
            state.done -> GuestResultWall(cta = state.cta, onSignUp = onSignUp, onBack = onBack)
            state.exercises.isEmpty() -> EmptyView(onBack = onBack)
            else -> GuestQuizView(state, vm::submit, vm::continueLesson, onBack)
        }
    }
}

@Composable
private fun GuestQuizView(
    state: GuestLessonState,
    onSubmit: (String) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
) {
    val ex = state.exercises.getOrNull(state.currentIndex) ?: return
    val total = state.exercises.size
    val progressFraction = (state.currentIndex + if (state.answered) 1 else 0).toFloat() / total.toFloat()

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        // ── Barra: ✕ + progreso ───────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.Close, contentDescription = I18nStore.t("common.back", "Volver"), tint = FluentaTokens.Muted)
            }
            Spacer(Modifier.width(4.dp))
            LinearProgressIndicator(
                progress = { progressFraction.coerceIn(0f, 1f) },
                modifier = Modifier.weight(1f).height(14.dp).clip(RoundedCornerShape(99.dp)),
                color = FluentaTokens.Primary,
                trackColor = FluentaTokens.Container,
            )
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 26.dp)) {
            Spacer(Modifier.height(20.dp))
            GuestExercise(ex = ex, onSubmit = onSubmit, answered = state.answered)
        }

        if (state.answered) {
            SuccessPanel(onContinue = onContinue)
        }
    }
}

@Composable
private fun GuestExercise(ex: PlayableExercise, onSubmit: (String) -> Unit, answered: Boolean) {
    var selected by remember(ex.index) { mutableStateOf(-1) }
    var textInput by remember(ex.index) { mutableStateOf("") }

    when (ex.kind) {
        "multiple_choice", "listen_select" -> {
            val options = ex.options ?: emptyList()
            SectionLabel(I18nStore.t("guest.chooseTranslation", "Elige la traducción"))
            Spacer(Modifier.height(18.dp))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    ex.prompt ?: ex.audioText ?: "",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = FluentaTokens.Ink,
                    textAlign = TextAlign.Center,
                )
                Pinyin(ex.transliteration)
            }
            Spacer(Modifier.height(26.dp))
            options.forEachIndexed { idx, opt ->
                OptionCard(text = opt, selected = selected == idx, enabled = !answered) { selected = idx }
                Spacer(Modifier.height(13.dp))
            }
            Spacer(Modifier.height(8.dp))
            FluentaButton(
                text = I18nStore.t("lesson.check", "Comprobar"),
                onClick = { onSubmit(selected.toString()) },
                enabled = selected >= 0 && !answered,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(20.dp))
        }
        "translate_l1_to_l2", "translate_l2_to_l1", "fill_blank" -> {
            SectionLabel(I18nStore.t("guest.translatePhrase", "Traduce esta frase"))
            Spacer(Modifier.height(16.dp))
            Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(FluentaTokens.Container),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = FluentaTokens.BrandText, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(ex.prompt ?: "", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = FluentaTokens.Ink)
                        Pinyin(ex.transliteration)
                    }
                }
            }
            ex.hint?.let { hint ->
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = FluentaTokens.Amber, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(hint, style = MaterialTheme.typography.bodySmall, color = FluentaTokens.Muted)
                }
            }
            Spacer(Modifier.height(20.dp))
            Surface(shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(2.dp, FluentaTokens.Border), modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.padding(16.dp)) {
                    if (textInput.isEmpty()) {
                        Text(I18nStore.t("lesson.yourAnswer", "Tu respuesta"), style = MaterialTheme.typography.bodyLarge, color = FluentaTokens.Muted)
                    }
                    BasicTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        singleLine = true,
                        enabled = !answered,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = FluentaTokens.Ink),
                        cursorBrush = SolidColor(FluentaTokens.Primary),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            FluentaButton(
                text = I18nStore.t("lesson.check", "Comprobar"),
                onClick = { onSubmit(textInput.trim()) },
                enabled = textInput.isNotBlank() && !answered,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(20.dp))
        }
        else -> {
            Text(ex.prompt ?: ex.audioText ?: "", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = FluentaTokens.Ink)
            Pinyin(ex.transliteration)
            Spacer(Modifier.height(20.dp))
            FluentaButton(
                text = I18nStore.t("common.continue", "Continuar"),
                onClick = { onSubmit("ok") },
                enabled = !answered,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun OptionCard(text: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) FluentaTokens.Container else Color.White,
        border = BorderStroke(2.dp, if (selected) FluentaTokens.Primary else FluentaTokens.Border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 17.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = FluentaTokens.Ink,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 0.5.sp,
        color = FluentaTokens.BrandText,
    )
}

// Pinyin/romaji bajo el prompt para scripts no latinos (zh/ja/ko/ar…). Solo
// aparece si el backend manda la lectura.
@Composable
private fun Pinyin(reading: String?) {
    if (reading.isNullOrBlank()) return
    Spacer(Modifier.height(6.dp))
    Text(
        reading,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = FluentaTokens.AmberInk,
    )
}

@Composable
private fun SuccessPanel(onContinue: () -> Unit) {
    Surface(
        color = FluentaTokens.SuccessBg,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(start = 26.dp, end = 26.dp, top = 22.dp, bottom = 26.dp).navigationBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.ic_fluenta_celebra),
                    contentDescription = null,
                    modifier = Modifier.size(58.dp),
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        I18nStore.t("guest.wellDone", "¡Bien hecho!"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = FluentaTokens.SuccessInk,
                    )
                    Text(
                        I18nStore.t("guest.keepGoing", "Sigamos aprendiendo."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = FluentaTokens.SuccessInk,
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            FluentaButton(
                text = I18nStore.t("common.continue", "Continuar"),
                onClick = onContinue,
                style = FluentaButtonStyle.Success,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun GuestResultWall(cta: GuestPostLessonCta?, onSignUp: () -> Unit, onBack: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Column(
        Modifier
            .fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    0f to FluentaTokens.Primary,
                    0.7f to FluentaTokens.Container,
                    1f to FluentaTokens.Surface,
                )
            )
            .statusBarsPadding(),
    ) {
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedVisibility(visible = visible, enter = fadeIn() + scaleIn()) {
                Image(
                    painter = painterResource(R.drawable.ic_fluenta_celebra),
                    contentDescription = null,
                    modifier = Modifier.size(132.dp),
                )
            }
            Spacer(Modifier.height(22.dp))
            Text(
                cta?.title ?: I18nStore.t("guest.done", "¡Tu primera lección completada!"),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = FluentaTokens.Ink,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                cta?.body ?: I18nStore.t("guest.ctaBody", "Crea una cuenta gratis para guardar tu progreso y seguir aprendiendo."),
                style = MaterialTheme.typography.titleMedium,
                color = FluentaTokens.Ink.copy(alpha = 0.78f),
                textAlign = TextAlign.Center,
            )
        }
        Column(Modifier.padding(26.dp).navigationBarsPadding()) {
            FluentaButton(
                text = cta?.ctaLabel ?: I18nStore.t("guest.ctaButton", "Crear cuenta gratis"),
                onClick = onSignUp,
                style = FluentaButtonStyle.Ink,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text(
                    I18nStore.t("guest.maybeLater", "Quizás más tarde"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = FluentaTokens.BrandText,
                )
            }
        }
    }
}

@Composable
private fun LoadingView() {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = FluentaTokens.Primary, trackColor = FluentaTokens.Container, strokeWidth = 5.dp, modifier = Modifier.size(54.dp))
        Spacer(Modifier.height(22.dp))
        Text(
            I18nStore.t("guest.preparing", "Preparando tu lección…"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = FluentaTokens.Muted,
        )
    }
}

@Composable
private fun EmptyView(onBack: () -> Unit) {
    CenteredMessage(
        emoji = "📭",
        title = I18nStore.t("guest.emptyTitle", "Aún no hay ejercicios"),
        body = I18nStore.t("guest.emptyBody", "Esta lección de prueba todavía no está disponible. Vuelve pronto."),
    ) {
        FluentaButton(
            text = I18nStore.t("guest.chooseOther", "Elegir otro idioma"),
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit, onBack: () -> Unit) {
    CenteredMessage(
        image = R.drawable.ic_fluenta_hola,
        title = I18nStore.t("guest.errorTitle", "No pudimos cargar la lección"),
        body = message,
    ) {
        FluentaButton(
            text = I18nStore.t("common.retry", "Reintentar"),
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(I18nStore.t("common.back", "Volver"), color = FluentaTokens.Muted)
        }
    }
}

@Composable
private fun CenteredMessage(
    emoji: String? = null,
    image: Int? = null,
    title: String,
    body: String,
    actions: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when {
            image != null -> Image(painterResource(image), contentDescription = null, modifier = Modifier.size(104.dp))
            emoji != null -> Box(
                Modifier.size(96.dp).clip(CircleShape).background(Color(0xFFE3EFEA)),
                contentAlignment = Alignment.Center,
            ) { Text(emoji, fontSize = 42.sp) }
        }
        Spacer(Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = FluentaTokens.Ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = FluentaTokens.Muted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        actions()
    }
}
