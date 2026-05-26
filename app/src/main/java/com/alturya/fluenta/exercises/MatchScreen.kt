package com.alturya.fluenta.exercises

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alturya.fluenta.ui.FeedbackBar

@Composable
fun MatchScreen(onDone: () -> Unit = {}) {
    val vm: MatchViewModel = viewModel()
    val state by vm.state.collectAsState()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(state.wrongFlash) {
        if (state.wrongFlash != null) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    LaunchedEffect(state.won) {
        if (state.won) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    Box(Modifier.fillMaxSize().padding(20.dp)) {
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.empty -> CenteredMessage("Completa lecciones para desbloquear este juego de vocabulario. 🎮")
            state.error != null -> CenteredMessage(state.error!!)
            state.won -> WinPanel(state.attempts, state.answerKey.size, onAgain = { vm.load() }, onDone = onDone)
            else -> GameBoard(state, vm)
        }
    }
}

@Composable
private fun GameBoard(state: MatchState, vm: MatchViewModel) {
    Column(Modifier.fillMaxSize()) {
        Text("Empareja", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Une cada frase con su significado", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        val progress by animateFloatAsState(
            targetValue = if (state.answerKey.isEmpty()) 0f else state.matched.size.toFloat() / state.answerKey.size,
            label = "match_progress"
        )
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp))
        Spacer(Modifier.height(20.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Left column — L2
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.leftItems.forEach { item ->
                    val matched = state.matched.contains(item)
                    val selected = state.selectedLeft == item
                    val wrong = state.wrongFlash?.first == item
                    Tile(
                        text = item,
                        bg = when {
                            matched -> Color(0xFF22C55E)
                            wrong -> MaterialTheme.colorScheme.error
                            selected -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        enabled = !matched,
                        onClick = { vm.tapLeft(item) }
                    )
                }
            }
            // Right column — L1
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.rightItems.forEach { item ->
                    val matched = state.matched.any { state.answerKey[it] == item }
                    val wrong = state.wrongFlash?.second == item
                    Tile(
                        text = item,
                        bg = when {
                            matched -> Color(0xFF22C55E)
                            wrong -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        enabled = !matched,
                        onClick = { vm.tapRight(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun Tile(text: String, bg: Color, enabled: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bg),
        onClick = onClick,
        enabled = enabled
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(14.dp),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun WinPanel(attempts: Int, total: Int, onAgain: () -> Unit, onDone: () -> Unit) {
    val accuracy = if (attempts > 0) (total * 100 / attempts) else 100
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedVisibility(visible = true, enter = fadeIn() + scaleIn()) {
            Text("🏆", style = MaterialTheme.typography.displayLarge)
        }
        Spacer(Modifier.height(16.dp))
        Text("¡Completado!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Precisión: $accuracy%  ·  $total pares", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onAgain, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Jugar de nuevo") }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Listo") }
        Spacer(Modifier.height(20.dp))
        FeedbackBar(surface = "match")
    }
}

@Composable
private fun CenteredMessage(msg: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(msg, style = MaterialTheme.typography.bodyLarge)
    }
}
