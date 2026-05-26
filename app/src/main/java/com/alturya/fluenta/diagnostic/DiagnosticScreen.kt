package com.alturya.fluenta.diagnostic

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alturya.fluenta.network.DiagnosticQuestion
import com.alturya.fluenta.ui.FeedbackBar
import com.alturya.fluenta.util.levelLabel
import com.alturya.fluenta.util.levelSystemName

@Composable
fun DiagnosticScreen(onDone: () -> Unit = {}) {
    val vm: DiagnosticViewModel = viewModel()
    val state by vm.state.collectAsState()

    Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        when (val s = state) {
            DiagnosticUiState.Intro -> IntroPanel(onStart = { vm.start() })
            DiagnosticUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is DiagnosticUiState.Quiz -> QuizPanel(s, onAnswer = vm::answer)
            is DiagnosticUiState.Result -> ResultPanel(s, onDone = onDone)
            is DiagnosticUiState.Error -> ErrorPanel(s.message, onRetry = { vm.start() })
        }
    }
}

@Composable
private fun IntroPanel(onStart: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🎯", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        Text("Test de nivel", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "6 preguntas adaptativas que ajustan su dificultad a tu nivel real. Tarda menos de 2 minutos.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) { Text("Empezar el test", style = MaterialTheme.typography.titleMedium) }
    }
}

@Composable
private fun QuizPanel(s: DiagnosticUiState.Quiz, onAnswer: (Int) -> Unit) {
    val haptic = LocalHapticFeedback.current
    val progressFraction = s.progress.current.toFloat() / s.progress.total.toFloat()
    val animatedProgress by animateFloatAsState(targetValue = progressFraction, label = "progress")

    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Pregunta ${s.progress.current} de ${s.progress.total}", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.weight(1f))
            Text("🎯", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxWidth().height(8.dp)
        )

        Spacer(Modifier.height(32.dp))

        AnimatedContent(
            targetState = s.question,
            transitionSpec = {
                (slideInHorizontally { it } + fadeIn()) togetherWith
                    (slideOutHorizontally { -it } + fadeOut())
            },
            label = "question"
        ) { q ->
            QuestionContent(q, s, onAnswer = { idx ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onAnswer(idx)
            })
        }
    }
}

@Composable
private fun QuestionContent(
    q: DiagnosticQuestion,
    s: DiagnosticUiState.Quiz,
    onAnswer: (Int) -> Unit
) {
    Column {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Text(
                q.prompt,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(24.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
        q.options.forEachIndexed { idx, opt ->
            OptionCard(
                text = opt,
                state = optionState(idx, s),
                onClick = { onAnswer(idx) },
                enabled = !s.answering && s.lastAnsweredIndex == null
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

private enum class OptionUiState { Idle, ChosenWaiting, Correct, Wrong, Disabled }

private fun optionState(idx: Int, s: DiagnosticUiState.Quiz): OptionUiState {
    if (s.lastAnsweredIndex == idx) {
        return when (s.lastCorrect) {
            true -> OptionUiState.Correct
            false -> OptionUiState.Wrong
            null -> OptionUiState.ChosenWaiting
        }
    }
    if (s.lastAnsweredIndex != null) return OptionUiState.Disabled
    return OptionUiState.Idle
}

@Composable
private fun OptionCard(text: String, state: OptionUiState, onClick: () -> Unit, enabled: Boolean) {
    val container = when (state) {
        OptionUiState.Correct -> Color(0xFF22C55E)
        OptionUiState.Wrong -> MaterialTheme.colorScheme.error
        OptionUiState.ChosenWaiting -> MaterialTheme.colorScheme.tertiary
        OptionUiState.Disabled -> MaterialTheme.colorScheme.surfaceVariant
        OptionUiState.Idle -> MaterialTheme.colorScheme.surface
    }
    val on = when (state) {
        OptionUiState.Correct, OptionUiState.Wrong, OptionUiState.ChosenWaiting -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = on),
        onClick = onClick,
        enabled = enabled
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            when (state) {
                OptionUiState.Correct -> Text("✓", style = MaterialTheme.typography.titleLarge)
                OptionUiState.Wrong -> Text("✗", style = MaterialTheme.typography.titleLarge)
                else -> {}
            }
        }
    }
}

@Composable
private fun ResultPanel(s: DiagnosticUiState.Result, onDone: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.4f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedVisibility(visible = visible, enter = fadeIn() + scaleIn()) {
            Text("🎉", style = MaterialTheme.typography.displayLarge)
        }
        Spacer(Modifier.height(16.dp))
        Text("Tu nivel", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            levelLabel(s.level, "cefr"),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Black,
            modifier = Modifier.scale(scale),
            color = MaterialTheme.colorScheme.primary
        )
        Text(levelSystemName("cefr"), style = MaterialTheme.typography.labelLarge)

        Spacer(Modifier.height(24.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text("Acertaste ${s.correctCount} de ${s.total}", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(4.dp))
                Text("Confianza: ${(s.confidence * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(32.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("Continuar", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(20.dp))
        FeedbackBar(surface = "diagnostic")
    }
}

@Composable
private fun ErrorPanel(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("⚠️", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(12.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onRetry) { Text("Reintentar") }
    }
}
