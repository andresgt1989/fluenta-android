package com.alturya.fluenta.diagnostic

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.data.Session
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
            DiagnosticUiState.Intro -> IntroPanel(onStart = { vm.start() }, onSkip = onDone)
            DiagnosticUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is DiagnosticUiState.Quiz -> QuizPanel(s, onAnswer = vm::answer, onSkip = vm::skip)
            is DiagnosticUiState.Result -> ResultPanel(s, onDone = onDone, onRetake = { vm.start() })
            is DiagnosticUiState.Error -> ErrorPanel(s.message, onRetry = { vm.start() })
        }
    }
}

@Composable
private fun IntroPanel(onStart: () -> Unit, onSkip: () -> Unit = {}) {
    Column(
        modifier = Modifier.fillMaxSize().padding(top = 40.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.GpsFixed, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(80.dp))
        Spacer(Modifier.height(16.dp))
        Text(I18nStore.t("diagnostic.title", "Test de nivel"), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(8.dp))
        Text(
            I18nStore.t("diagnostic.intro", "6 preguntas adaptativas que ajustan su dificultad a tu nivel real. Tarda menos de 2 minutos."),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        // Sample question preview to reduce test anxiety
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    I18nStore.t("diagnostic.exampleLabel", "Ejemplo de pregunta:"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    I18nStore.t("diagnostic.exampleQ", "\"She ___ to the store yesterday.\""),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("go", "went", "gone", "goes").forEach { opt ->
                        Surface(
                            color = if (opt == "went") MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.small,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (opt == "went") MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                            ),
                        ) {
                            Text(opt, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (opt == "went") MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (opt == "went") FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        com.alturya.fluenta.ui.FluentaButton(
            text = I18nStore.t("diagnostic.start", "Empezar el test"),
            onClick = onStart,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onSkip) {
            Text(I18nStore.t("diagnostic.skipForNow", "Hacerlo más tarde"))
        }
    }
}

@Composable
private fun QuizPanel(s: DiagnosticUiState.Quiz, onAnswer: (Int) -> Unit, onSkip: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val progressFraction = s.progress.current.toFloat() / s.progress.total.toFloat()
    val animatedProgress by animateFloatAsState(targetValue = progressFraction, label = "progress")

    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${I18nStore.t("diagnostic.question", "Pregunta")} ${s.progress.current} ${I18nStore.t("diagnostic.of", "de")} ${s.progress.total}",
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.GpsFixed, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
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
            }, onSkip = onSkip)
        }
    }
}

@Composable
private fun QuestionContent(
    q: DiagnosticQuestion,
    s: DiagnosticUiState.Quiz,
    onAnswer: (Int) -> Unit,
    onSkip: () -> Unit
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
                enabled = !s.answering && s.lastAnsweredIndex == null && !s.wasSkipped
            )
            Spacer(Modifier.height(12.dp))
        }
        // "No sé" skip button — shown only while the user can still answer.
        // Tapping it registers as wrong without forcing a random guess, which
        // prevents lucky guesses from inflating the computed CEFR/JLPT/HSK level.
        if (!s.answering && s.lastAnsweredIndex == null && !s.wasSkipped) {
            Spacer(Modifier.height(4.dp))
            TextButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    I18nStore.t("diagnostic.dontKnow", "No sé / Saltar"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        // Skip feedback shown for ~800 ms while the next question loads
        if (s.wasSkipped && s.answering) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    I18nStore.t("diagnostic.skippedFeedback", "Saltada — no suma puntos"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
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
    // a11y: la corrección se señala visualmente con color + icono; sin esto un lector de
    // pantalla solo oiría el texto de la opción (fallo WCAG 1.1.1 y 1.4.1 "uso del color").
    // stateDescription en la Card (que fusiona a sus hijos) lo anuncia: "Pekín. Correcto".
    val answerState = when (state) {
        OptionUiState.Correct -> I18nStore.t("diagnostic.answerCorrect", "Correcto")
        OptionUiState.Wrong -> I18nStore.t("diagnostic.answerWrong", "Incorrecto")
        else -> null
    }
    Card(
        modifier = Modifier.fillMaxWidth().then(
            if (answerState != null) Modifier.semantics { stateDescription = answerState } else Modifier
        ),
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
                OptionUiState.Correct -> Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                OptionUiState.Wrong -> Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                else -> {}
            }
        }
    }
}

@Composable
private fun ResultPanel(s: DiagnosticUiState.Result, onDone: () -> Unit, onRetake: () -> Unit = {}) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.4f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    // Level system depends on the language being learned:
    // Japanese → JLPT, Chinese → HSK, Korean → TOPIK, all others → CEFR
    val levelSystem = when (Session.l2?.lowercase()) {
        "ja" -> "jlpt"
        "zh" -> "hsk"
        "ko" -> "topik"
        else -> "cefr"
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedVisibility(visible = visible, enter = fadeIn() + scaleIn()) {
            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(80.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(I18nStore.t("diagnostic.yourLevel", "Tu nivel"), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            levelLabel(s.level, levelSystem),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Black,
            modifier = Modifier.scale(scale),
            color = MaterialTheme.colorScheme.primary
        )
        Text(levelSystemName(levelSystem), style = MaterialTheme.typography.labelLarge)

        // Explica en lenguaje claro qué SIGNIFICA ese nivel (can-do), no solo la sigla.
        Spacer(Modifier.height(12.dp))
        Text(
            cefrCanDo(s.level),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(Modifier.height(24.dp))
        val confPct = (s.confidence * 100).toInt().coerceIn(0, 100)
        val confColor = when {
            confPct >= 80 -> Color(0xFF16A34A)
            confPct >= 60 -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.error
        }
        val confLabel = when {
            confPct >= 80 -> I18nStore.t("diagnostic.confidenceHigh", "Alta")
            confPct >= 60 -> I18nStore.t("diagnostic.confidenceMed", "Media")
            else -> I18nStore.t("diagnostic.confidenceLow", "Baja")
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${I18nStore.t("diagnostic.correct", "Acertaste")} ${s.correctCount} de ${s.total}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        I18nStore.t("diagnostic.confidence", "Confianza diagnóstica"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Surface(
                        color = confColor.copy(alpha = 0.15f),
                        shape = MaterialTheme.shapes.extraSmall,
                    ) {
                        Text(
                            "$confLabel · $confPct%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = confColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                val animConf by animateFloatAsState(confPct / 100f, label = "conf")
                LinearProgressIndicator(
                    progress = { animConf },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = confColor,
                    trackColor = confColor.copy(alpha = 0.15f),
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        com.alturya.fluenta.ui.FluentaButton(
            text = I18nStore.t("common.continue", "Continuar"),
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
        )
        if (confPct < 60) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onRetake,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.GpsFixed, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(I18nStore.t("diagnostic.retake", "Repetir el test"))
            }
        }
        Spacer(Modifier.height(16.dp))
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
        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(12.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onRetry) { Text(I18nStore.t("common.retry", "Reintentar")) }
    }
}

// Descripción "can-do" del nivel CEFR (y aproximaciones para JLPT/HSK/TOPIK que
// comparten la escala A1–C2 a grandes rasgos), para que el usuario entienda QUÉ
// puede hacer con su nivel, no solo la sigla.
private fun cefrCanDo(level: String?): String {
    val key = (level ?: "").lowercase().take(2)
    return when {
        key.startsWith("a1") || key.contains("1") && key.startsWith("a") ->
            I18nStore.t("cefr.a1", "Entiendes y usas frases básicas y cotidianas. Te presentas y pides cosas simples.")
        key.startsWith("a2") ->
            I18nStore.t("cefr.a2", "Te comunicas en situaciones rutinarias: compras, lugares, trabajo. Frases cortas.")
        key.startsWith("b1") ->
            I18nStore.t("cefr.b1", "Te desenvuelves en viajes y conversaciones cotidianas. Cuentas experiencias y planes.")
        key.startsWith("b2") ->
            I18nStore.t("cefr.b2", "Hablas con fluidez y espontaneidad. Discutes temas complejos con cierta soltura.")
        key.startsWith("c1") ->
            I18nStore.t("cefr.c1", "Usas el idioma con flexibilidad y eficacia, casi sin esfuerzo, en contextos exigentes.")
        key.startsWith("c2") ->
            I18nStore.t("cefr.c2", "Dominio casi nativo: entiendes todo con facilidad y te expresas con gran precisión.")
        else ->
            I18nStore.t("cefr.generic", "Este es tu punto de partida. Ajustamos las lecciones a tu nivel real.")
    }
}
