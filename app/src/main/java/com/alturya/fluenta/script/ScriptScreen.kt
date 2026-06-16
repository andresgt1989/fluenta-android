package com.alturya.fluenta.script

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alturya.fluenta.network.ScriptItem

// Pantalla "aprende el alfabeto/silabario primero" para idiomas no-latinos.
// LEER (lección método nativo) + ESCRIBIR (trazo a trazo) + examen de lectura
// que actualiza el dominio (scriptMastery) y va apagando la transliteración.
@Composable
fun ScriptScreen(l2: String, onDone: () -> Unit = {}) {
    val vm: ScriptViewModel = viewModel()
    val state by vm.state.collectAsState()

    LaunchedEffect(l2) { vm.load(l2) }

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
                Button(onClick = { vm.load(l2) }) { Text("Reintentar") }
            }

            ScriptPhase.LEARN -> {
                val lesson = state.lesson
                Text(
                    "✍️ Aprende a leer y escribir",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                )
                lesson?.let { l ->
                    Text(l.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text(l.intro, style = MaterialTheme.typography.bodyMedium)
                    state.info?.let { info ->
                        ReadingMeter(info.readingScore)
                    }
                    l.items.forEach { item -> GlyphCard(item) }
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = { vm.startQuiz() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) { Text("Hacer examen de lectura") }
                    TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                        Text("Volver")
                    }
                }
            }

            ScriptPhase.QUIZ -> {
                val quiz = state.quiz
                Text("Examen de lectura", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("¿Qué sonido tiene cada carácter?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                ) { Text("Calificar") }
            }

            ScriptPhase.RESULT -> {
                val r = state.result
                Spacer(Modifier.height(24.dp))
                Text("${r?.score ?: 0}%", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.ExtraBold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.primary)
                Text(
                    "Acertaste ${r?.correct ?: 0} de ${r?.total ?: 0}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                ReadingMeter(r?.mastery ?: 0)
                Text(
                    when {
                        (r?.mastery ?: 0) >= 80 -> "¡Ya lees este alfabeto! La transliteración se irá apagando para que leas de verdad."
                        (r?.mastery ?: 0) >= 40 -> "Vas bien. Seguimos mostrando la lectura latina solo donde la necesitas."
                        else -> "Recién empiezas — practica los trazos y vuelve a intentarlo."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = { vm.backToLearn() }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text("Seguir practicando")
                }
                TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Listo") }
            }
        }
    }
}

@Composable
private fun ReadingMeter(score: Int) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Lectura", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
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
private fun GlyphCard(item: ScriptItem) {
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
                        Text("Trazos: $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { writing = !writing }) {
                Text(if (writing) "Ocultar trazos" else "✍️ Escribir")
            }
            if (writing) {
                Spacer(Modifier.height(8.dp))
                StrokeWriter(glyph = item.glyph)
            }
        }
    }
}
