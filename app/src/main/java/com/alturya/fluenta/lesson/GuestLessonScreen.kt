package com.alturya.fluenta.lesson

// Play-first onboarding: a full lesson playable WITHOUT logging in, in the exact
// language pair the user picked during onboarding. Calls GET /api/guest/lesson
// (no auth). The guest endpoint does NOT return correct answers, so we do NOT
// fake a "correct/wrong" verdict — this is an honest guided taste of the product.
// After it, postLessonCta drives registration. Max 3 lessons per IP/hour (server).

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(8.dp))
                Text(state.error!!, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(20.dp))
                Button(onClick = onBack) { Text(I18nStore.t("common.back", "Volver")) }
            }
            state.done -> GuestResultView(
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
            IconButton(onClick = onBack) { Icon(Icons.Default.Close, contentDescription = I18nStore.t("common.back", "Volver"), modifier = Modifier.size(20.dp)) }
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

        GuestExercise(ex = ex, onSubmit = onSubmit)

        Spacer(Modifier.weight(1f))

        if (state.answered) {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "👍 ${I18nStore.t("guest.keepGoing", "¡Bien! Sigamos")}",
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
            ex.hint?.let { hint ->
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(hint, style = MaterialTheme.typography.bodySmall)
                }
            }
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
            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(80.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(
            cta?.title ?: I18nStore.t("guest.done", "¡Primera lección completada!"),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            I18nStore.t("guest.tasteSubtitle", "Así se siente aprender con Fluenta."),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
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
            Text(I18nStore.t("common.back", "Volver"))
        }
    }
}
