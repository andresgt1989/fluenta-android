package com.alturya.fluenta.script

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alturya.fluenta.audio.TtsPlayer
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.network.ScriptItem
import com.alturya.fluenta.ui.ShimmerBox
import com.alturya.fluenta.ui.ShimmerCard
import kotlinx.coroutines.launch

// ── Tokens del kit Claude Design (locales, sin tocar el theme compartido) ──
private val DcInk = Color(0xFF0F2E27)
private val DcSlate = Color(0xFF5B7268)
private val DcSurface = Color(0xFFF1FAF6)
private val DcMint = Color(0xFFCDEEE6)
private val DcMintDeep = Color(0xFF06463A)
private val DcAmber = Color(0xFFE08A00)
private val DcHairline = Color(0xFFE5EFEA)

// Pantalla "aprende el alfabeto/silabario primero" para idiomas no-latinos.
// Diseño FIEL a [12] ScriptScreen del kit de Claude Design.
@Composable
fun ScriptScreen(l2: String, onDone: () -> Unit = {}, onReviewChars: () -> Unit = {}, onWrite: (String) -> Unit = {}, previewState: ScriptUiState? = null) {
    val vm: ScriptViewModel = viewModel()
    val vmState by vm.state.collectAsState()
    val state = previewState ?: vmState

    LaunchedEffect(l2) { if (previewState == null) vm.load(l2) }

    // SRS de caracteres: los glifos de esta lección entran a la cola de repaso.
    val context = LocalContext.current
    LaunchedEffect(state.lesson?.items) {
        val items = state.lesson?.items
        if (previewState == null && !items.isNullOrEmpty()) {
            HanziSrsStore.enqueue(context, l2, items)
        }
    }

    val resultGradient = Brush.verticalGradient(
        listOf(MaterialTheme.colorScheme.primary, DcMint, DcSurface),
    )
    val bg = if (state.phase == ScriptPhase.RESULT) resultGradient else Brush.verticalGradient(listOf(DcSurface, DcSurface))

    Box(Modifier.fillMaxSize().background(bg)) {
        when (state.phase) {
            ScriptPhase.LOADING -> LoadingState()
            ScriptPhase.ERROR -> ErrorState(state.error, onRetry = { vm.load(l2) }, onBack = onDone)
            ScriptPhase.LEARN -> {
                val items = state.lesson?.items
                if (items.isNullOrEmpty()) EmptyState(onBack = onDone)
                else LearnState(state, onTakeExam = { vm.startQuiz() }, onWrite = onWrite, onBack = onDone)
            }
            ScriptPhase.QUIZ -> QuizState(state, onChoose = vm::choose, onGrade = { vm.submitQuiz() }, onBack = onDone)
            ScriptPhase.RESULT -> ResultState(state, onKeepLearning = { vm.backToLearn() }, onReviewChars = onReviewChars, onDone = onDone)
        }
    }
}

@Composable
private fun ScreenHeader(title: String, onBack: () -> Unit, close: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(
                if (close) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = I18nStore.t("common.back", "Volver"),
                tint = DcSlate,
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = DcInk)
    }
}

@Composable
private fun LearnState(state: ScriptUiState, onTakeExam: () -> Unit, onWrite: (String) -> Unit, onBack: () -> Unit) {
    val lesson = state.lesson ?: return
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(lesson.name, onBack = onBack)
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.info?.let { info ->
                item { ReadingMeterCard(info.readingScore) }
            }
            lesson.intro.takeIf { it.isNotBlank() }?.let {
                item { Text(it, style = MaterialTheme.typography.bodyMedium, color = DcSlate) }
            }
            items(lesson.items) { glyph ->
                GlyphCard(glyph, writingSupported = lesson.writingSupported, onWrite = onWrite)
            }
            item {
                Surface(color = DcMint, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Create, contentDescription = null, tint = DcMintDeep, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            I18nStore.t("script.examScope", "El examen solo incluye los {n} caracteres que acabas de ver.").replace("{n}", "${lesson.items.size}"),
                            style = MaterialTheme.typography.bodySmall,
                            color = DcMintDeep,
                        )
                    }
                }
            }
        }
        Column(Modifier.padding(20.dp)) {
            com.alturya.fluenta.ui.FluentaButton(
                text = I18nStore.t("script.takeExam", "Hacer examen de lectura"),
                onClick = onTakeExam,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text(I18nStore.t("common.back", "Volver"), color = DcSlate)
            }
        }
    }
}

@Composable
private fun ReadingMeterCard(score: Int) {
    Surface(color = Color.White, shape = RoundedCornerShape(16.dp), shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            RingMeter(score, size = 56.dp, stroke = 6.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(I18nStore.t("script.reading", "Lectura del nivel"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = DcInk)
                Text(I18nStore.t("script.readingHint", "Tu dominio de lectura actual"), style = MaterialTheme.typography.bodySmall, color = DcSlate)
            }
        }
    }
}

@Composable
private fun RingMeter(score: Int, size: androidx.compose.ui.unit.Dp, stroke: androidx.compose.ui.unit.Dp, onColor: Color = DcInk) {
    val pct = (score / 100f).coerceIn(0f, 1f)
    val primary = MaterialTheme.colorScheme.primary
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val sw = stroke.toPx()
            val inset = sw / 2
            val arcSize = Size(this.size.width - sw, this.size.height - sw)
            drawArc(
                color = DcHairline, startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset), size = arcSize,
                style = Stroke(width = sw, cap = StrokeCap.Round),
            )
            drawArc(
                color = primary, startAngle = -90f, sweepAngle = 360f * pct, useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset), size = arcSize,
                style = Stroke(width = sw, cap = StrokeCap.Round),
            )
        }
        Text("$score%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, color = onColor)
    }
}

@Composable
private fun GlyphCard(item: ScriptItem, writingSupported: Boolean, onWrite: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Surface(color = Color.White, shape = RoundedCornerShape(18.dp), shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(item.glyph, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(item.romanization, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = DcAmber)
                        item.meaning?.let {
                            Spacer(Modifier.width(8.dp))
                            Text("· $it", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = DcInk)
                        }
                    }
                    item.mnemonic?.let {
                        Text("💡 $it", style = MaterialTheme.typography.bodySmall, color = DcSlate)
                    }
                    val strokes = item.strokeCount?.let { "$it ${I18nStore.t("script.strokesShort", "trazos")}" }
                        ?: item.strokeOrder?.let { I18nStore.t("script.strokes", "Trazos: {s}").replace("{s}", it) }
                    strokes?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = DcSlate.copy(alpha = 0.8f))
                    }
                }
                IconButton(onClick = { scope.launch { TtsPlayer.play(context, item.glyph) } }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = I18nStore.t("convo.hear", "Escuchar"), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                }
            }
            if (writingSupported) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { onWrite(item.glyph) }) {
                    Icon(Icons.Default.Create, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(I18nStore.t("script.write", "Escribir"))
                }
            }
        }
    }
}

@Composable
private fun QuizState(state: ScriptUiState, onChoose: (Int, String) -> Unit, onGrade: () -> Unit, onBack: () -> Unit) {
    val quiz = state.quiz
    val total = quiz?.items?.size ?: 0
    val answered = state.answers.size
    Column(Modifier.fillMaxSize()) {
        // Cabecera con barra de progreso (respondidas / total)
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Close, contentDescription = I18nStore.t("common.back", "Volver"), tint = DcSlate)
            }
            LinearProgressIndicator(
                progress = { if (total == 0) 0f else answered.toFloat() / total },
                modifier = Modifier.weight(1f).height(12.dp).clip(RoundedCornerShape(99.dp)),
                trackColor = DcMint,
            )
            Spacer(Modifier.width(10.dp))
            Text("$answered/$total", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, color = DcInk)
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text(I18nStore.t("script.examPrompt", "¿Qué sonido tiene cada carácter?").uppercase(),
                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            }
            quiz?.items?.let { qitems ->
                itemsIndexed(qitems) { i, q ->
                    QuizQuestionCard(q.glyph, q.options, selected = state.answers[i], onChoose = { opt: String -> onChoose(i, opt) })
                }
            }
        }
        Box(Modifier.padding(20.dp)) {
            com.alturya.fluenta.ui.FluentaButton(
                text = I18nStore.t("script.grade", "Calificar"),
                onClick = onGrade,
                enabled = total > 0 && answered == total,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun QuizQuestionCard(glyph: String, options: List<String>, selected: String?, onChoose: (String) -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(18.dp), shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(glyph, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.ExtraBold, color = DcInk)
            Spacer(Modifier.height(12.dp))
            options.forEach { opt ->
                val isSel = selected == opt
                Surface(
                    onClick = { onChoose(opt) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSel) MaterialTheme.colorScheme.primary else Color.White,
                    border = if (isSel) null else androidx.compose.foundation.BorderStroke(2.dp, DcHairline),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).heightIn(min = 48.dp),
                ) {
                    Box(Modifier.fillMaxWidth().padding(14.dp), contentAlignment = Alignment.Center) {
                        Text(
                            opt,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) MaterialTheme.colorScheme.onPrimary else DcInk,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultState(state: ScriptUiState, onKeepLearning: () -> Unit, onReviewChars: () -> Unit, onDone: () -> Unit) {
    val r = state.result
    val mastery = r?.mastery ?: 0
    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        RingMeter(r?.score ?: 0, size = 160.dp, stroke = 12.dp, onColor = Color.White)
        Spacer(Modifier.height(16.dp))
        Text(I18nStore.t("script.goodExam", "¡Buen examen!"), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Color.White)
        Spacer(Modifier.height(4.dp))
        Text(
            I18nStore.t("script.gotRight", "Acertaste {c} de {t}").replace("{c}", "${r?.correct ?: 0}").replace("{t}", "${r?.total ?: 0}"),
            style = MaterialTheme.typography.titleMedium, color = DcMintDeep, fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            val masteryLabel = when {
                mastery >= 80 -> I18nStore.t("script.masteryLevelHigh", "Avanzado")
                mastery >= 40 -> I18nStore.t("script.masteryLevelMid", "Intermedio")
                else -> I18nStore.t("script.masteryLevelLow", "Inicial")
            }
            ResultStatCard(masteryLabel, I18nStore.t("script.masteryLabel", "Dominio lectura"), Modifier.weight(1f), valueColor = MaterialTheme.colorScheme.primary)
            ResultStatCard("$mastery%", I18nStore.t("script.reading", "Lectura"), Modifier.weight(1f), valueColor = DcAmber)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            when {
                mastery >= 80 -> I18nStore.t("script.masteryHigh", "¡Ya lees este alfabeto! La transliteración se irá apagando para que leas de verdad.")
                mastery >= 40 -> I18nStore.t("script.masteryMid", "Vas bien. Seguimos mostrando la lectura latina solo donde la necesitas.")
                else -> I18nStore.t("script.masteryLow", "Recién empiezas — practica los trazos y vuelve a intentarlo.")
            },
            style = MaterialTheme.typography.bodyMedium, color = DcMintDeep, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))
        Surface(color = Color.White, shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                com.alturya.fluenta.ui.FluentaButton(
                    text = I18nStore.t("script.keepPracticing", "Seguir aprendiendo"),
                    onClick = onKeepLearning,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = onReviewChars, modifier = Modifier.fillMaxWidth()) {
                    Text(I18nStore.t("hanzi.review.cta", "Repasar caracteres (SRS) →"))
                }
                TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                    Text(I18nStore.t("common.done", "Listo"), color = DcSlate)
                }
            }
        }
    }
}

@Composable
private fun ResultStatCard(value: String, label: String, modifier: Modifier = Modifier, valueColor: Color = DcInk) {
    Surface(color = Color.White.copy(alpha = 0.95f), shape = RoundedCornerShape(18.dp), modifier = modifier) {
        Column(Modifier.padding(vertical = 14.dp, horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = valueColor)
            Text(label, style = MaterialTheme.typography.labelMedium, color = DcSlate, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun LoadingState() {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ShimmerBox(Modifier.fillMaxWidth().height(80.dp), shape = RoundedCornerShape(16.dp))
        repeat(4) {
            ShimmerCard(height = 92.dp, modifier = Modifier.clip(RoundedCornerShape(18.dp)))
        }
    }
}

@Composable
private fun EmptyState(onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(96.dp).clip(RoundedCornerShape(22.dp)).background(DcMint),
            contentAlignment = Alignment.Center,
        ) { Text("字", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary) }
        Spacer(Modifier.height(18.dp))
        Text(I18nStore.t("script.emptyTitle", "Aún no hay caracteres"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = DcInk, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            I18nStore.t("script.emptyBody", "Este idioma no usa un sistema de escritura propio o todavía no está disponible."),
            style = MaterialTheme.typography.bodyMedium, color = DcSlate, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = onBack) { Text(I18nStore.t("script.backToLessons", "Volver a lecciones")) }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(com.alturya.fluenta.R.drawable.ic_fluenta_saluda),
            contentDescription = null,
            modifier = Modifier.size(104.dp),
        )
        Spacer(Modifier.height(20.dp))
        Text(
            message.ifBlank { I18nStore.t("script.errorTitle", "No se cargaron los caracteres") },
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DcInk, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        com.alturya.fluenta.ui.FluentaButton(text = I18nStore.t("common.retry", "Reintentar"), onClick = onRetry)
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBack) { Text(I18nStore.t("common.back", "Volver"), color = DcSlate) }
    }
}
