package com.alturya.fluenta.script

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.network.ScriptItem

// Pantalla "aprende el alfabeto/silabario primero" para idiomas no-latinos.
// LEER (lección método nativo) + ESCRIBIR (trazo a trazo) + examen de lectura
// que actualiza el dominio (scriptMastery) y va apagando la transliteración.
@Composable
fun ScriptScreen(l2: String, onDone: () -> Unit = {}, previewState: ScriptUiState? = null) {
    val vm: ScriptViewModel = viewModel()
    val vmState by vm.state.collectAsState()
    val state = previewState ?: vmState

    LaunchedEffect(l2) { if (previewState == null) vm.load(l2) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        when (state.phase) {
            ScriptPhase.LOADING -> Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            ScriptPhase.ERROR -> {
                Text(state.error, color = MaterialTheme.colorScheme.error)
                Button(onClick = { vm.load(l2) }) { Text(I18nStore.t("common.retry", "Reintentar")) }
            }

            ScriptPhase.LEARN -> {
                val lesson = state.lesson
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(I18nStore.t("script.title", "Aprende a leer y escribir"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                }
                lesson?.let { l ->
                    Text(l.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text(l.intro, style = MaterialTheme.typography.bodyMedium)
                    state.info?.let { info ->
                        ReadingMeter(info.readingScore)
                    }
                    l.items.forEach { item -> GlyphCard(item, writingSupported = l.writingSupported) }
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Create,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                I18nStore.t("script.examScope", "El examen solo incluye los {n} caracteres que acabas de ver.").replace("{n}", "${l.items.size}"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { vm.startQuiz() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) { Text(I18nStore.t("script.takeExam", "Hacer examen de lectura")) }
                    TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                        Text(I18nStore.t("common.back", "Volver"))
                    }
                }
            }

            ScriptPhase.QUIZ -> {
                val quiz = state.quiz
                Text(I18nStore.t("script.examTitle", "Examen de lectura"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(I18nStore.t("script.examPrompt", "¿Qué sonido tiene cada carácter?"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                quiz?.items?.forEachIndexed { i, q ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(q.glyph, style = MaterialTheme.typography.displaySmall, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                            Spacer(Modifier.height(10.dp))
                            q.options.forEach { opt ->
                                val selected = state.answers[i] == opt
                                OutlinedButton(
                                    onClick = { vm.choose(i, opt) },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                    colors = if (selected) ButtonDefaults.outlinedButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    ) else ButtonDefaults.outlinedButtonColors(),
                                ) { Text(opt) }
                            }
                        }
                    }
                }
                Button(
                    onClick = { vm.submitQuiz() },
                    enabled = (quiz?.items?.size ?: 0) > 0 && state.answers.size == (quiz?.items?.size ?: -1),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text(I18nStore.t("script.grade", "Calificar")) }
            }

            ScriptPhase.RESULT -> {
                val r = state.result
                Spacer(Modifier.height(24.dp))
                Text("${r?.score ?: 0}%", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.ExtraBold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.primary)
                Text(
                    I18nStore.t("script.gotRight", "Acertaste {c} de {t}").replace("{c}", "${r?.correct ?: 0}").replace("{t}", "${r?.total ?: 0}"),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                ReadingMeter(r?.mastery ?: 0)
                Text(
                    when {
                        (r?.mastery ?: 0) >= 80 -> I18nStore.t("script.masteryHigh", "¡Ya lees este alfabeto! La transliteración se irá apagando para que leas de verdad.")
                        (r?.mastery ?: 0) >= 40 -> I18nStore.t("script.masteryMid", "Vas bien. Seguimos mostrando la lectura latina solo donde la necesitas.")
                        else -> I18nStore.t("script.masteryLow", "Recién empiezas — practica los trazos y vuelve a intentarlo.")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = { vm.backToLearn() }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text(I18nStore.t("script.keepPracticing", "Seguir practicando"))
                }
                TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text(I18nStore.t("common.done", "Listo")) }
            }
        }
    }
}

@Composable
private fun ReadingMeter(score: Int) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(I18nStore.t("script.reading", "Lectura"), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            Text("$score%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (score / 100f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp),
        )
    }
}

@Composable
private fun GlyphCard(item: ScriptItem, writingSupported: Boolean = true) {
    var writing by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.glyph, style = MaterialTheme.typography.displaySmall)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        item.romanization + (item.meaning?.let { "  ·  $it" } ?: ""),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    item.mnemonic?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    item.strokeOrder?.let {
                        Text(I18nStore.t("script.strokes", "Trazos: {s}").replace("{s}", it), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (writingSupported) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { writing = !writing }) {
                    Icon(Icons.Default.Create, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (writing) I18nStore.t("script.hideStrokes", "Ocultar trazos") else I18nStore.t("script.write", "Escribir"))
                }
            } else {
                Spacer(Modifier.height(6.dp))
                Text(
                    I18nStore.t("script.writingSoon", "✏️ Práctica de escritura próximamente"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (writing && writingSupported) {
                Spacer(Modifier.height(8.dp))
                StrokeWriter(glyph = item.glyph)
            }
        }
    }
}
