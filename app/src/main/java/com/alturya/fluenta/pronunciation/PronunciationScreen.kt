package com.alturya.fluenta.pronunciation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.network.WordResult

@Composable
fun PronunciationScreen() {
    val context = LocalContext.current
    val vm: PronunciationViewModel = viewModel()
    val state by vm.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.error) { state.error?.let { snackbar.showSnackbar(it) } }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { pad ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val drill = state.drill
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(I18nStore.t("pronunciation.title", "Pronunciación"), style = MaterialTheme.typography.headlineMedium)
                if (state.source == "top_error") {
                    Text(
                        I18nStore.t("pronunciation.workingWeak", "Trabajando tu fonema más débil"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (drill == null) {
                item { Text(I18nStore.t("pronunciation.noExercise", "No hay ejercicio disponible.")) }
                return@LazyColumn
            }

            item {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                drill.symbol,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(drill.label, style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            drill.tipEs,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            item {
                Text(
                    I18nStore.t("pronunciation.practicePhrases", "Practica estas frases"),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(drill.phrases) { phrase ->
                SpeakRepeatCard(
                    phrase = phrase,
                    l2 = "en",  // drill is ES→EN focused; TODO: make dynamic from user profile
                    playing = state.playing == phrase,
                    recording = state.recording == phrase,
                    assessing = state.assessing == phrase,
                    assessResult = state.assessResults[phrase],
                    onPlay = { vm.listen(context, phrase) },
                    onStartRecord = { vm.startRecording(context, phrase) },
                    onStopRecord = { vm.stopAndAssess("en") },
                )
            }
        }
    }
}

@Composable
private fun SpeakRepeatCard(
    phrase: String,
    l2: String,
    playing: Boolean,
    recording: Boolean,
    assessing: Boolean,
    assessResult: AssessResult?,
    onPlay: () -> Unit,
    onStartRecord: () -> Unit,
    onStopRecord: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            // Phrase text
            Text(phrase, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(12.dp))

            // Action buttons row
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Listen button
                FilledTonalButton(
                    onClick = onPlay,
                    enabled = !recording && !assessing,
                    modifier = Modifier.weight(1f),
                ) {
                    if (playing) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("🔊 ${I18nStore.t("pron.listen", "Escuchar")}")
                }

                // Record / Stop button
                Button(
                    onClick = if (recording) onStopRecord else onStartRecord,
                    enabled = !playing && !assessing,
                    modifier = Modifier.weight(1f),
                    colors = if (recording)
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    else ButtonDefaults.buttonColors(),
                ) {
                    when {
                        assessing -> {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(6.dp))
                            Text(I18nStore.t("pron.analyzing", "Analizando…"))
                        }
                        recording -> Text("⏹ ${I18nStore.t("pron.stop", "Parar")}")
                        else -> Text("🎙 ${I18nStore.t("pron.record", "Grabar")}")
                    }
                }
            }

            // Assessment result
            if (assessResult != null) {
                Spacer(Modifier.height(12.dp))
                ScoreBar(assessResult)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScoreBar(result: AssessResult) {
    val scoreColor = when {
        result.score >= 90 -> Color(0xFF15803D)
        result.score >= 70 -> Color(0xFF1BB6A6)
        result.score >= 40 -> Color(0xFFF0A22E)
        else -> MaterialTheme.colorScheme.error
    }
    Surface(
        color = scoreColor.copy(alpha = 0.12f),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${result.score}%",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = scoreColor,
                )
                Spacer(Modifier.width(8.dp))
                Text(result.feedback, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            }
            // Word-by-word result
            if (result.wordResults.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    result.wordResults.forEach { w ->
                        Surface(
                            color = if (w.ok) Color(0xFF22C55E).copy(alpha = 0.18f)
                            else MaterialTheme.colorScheme.error.copy(alpha = 0.18f),
                            shape = MaterialTheme.shapes.extraSmall,
                        ) {
                            Text(
                                w.word,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (w.ok) Color(0xFF15803D) else MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }
}
