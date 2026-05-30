package com.alturya.fluenta.lesson

// Play-first onboarding: a full lesson playable WITHOUT logging in.
// Calls GET /api/guest/lesson (no auth token). Shows postLessonCta after
// completion to drive registration. Max 3 lessons per IP/hour (server enforced).

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.network.ApiClient
import com.alturya.fluenta.network.GuestPostLessonCta
import com.alturya.fluenta.network.LessonHeader
import com.alturya.fluenta.network.PlayableExercise
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
    val feedback: Boolean? = null, // true=correct, false=wrong
    val done: Boolean = false,
    val correctCount: Int = 0,
)

class GuestLessonViewModel : ViewModel() {
    private val _state = MutableStateFlow(GuestLessonState())
    val state = _state.asStateFlow()

    init { loadLesson() }

    private fun loadLesson() {
        viewModelScope.launch {
            try {
                val res = ApiClient.apiNoAuth.getGuestLesson(l1 = "en", l2 = "es")
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
                    error = "No se pudo cargar la lección. Verifica tu conexión.",
                )
            }
        }
    }

    fun submit(value: String) {
        val s = _state.value
        val ex = s.exercises.getOrNull(s.currentIndex) ?: return
        // Local grading for guest (no server round-trip needed — no auth)
        val correct = when (ex.kind) {
            "multiple_choice", "listen_select" -> {
                val picked = value.toIntOrNull() ?: -1
                picked >= 0 // guest: always accept any selection as "ok" — just show answer
            }
            "match_pairs" -> true // guest: accept anything
            else -> value.isNotBlank()
        }
        val newCorrect = if (correct) s.correctCount + 1 else s.correctCount
        _state.value = s.copy(feedback = correct, correctCount = newCorrect)
    }

    fun continueLesson() {
        val s = _state.value
        val next = s.currentIndex + 1
        if (next >= s.exercises.size) {
            _state.value = s.copy(feedback = null, done = true)
        } else {
            _state.value = s.copy(feedback = null, currentIndex = next)
        }
    }
}

@Composable
fun GuestLessonScreen(onSignUp: () -> Unit, onBack: () -> Unit) {
    val vm: GuestLessonViewModel = viewModel()
    val state by vm.state.collectAsState()

    Box(Modifier.fillMaxSize()) {
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.error != null -> Column(
                Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("⚠️ ${state.error}", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(20.dp))
                Button(onClick = onBack) { Text(I18nStore.t("common.back", "Volver")) }
            }
            state.done -> GuestResultView(
                correctCount = state.correctCount,
                total = state.exercises.size,
                cta = state.cta,
                onSignUp = onSignUp,
                onBack = onBack,
            )
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
    val progressFraction = state.currentIndex.toFloat() / total.toFloat()

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("✕") }
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier.weight(1f).height(8.dp),
            )
            Text(
                "${state.currentIndex + 1}/$total",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        state.introMessage?.let {
            Text(it, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
        }

        // Reuse the same exercise composables from LessonPlayerScreen
        // by delegating to a simplified version here for guest mode
        GuestExercise(ex = ex, onSubmit = onSubmit)

        Spacer(Modifier.weight(1f))

        if (state.feedback != null) {
            val correct = state.feedback
            Surface(
                color = if (correct) MaterialTheme.colorScheme.tertiaryContainer
                else MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        if (correct) "✅ ${I18nStore.t("lesson.correct", "¡Correcto!")}"
                        else "💪 ${I18nStore.t("lesson.keepGoing", "¡Sigue intentando!")}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                        Text(I18nStore.t("common.continue", "Continuar"))
                    }
                }
            }
        }
    }
}

@Composable
private fun GuestExercise(ex: PlayableExercise, onSubmit: (String) -> Unit) {
    // Simplified exercise view for guest mode — no server check, just tap & continue
    var selected by remember(ex.index) { mutableStateOf(-1) }
    var textInput by remember(ex.index) { mutableStateOf("") }

    when (ex.kind) {
        "multiple_choice", "listen_select" -> {
            val options = ex.options ?: emptyList()
            Text(
                ex.prompt ?: ex.audioText ?: "",
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(16.dp))
            options.forEachIndexed { idx, opt ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected == idx) MaterialTheme.colorScheme.tertiaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    onClick = { selected = idx },
                ) { Text(opt, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge) }
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { onSubmit(selected.toString()) },
                enabled = selected >= 0,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text(I18nStore.t("lesson.check", "Comprobar")) }
        }
        "translate_l1_to_l2", "translate_l2_to_l1", "fill_blank" -> {
            Text(ex.prompt ?: "", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            ex.hint?.let { Spacer(Modifier.height(8.dp)); Text("💡 $it", style = MaterialTheme.typography.bodySmall) }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                label = { Text(I18nStore.t("lesson.yourAnswer", "Tu respuesta")) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { onSubmit(textInput.trim()) },
                enabled = textInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text(I18nStore.t("lesson.check", "Comprobar")) }
        }
        else -> {
            Text(ex.prompt ?: ex.audioText ?: "", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(20.dp))
            Button(onClick = { onSubmit("ok") }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(I18nStore.t("common.continue", "Continuar"))
            }
        }
    }
}

@Composable
private fun GuestResultView(
    correctCount: Int,
    total: Int,
    cta: GuestPostLessonCta?,
    onSignUp: () -> Unit,
    onBack: () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        AnimatedVisibility(visible = visible, enter = fadeIn() + scaleIn()) {
            Text("🎉", style = MaterialTheme.typography.displayLarge)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            cta?.title ?: I18nStore.t("guest.done", "¡Primera lección completada!"),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text("$correctCount/$total ${I18nStore.t("guest.correct", "correctas")}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(24.dp))

        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    cta?.body ?: I18nStore.t("guest.ctaBody", "Crea tu cuenta gratis para guardar tu progreso y seguir aprendiendo."),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = onSignUp, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text(cta?.ctaLabel ?: I18nStore.t("guest.ctaButton", "Crear cuenta gratis →"),
                        fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onBack) {
            Text(I18nStore.t("common.back", "Volver al inicio"))
        }
    }
}
