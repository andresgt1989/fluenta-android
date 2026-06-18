package com.alturya.fluenta.lesson

import android.content.Intent
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.sin
import kotlin.math.cos
import kotlin.random.Random
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBounce
import androidx.compose.animation.core.EaseOutElastic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.alturya.fluenta.R
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.audio.Sfx
import com.alturya.fluenta.audio.TtsPlayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alturya.fluenta.network.ApiClient
import com.alturya.fluenta.network.PlayableExercise
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

@Composable
fun LessonPlayerScreen(lessonId: String, onDone: () -> Unit, previewState: LessonPlayerState? = null) {
    val vm: LessonPlayerViewModel = viewModel(
        key = "lesson_$lessonId",
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                LessonPlayerViewModel(
                    androidx.lifecycle.SavedStateHandle(mapOf("lessonId" to lessonId))
                ) as T
        }
    )
    val vmState by vm.state.collectAsState()
    val state = previewState ?: vmState
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.error != null -> ErrorView(state.error!!, onRetry = vm::retry, onBack = onDone)
            state.result != null -> ResultView(
                result = state.result!!,
                onContinueOnWhatsApp = {
                    scope.launch {
                        try {
                            // intent=lesson_done makes the bot apply THIS lesson's vocab/grammar
                            // in a real-conversation roleplay immediately on landing.
                            val ho = ApiClient.api.getWhatsAppHandoff(
                                intent = "lesson_done",
                                lessonId = lessonId,
                            )
                            ho.url?.let { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }
                        } catch (_: Exception) { /* user can press WhatsApp from elsewhere */ }
                    }
                },
                onDone = onDone,
            )
            state.exercises.isEmpty() -> ErrorView(I18nStore.t("lesson.noExercises", "Esta lección aún no tiene ejercicios disponibles."), onRetry = vm::retry, onBack = onDone)
            !state.teachDone -> TeachView(state.teach, onDone = vm::finishTeach)
            else -> QuizView(state, vm)
        }
    }
}

@Composable
private fun TeachView(items: List<com.alturya.fluenta.network.TeachItem>, onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text(
            I18nStore.t("lesson.teachTitle", "Vocabulario nuevo"),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                I18nStore.t("lesson.teachSubtitle", "Toca una tarjeta para escucharla. Luego practicamos."),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(16.dp))
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 8.dp),
        ) {
            items(items.size) { i ->
                val t = items[i]
                Card(
                    onClick = { scope.launch { TtsPlayer.play(context, t.l2) } },
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 100.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(t.l2, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(24.dp))
                        }
                        t.transliteration?.takeIf { it.isNotBlank() }?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(t.l1, style = MaterialTheme.typography.bodyLarge)
                        t.note?.takeIf { it.isNotBlank() }?.let {
                            Spacer(Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        com.alturya.fluenta.ui.FluentaButton(
            text = I18nStore.t("lesson.teachStart", "¡Listo, a practicar!"),
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun QuizView(state: LessonPlayerState, vm: LessonPlayerViewModel) {
    val ex = state.exercises.getOrNull(state.currentIndex) ?: return
    val total = state.exercises.size
    val progressFraction = state.currentIndex.toFloat() / total.toFloat()

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(I18nStore.t("lesson.exerciseOf", "Ejercicio {n} de {total}").replace("{n}", "${state.currentIndex + 1}").replace("{total}", "$total"), style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.weight(1f))
            // Combo de aciertos seguidos — refuerzo positivo (gamificación).
            if (state.combo >= 2) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.padding(end = 8.dp),
                ) {
                    Text(
                        "🔥 ${state.combo}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
            Text(
                "❤️".repeat(state.hearts) + "🤍".repeat((5 - state.hearts).coerceAtLeast(0)),
                style = MaterialTheme.typography.labelMedium,
            )
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progressFraction },
            modifier = Modifier.fillMaxWidth().height(8.dp),
        )
        Spacer(Modifier.height(20.dp))

        AnimatedContent(
            targetState = ex.index,
            transitionSpec = {
                (slideInHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { it / 2 } + fadeIn(tween(200))) togetherWith
                    (slideOutHorizontally(tween(180)) { -it / 2 } + fadeOut(tween(150)))
            },
            label = "exercise",
        ) { _ ->
            when (ex.kind) {
                "translate_l1_to_l2", "translate_l2_to_l1" -> TranslateExercise(
                    ex = ex,
                    teachItems = state.teach,
                    onSubmit = { v -> vm.checkAnswer(v) },
                )
                "multiple_choice" -> MultipleChoiceExercise(ex, onSubmit = { v ->
                    vm.checkAnswer(v)
                })
                "match_pairs" -> MatchPairsExercise(ex, onSubmit = { v ->
                    vm.checkAnswer(v)
                })
                "word_order" -> WordOrderExercise(ex, onSubmit = { v ->
                    vm.checkAnswer(v)
                })
                "fill_blank" -> FillBlankExercise(ex, onSubmit = { v ->
                    vm.checkAnswer(v)
                })
                "listen_select" -> ListenSelectExercise(ex, onSubmit = { v ->
                    vm.checkAnswer(v)
                })
                "speak_repeat" -> SpeakRepeatExercise(ex, onSubmit = { v ->
                    vm.checkAnswer(v)
                })
                else -> Text(I18nStore.t("lesson.unsupportedType", "Tipo no soportado: {k}").replace("{k}", ex.kind), style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(Modifier.weight(1f))
        val fb = state.feedback
        AnimatedVisibility(
            visible = fb != null,
            enter = slideInVertically(tween(280)) { it } + fadeIn(tween(200)),
            exit = slideOutVertically(tween(220)) { it } + fadeOut(tween(150)),
        ) {
            if (fb != null) {
                FeedbackBar(
                    fb = fb,
                    showExpected = ex.kind != "match_pairs",
                    canRetry = !fb.correct && state.hearts > 0,
                    onRetry = vm::retryAfterFeedback,
                    onContinue = vm::continueAfterFeedback,
                )
            }
        }
        if (fb == null) {
            TextButton(onClick = vm::skip, enabled = !state.checking) { Text(I18nStore.t("lesson.skip", "Saltar este")) }
        }
    }
}

@Composable
private fun FeedbackBar(
    fb: com.alturya.fluenta.network.ExerciseCheckResponse,
    showExpected: Boolean,
    canRetry: Boolean = false,
    onRetry: () -> Unit = {},
    onContinue: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val iconScale = remember { Animatable(0f) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(fb) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (fb.correct) Sfx.correct() else Sfx.wrong()
        // Bounce animation for the feedback icon
        iconScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
    }

    val correct = fb.correct
    // Verde para correcto (semántico) pero adaptado a dark mode: en tema oscuro el
    // verde claro fijo se veía fuera de lugar; usamos un verde que contrasta en ambos.
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val container = if (correct) {
        if (isDark) Color(0xFF14532D) else Color(0xFFD7F5DD)
    } else MaterialTheme.colorScheme.errorContainer
    val accent = if (correct) {
        if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D)
    } else MaterialTheme.colorScheme.error

    Surface(color = container, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (correct) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(32.dp).scale(iconScale.value),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (correct) I18nStore.t("lesson.correct", "¡Correcto!") else I18nStore.t("lesson.almost", "Casi…"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                )
            }
            if (!correct && showExpected) {
                Spacer(Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${I18nStore.t("lesson.answerLabel", "Respuesta correcta")}: ${fb.expected}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                        )
                        // Hear the correct L2 answer — key for actually learning pronunciation.
                        IconButton(onClick = {
                            scope.launch { TtsPlayer.play(context, fb.expected, com.alturya.fluenta.data.Session.l2 ?: "en") }
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = I18nStore.t("lesson.hearAnswer", "Escuchar respuesta"),
                                tint = accent,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
            fb.feedback?.let { tip ->
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                I18nStore.t("lesson.grammarTip", "¿Por qué?"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = accent,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(tip, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            if (!correct) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        I18nStore.t("lesson.savedToReview", "Guardado en tu repaso para no olvidarlo"),
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            val contStyle = if (correct) com.alturya.fluenta.ui.FluentaButtonStyle.Success
                            else com.alturya.fluenta.ui.FluentaButtonStyle.Danger
            if (canRetry) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    com.alturya.fluenta.ui.FluentaButton(
                        text = I18nStore.t("lesson.retry", "Reintentar"),
                        onClick = onRetry,
                        style = com.alturya.fluenta.ui.FluentaButtonStyle.Neutral,
                        modifier = Modifier.weight(1f),
                    )
                    com.alturya.fluenta.ui.FluentaButton(
                        text = I18nStore.t("common.continue", "Continuar"),
                        onClick = onContinue,
                        style = contStyle,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                com.alturya.fluenta.ui.FluentaButton(
                    text = I18nStore.t("common.continue", "Continuar"),
                    onClick = onContinue,
                    style = contStyle,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun TransliterationText(reading: String?) {
    if (reading.isNullOrBlank()) return
    Spacer(Modifier.height(2.dp))
    Text(
        reading,
        style = MaterialTheme.typography.bodyMedium,
        fontStyle = FontStyle.Italic,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun TranslateExercise(
    ex: PlayableExercise,
    teachItems: List<com.alturya.fluenta.network.TeachItem> = emptyList(),
    onSubmit: (String) -> Unit,
) {
    var text by rememberSaveable(ex.index) { mutableStateOf("") }
    var vocabExpanded by rememberSaveable(ex.index) { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // When translating FROM L2, the prompt is in the target language — let the user hear it.
    val promptIsL2 = ex.kind == "translate_l2_to_l1"

    Column {
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    I18nStore.t("lesson.translatePrompt", "Traduce esto:"),
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        ex.prompt ?: "",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    if (promptIsL2 && !ex.prompt.isNullOrBlank()) {
                        IconButton(onClick = {
                            scope.launch { TtsPlayer.play(context, ex.prompt!!, com.alturya.fluenta.data.Session.l2 ?: "en") }
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = I18nStore.t("lesson.hearPrompt", "Escuchar"),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
                TransliterationText(ex.transliteration)
                ex.hint?.let {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        // Collapsible vocab reference — lets students look back without leaving the quiz
        if (teachItems.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { vocabExpanded = !vocabExpanded }.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            I18nStore.t("lesson.vocabHintTap", "Vocabulario — toca una palabra para usarla"),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            if (vocabExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    AnimatedVisibility(visible = vocabExpanded) {
                        Column(Modifier.padding(horizontal = 14.dp).padding(bottom = 10.dp)) {
                            HorizontalDivider()
                            Spacer(Modifier.height(8.dp))
                            teachItems.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .clickable {
                                            // Tap a word to build the answer — scaffolding for A1 beginners.
                                            text = (text.trim() + " " + item.l2).trim()
                                        }
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(item.l2, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Spacer(Modifier.width(8.dp))
                                    Text(item.l1, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(I18nStore.t("lesson.yourAnswer", "Tu respuesta")) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onSubmit(text.trim())
            }),
            singleLine = true,
        )
        Spacer(Modifier.height(20.dp))
        com.alturya.fluenta.ui.FluentaButton(
            text = I18nStore.t("lesson.check", "Comprobar"),
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onSubmit(text.trim())
            },
            enabled = text.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun FillBlankExercise(ex: PlayableExercise, onSubmit: (String) -> Unit) {
    var text by rememberSaveable(ex.index) { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current

    Column {
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    I18nStore.t("lesson.fillPrompt", "Completa la frase:"),
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    ex.prompt ?: "",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                TransliterationText(ex.transliteration)
                ex.hint?.let {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(I18nStore.t("lesson.yourAnswer", "Tu respuesta")) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onSubmit(text.trim())
            }),
            singleLine = true,
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onSubmit(text.trim())
            },
            enabled = text.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text(I18nStore.t("lesson.check", "Comprobar")) }
    }
}

@Composable
private fun MultipleChoiceExercise(ex: PlayableExercise, onSubmit: (String) -> Unit) {
    val options = ex.options ?: emptyList()
    var selected by rememberSaveable(ex.index) { mutableStateOf(-1) }
    val haptic = LocalHapticFeedback.current

    Column {
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    ex.prompt ?: "",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                )
                TransliterationText(ex.transliteration)
            }
        }
        Spacer(Modifier.height(16.dp))
        options.forEachIndexed { idx, opt ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selected == idx) MaterialTheme.colorScheme.tertiaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
                ),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    selected = idx
                },
            ) {
                Text(opt, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
            }
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { onSubmit(selected.toString()) },
            enabled = selected >= 0,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text(I18nStore.t("lesson.check", "Comprobar")) }
    }
}

@Composable
private fun MatchPairsExercise(ex: PlayableExercise, onSubmit: (String) -> Unit) {
    val left = ex.left ?: emptyList()
    val right = ex.right ?: emptyList()
    var selectedLeft by rememberSaveable(ex.index) { mutableStateOf<String?>(null) }
    val matched = rememberSaveable(ex.index, saver = androidx.compose.runtime.saveable.listSaver(
        save = { it.flatMap { p -> listOf(p.first, p.second) } },
        restore = { list ->
            mutableStateListOf<Pair<String, String>>().apply {
                for (i in list.indices step 2) if (i + 1 < list.size) add(list[i] as String to list[i + 1] as String)
            }
        }
    )) { mutableStateListOf<Pair<String, String>>() }
    val haptic = LocalHapticFeedback.current

    Column {
        Text(
            I18nStore.t("match.title", "Empareja"),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                left.forEachIndexed { li, l ->
                    val isMatched = matched.any { it.first == l }
                    val isSelected = selectedLeft == l
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                isMatched -> Color(0xFF22C55E)
                                isSelected -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                        enabled = !isMatched,
                        onClick = { selectedLeft = if (isSelected) null else l },
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(l, style = MaterialTheme.typography.bodyMedium)
                            ex.leftTransliteration?.getOrNull(li)?.takeIf { it.isNotBlank() }?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
                            }
                        }
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                right.forEach { r ->
                    val isMatched = matched.any { it.second == r }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isMatched) Color(0xFF22C55E)
                            else MaterialTheme.colorScheme.surfaceVariant,
                        ),
                        enabled = !isMatched,
                        onClick = {
                            if (selectedLeft != null) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                matched.add(selectedLeft!! to r)
                                selectedLeft = null
                            }
                        },
                    ) { Text(r, Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium) }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                val arr = JSONArray()
                matched.forEach { (l, r) ->
                    arr.put(JSONObject().put("l2", l).put("l1", r))
                }
                onSubmit(arr.toString())
            },
            enabled = matched.size == left.size && left.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text(I18nStore.t("lesson.check", "Comprobar")) }
    }
}

@Composable
private fun SpeakRepeatExercise(ex: PlayableExercise, onSubmit: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var recording by remember { mutableStateOf(false) }
    var assessing by remember { mutableStateOf(false) }
    var score by remember { mutableStateOf<Int?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recFile by remember { mutableStateOf<File?>(null) }

    val phrase = ex.phrase ?: ex.prompt ?: ""
    val l2 = com.alturya.fluenta.data.Session.l2 ?: "en"

    // Cleanup on exit
    androidx.compose.runtime.DisposableEffect(ex.index) {
        onDispose {
            recorder?.let { runCatching { it.stop(); it.release() } }
            recorder = null
            recFile?.delete()
        }
    }

    Column {
        // Phrase card
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    ex.prompt ?: I18nStore.t("speak.repeatThis", "Repite esta frase:"),
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    phrase,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                TransliterationText(ex.transliteration)
                ex.hint?.let {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))

        // Score display if assessed
        score?.let { s ->
            val color = when {
                s >= 90 -> androidx.compose.ui.graphics.Color(0xFF15803D)
                s >= 70 -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.error
            }
            Surface(
                color = color.copy(alpha = 0.12f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "$s%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = color,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        feedback ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Record / Stop button
        Button(
            onClick = {
                if (recording) {
                    // Stop and assess
                    recording = false
                    assessing = true
                    recorder?.let { r -> runCatching { r.stop(); r.release() } }
                    recorder = null
                    scope.launch {
                        try {
                            val file = recFile ?: return@launch
                            val result = withContext(Dispatchers.IO) {
                                val audioPart = MultipartBody.Part.createFormData(
                                    "audio", file.name,
                                    file.asRequestBody("audio/mp4".toMediaType())
                                )
                                ApiClient.api.assessPronunciation(
                                    audioPart,
                                    phrase.toRequestBody("text/plain".toMediaType()),
                                    l2.toRequestBody("text/plain".toMediaType()),
                                )
                            }
                            score = result.score
                            feedback = result.feedback
                            assessing = false
                            // Auto-submit score as answer
                            onSubmit(result.score.toString())
                        } catch (e: Exception) {
                            assessing = false
                            score = 0
                            feedback = "Error al procesar audio. Intenta de nuevo."
                        } finally {
                            recFile?.delete()
                            recFile = null
                        }
                    }
                } else {
                    // Start recording
                    val file = File(context.cacheDir, "lesson_rec_${ex.index}.mp4")
                    recFile = file
                    val mr = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                        MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()).apply {
                        setAudioSource(MediaRecorder.AudioSource.MIC)
                        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                        setOutputFile(file.absolutePath)
                        prepare()
                        start()
                    }
                    recorder = mr
                    recording = true
                    score = null
                    feedback = null
                }
            },
            enabled = !assessing,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = if (recording)
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            else ButtonDefaults.buttonColors(),
        ) {
            when {
                assessing -> {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text(I18nStore.t("speak.analyzing", "Analizando pronunciación…"))
                }
                recording -> {
                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(I18nStore.t("speak.stopRecording", "Parar y evaluar"))
                }
                score != null -> {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(I18nStore.t("speak.tryAgain", "Intentar de nuevo"))
                }
                else -> {
                    Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(I18nStore.t("speak.startRecording", "Grabar mi voz"))
                }
            }
        }

        if (!recording && !assessing && score == null) {
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = { onSubmit("50") }, // skip with mid score
                modifier = Modifier.fillMaxWidth(),
            ) { Text(I18nStore.t("lesson.skip", "Saltar este ejercicio")) }
        }
    }
}

@Composable
private fun ListenSelectExercise(ex: PlayableExercise, onSubmit: (String) -> Unit) {
    val options = ex.options ?: emptyList()
    var selected by rememberSaveable(ex.index) { mutableStateOf(-1) }
    var playing by remember(ex.index) { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Auto-play TTS when exercise loads
    LaunchedEffect(ex.index) {
        ex.audioText?.let { text ->
            playing = true
            TtsPlayer.play(context, text)
            playing = false
        }
    }

    Column {
        // Audio play card
        Card(
            onClick = {
                ex.audioText?.let { text ->
                    scope.launch {
                        playing = true
                        TtsPlayer.play(context, text)
                        playing = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Spacer(Modifier.weight(1f))
                if (playing) {
                    CircularProgressIndicator(Modifier.size(36.dp), strokeWidth = 3.dp)
                } else {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(48.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    I18nStore.t("listen.tapToHear", "Toca para escuchar"),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            I18nStore.t("listen.selectMeaning", "¿Qué significa lo que escuchas?"),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        options.forEachIndexed { idx, opt ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selected == idx) MaterialTheme.colorScheme.tertiaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
                ),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    selected = idx
                },
            ) {
                Text(opt, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
            }
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { onSubmit(selected.toString()) },
            enabled = selected >= 0,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text(I18nStore.t("lesson.check", "Comprobar")) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordOrderExercise(ex: PlayableExercise, onSubmit: (String) -> Unit) {
    val tokens = ex.tokens ?: emptyList()
    val selected = remember(ex.index) { mutableStateListOf<Int>() }
    val haptic = LocalHapticFeedback.current

    Column {
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(I18nStore.t("lesson.wordOrder", "Ordena las palabras:"), style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                Text(ex.prompt ?: "", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(16.dp))
        // Fila respuesta (palabras elegidas en orden)
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
        ) {
            FlowRow(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                selected.forEachIndexed { pos, tokenIdx ->
                    TokenChip(tokens[tokenIdx], filled = true) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selected.removeAt(pos)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        // Banco de palabras disponibles
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tokens.indices.forEach { idx ->
                if (idx !in selected) {
                    TokenChip(tokens[idx], filled = false) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selected.add(idx)
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { onSubmit(selected.joinToString(" ") { tokens[it] }) },
            enabled = selected.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text(I18nStore.t("lesson.check", "Comprobar")) }
    }
}

@Composable
private fun TokenChip(text: String, filled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        tonalElevation = if (filled) 0.dp else 2.dp,
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = if (filled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ResultView(
    result: com.alturya.fluenta.network.LessonSubmitResponse,
    onContinueOnWhatsApp: () -> Unit,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    var visible by remember { mutableStateOf(false) }
    val xpScale = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        visible = true
        if (result.passed) {
            Sfx.success()
            xpScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow))
        }
    }
    val transition = rememberInfiniteTransition(label = "celebrate")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "pulse",
    )

    Box(Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(20.dp))
        AnimatedVisibility(visible = visible, enter = fadeIn() + scaleIn()) {
            if (result.passed) {
                Image(
                    painter = painterResource(R.drawable.ic_fluenta_celebra),
                    contentDescription = I18nStore.t("result.celebrateAlt", "¡Lección completada!"),
                    modifier = Modifier.size(150.dp).scale(pulse),
                )
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = I18nStore.t("result.keepGoing", "Sigue practicando"),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(100.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            if (result.passed) "¡Lección completada!" else "Sigue practicando",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.scale(if (result.passed) xpScale.value else 1f),
        ) {
            Stat(Icons.Default.CheckCircle, "${result.correctCount}/${result.total}", I18nStore.t("result.correct", "Correctos"))
            Stat(Icons.Default.Star, "+${result.xpEarned}", "XP")
            result.newStreakDays?.let { Stat(Icons.Default.LocalFireDepartment, "$it", I18nStore.t("result.streak", "Racha")) }
        }

        Spacer(Modifier.height(24.dp))

        if (!result.passed) {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(
                    "Necesitas 70% para completarla. Inténtala otra vez — vas en buen camino.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(I18nStore.t("result.performance", "Tu desempeño"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                result.results.forEachIndexed { idx, r ->
                    Row(Modifier.padding(vertical = 4.dp)) {
                        Icon(
                            imageVector = if (r.correct) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = null,
                            tint = if (r.correct) Color(0xFF22C55E) else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
                        )
                        Column {
                            Text(I18nStore.t("lesson.exNum", "Ej. {n}: {ans}").replace("{n}", "${idx + 1}").replace("{ans}", r.expected), style = MaterialTheme.typography.bodySmall)
                            if (!r.correct && r.given.isNotEmpty()) {
                                Text(I18nStore.t("lesson.yourAnswerWas", "Tu respuesta: {g}").replace("{g}", r.given), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                            r.feedback?.let {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(it, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Coach handoff — back to WhatsApp to apply what was just learned
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(I18nStore.t("result.applyTitle", "Aplica lo aprendido"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tu coach te espera en WhatsApp para usar esto en una conversación real.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onContinueOnWhatsApp,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(I18nStore.t("result.continueWa", "Continuar en WhatsApp →")) }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Share — primary: system chooser (viral for any app), secondary: WhatsApp shortcut
        if (result.passed) {
            val xpEarned = result.xpEarned
            val streakDays = result.newStreakDays ?: 0
            val shareText = if (streakDays > 0)
                "🔥 Racha de $streakDays días en Fluenta · +$xpEarned XP · Aprende inglés → fluenta.alturya.com"
            else
                "🎓 Completé una lección en Fluenta · +$xpEarned XP · Aprende inglés → fluenta.alturya.com"

            Button(
                onClick = {
                    context.startActivity(Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        },
                        I18nStore.t("result.shareTitle", "Compartir mi progreso"),
                    ))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(I18nStore.t("result.share", "Compartir logro"), fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        setPackage("com.whatsapp")
                    }
                    runCatching { context.startActivity(intent) }.onFailure {
                        context.startActivity(Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, shareText) },
                            I18nStore.t("result.shareTitle", "Compartir mi progreso"),
                        ))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = Color(0xFF25D366), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(I18nStore.t("result.shareWhatsapp", "Compartir en WhatsApp"))
            }
            Spacer(Modifier.height(8.dp))

            // Reto a un amigo — viral deeplink after streak ≥ 3
            if (streakDays >= 3) {
                val challengeText = "¡Llevo $streakDays días aprendiendo inglés con Fluenta sin parar! ¿Cuántos días aguantas tú? → fluenta.alturya.com"
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, challengeText)
                            setPackage("com.whatsapp")
                        }
                        runCatching { context.startActivity(intent) }.onFailure {
                            context.startActivity(Intent.createChooser(
                                Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, challengeText) }, null,
                            ))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = Color(0xFF25D366), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(I18nStore.t("result.challenge", "Retar a un amigo"))
                }
                Spacer(Modifier.height(6.dp))
            }
        }

        OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text(I18nStore.t("common.backHome", "Volver al inicio"))
        }
        Spacer(Modifier.height(20.dp))
    }  // end Column

    // Confetti overlay — full-screen Canvas above the scroll content, fades out after 2.5s
    if (result.passed) {
        ConfettiOverlay()
    }
    }  // end Box
}

private data class Particle(
    val x: Float,
    val vy: Float,        // vertical velocity (pixels/frame)
    val vx: Float,        // horizontal drift
    val color: Color,
    val size: Float,
    val rotSpeed: Float,  // degrees/frame
    var angle: Float = 0f,
)

@Composable
private fun ConfettiOverlay() {
    val colors = listOf(
        Color(0xFFFFD700), Color(0xFF25D366), Color(0xFF3B82F6),
        Color(0xFFEC4899), Color(0xFFF97316), Color(0xFF8B5CF6),
    )
    val particles = remember {
        List(60) {
            Particle(
                x = Random.nextFloat(),
                vy = Random.nextFloat() * 6f + 4f,
                vx = (Random.nextFloat() - 0.5f) * 3f,
                color = colors[Random.nextInt(colors.size)],
                size = Random.nextFloat() * 10f + 6f,
                rotSpeed = (Random.nextFloat() - 0.5f) * 8f,
            )
        }
    }
    val time = remember { Animatable(0f) }
    val alpha by animateFloatAsState(
        targetValue = if (time.value > 1.8f) 0f else 1f,
        animationSpec = tween(600),
        label = "confAlpha",
    )
    LaunchedEffect(Unit) {
        time.animateTo(2.5f, tween(2500))
    }
    if (time.value >= 2.5f) return

    Canvas(modifier = Modifier.fillMaxSize()) {
        val h = size.height
        val w = size.width
        particles.forEach { p ->
            val yFrac = (time.value * p.vy * 0.12f) % 1.1f
            val xFrac = p.x + time.value * p.vx * 0.02f
            val cx = xFrac * w
            val cy = yFrac * h + p.size
            p.angle += p.rotSpeed
            rotate(p.angle, pivot = Offset(cx, cy)) {
                drawRect(
                    color = p.color.copy(alpha = alpha),
                    topLeft = Offset(cx - p.size / 2, cy - p.size / 2),
                    size = androidx.compose.ui.geometry.Size(p.size, p.size * 0.5f),
                )
            }
        }
    }
}

@Composable
private fun Stat(icon: ImageVector, value: String, label: String) {
    Card(modifier = Modifier.width(96.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(12.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onRetry) { Text(I18nStore.t("common.retry", "Reintentar")) }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBack) { Text(I18nStore.t("common.back", "Volver")) }
    }
}
