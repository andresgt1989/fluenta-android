package com.alturya.fluenta.lesson

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alturya.fluenta.network.ApiClient
import com.alturya.fluenta.network.PlayableExercise
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun LessonPlayerScreen(lessonId: String, onDone: () -> Unit) {
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
    val state by vm.state.collectAsState()
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
            state.exercises.isEmpty() -> ErrorView("Esta lección aún no tiene ejercicios disponibles.", onRetry = vm::retry, onBack = onDone)
            else -> QuizView(state, vm)
        }
    }
}

@Composable
private fun QuizView(state: LessonPlayerState, vm: LessonPlayerViewModel) {
    val ex = state.exercises.getOrNull(state.currentIndex) ?: return
    val total = state.exercises.size
    val progressFraction = state.currentIndex.toFloat() / total.toFloat()

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Ejercicio ${state.currentIndex + 1} de $total", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.weight(1f))
            state.title?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
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
                (slideInHorizontally { it } + fadeIn()) togetherWith
                    (slideOutHorizontally { -it } + fadeOut())
            },
            label = "exercise",
        ) { _ ->
            when (ex.kind) {
                "translate_l1_to_l2", "translate_l2_to_l1" -> TranslateExercise(ex, onSubmit = { v ->
                    vm.recordAnswer(v); vm.next()
                })
                "multiple_choice" -> MultipleChoiceExercise(ex, onSubmit = { v ->
                    vm.recordAnswer(v); vm.next()
                })
                "match_pairs" -> MatchPairsExercise(ex, onSubmit = { v ->
                    vm.recordAnswer(v); vm.next()
                })
                else -> Text("Tipo no soportado: ${ex.kind}", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(Modifier.weight(1f))
        TextButton(onClick = vm::skip) { Text("Saltar este") }
    }
}

@Composable
private fun TranslateExercise(ex: PlayableExercise, onSubmit: (String) -> Unit) {
    var text by rememberSaveable(ex.index) { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current

    Column {
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    "Traduce esto:",
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    ex.prompt ?: "",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                ex.hint?.let {
                    Spacer(Modifier.height(8.dp))
                    Text("💡 $it", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Tu respuesta") },
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
        ) { Text("Comprobar") }
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
            Text(
                ex.prompt ?: "",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(20.dp),
                fontWeight = FontWeight.Medium,
            )
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
        ) { Text("Comprobar") }
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
            "Empareja",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                left.forEach { l ->
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
                    ) { Text(l, Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium) }
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
        ) { Text("Comprobar") }
    }
}

@Composable
private fun ResultView(
    result: com.alturya.fluenta.network.LessonSubmitResponse,
    onContinueOnWhatsApp: () -> Unit,
    onDone: () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(20.dp))
        AnimatedVisibility(visible = visible, enter = fadeIn() + scaleIn()) {
            Text(
                if (result.passed) "🎉" else "💪",
                style = MaterialTheme.typography.displayLarge,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            if (result.passed) "¡Lección completada!" else "Sigue practicando",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat("✓", "${result.correctCount}/${result.total}", "Correctos")
            Stat("⭐", "+${result.xpEarned}", "XP")
            result.newStreakDays?.let { Stat("🔥", "$it", "Racha") }
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
                Text("Tu desempeño", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                result.results.forEachIndexed { idx, r ->
                    Row(Modifier.padding(vertical = 4.dp)) {
                        Text(if (r.correct) "✓ " else "✗ ", color = if (r.correct) Color(0xFF22C55E) else MaterialTheme.colorScheme.error)
                        Column {
                            Text("Ej. ${idx + 1}: ${r.expected}", style = MaterialTheme.typography.bodySmall)
                            if (!r.correct && r.given.isNotEmpty()) {
                                Text("Tu respuesta: ${r.given}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                            r.feedback?.let { Text("💡 $it", style = MaterialTheme.typography.labelSmall) }
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
                Text("💬 Aplica lo aprendido", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tu coach te espera en WhatsApp para usar esto en una conversación real.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onContinueOnWhatsApp,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Continuar en WhatsApp →") }
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text("Volver al inicio")
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun Stat(icon: String, value: String, label: String) {
    Card(modifier = Modifier.width(96.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(icon, style = MaterialTheme.typography.titleLarge)
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
        Text("⚠️", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(12.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onRetry) { Text("Reintentar") }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBack) { Text("Volver") }
    }
}
