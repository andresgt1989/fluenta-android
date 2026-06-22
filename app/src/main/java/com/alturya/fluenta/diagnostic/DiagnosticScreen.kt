package com.alturya.fluenta.diagnostic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.ui.HootMascot

private object Tz {
    val BgTop = Color(0xFFF2FBF8)
    val BgMid = Color(0xFFE4F6F1)
    val BgBot = Color(0xFFCDEEE6)
    val Teal = Color(0xFF0E9D8E)
    val TealDark = Color(0xFF0A6F64)
    val Teal2 = Color(0xFF13B0A0)
    val Ink = Color(0xFF0A6F64)
    val Sub = Color(0xFF5E726C)
    val Mint = Color(0xFFCDEEE6)
    val Track = Color(0xFFD7EBE5)
    val SelBg = Color(0xFFE4F6F1)
}

@Composable
fun DiagnosticScreen(onDone: () -> Unit = {}) {
    val vm: DiagnosticViewModel = viewModel()
    val state by vm.state.collectAsState()

    Box(Modifier.fillMaxSize()) {
        when (val s = state) {
            is DiagnosticUiState.Intro -> IntroState(onStart = { vm.start() }, onLater = onDone)
            is DiagnosticUiState.Loading -> LoadingState()
            is DiagnosticUiState.Quiz -> QuizState(s, onAnswer = { vm.answer(it) }, onSkip = { vm.skip() }, onBack = onDone)
            is DiagnosticUiState.Result -> ResultState(s, onStart = onDone)
            is DiagnosticUiState.Error -> ErrorState(s.message, onRetry = { vm.start() }, onLater = onDone)
        }
    }
}

@Composable
private fun IntroState(onStart: () -> Unit, onLater: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Tz.BgMid, 0.4f to Tz.BgMid, 1f to Tz.BgBot))
            .verticalScroll(rememberScrollState()).padding(horizontal = 28.dp).padding(top = 24.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HootMascot(Modifier.size(120.dp))
        Text(I18nStore.t("diagnostic.introTitle", "Test de nivel"), fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Tz.Ink, modifier = Modifier.padding(top = 14.dp))
        Text(I18nStore.t("diagnostic.introSub", "6 preguntas adaptativas · menos de 2 min"), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF3E8A80), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(22.dp))
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = Color.White, shadowElevation = 4.dp) {
            Column(Modifier.padding(18.dp)) {
                Surface(shape = RoundedCornerShape(999.dp), color = Tz.Mint) {
                    Text(I18nStore.t("diagnostic.example", "Ejemplo").uppercase(), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, color = Tz.TealDark, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
                Text(I18nStore.t("diagnostic.examplePrompt", "Las preguntas se ajustan a tu nivel real."), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF13524B), modifier = Modifier.padding(top = 14.dp))
            }
        }
        Spacer(Modifier.height(28.dp))
        Hard3dT(Modifier.fillMaxWidth(), onClick = onStart) {
            Text(I18nStore.t("diagnostic.introCta", "Empezar el test"), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(14.dp))
        Text(I18nStore.t("diagnostic.introLater", "Hacerlo más tarde"), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Tz.TealDark,
            modifier = Modifier.heightIn(min = 48.dp).clickable(onClick = onLater).padding(8.dp))
    }
}

@Composable
private fun LoadingState() {
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Tz.BgTop, 1f to Tz.BgMid)), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(130.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.size(130.dp), color = Tz.Teal, trackColor = Tz.Mint, strokeWidth = 5.dp)
            HootMascot(Modifier.size(84.dp))
        }
        Text(I18nStore.t("diagnostic.loadTitle", "Preparando tu test…"), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Tz.Ink, modifier = Modifier.padding(top = 30.dp))
        Text(I18nStore.t("diagnostic.loadSub", "Ajustamos la dificultad a tu ritmo"), fontSize = 15.sp, color = Tz.Sub, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun QuizState(s: DiagnosticUiState.Quiz, onAnswer: (Int) -> Unit, onSkip: () -> Unit, onBack: () -> Unit) {
    var selected by remember(s.question.prompt) { mutableStateOf<Int?>(null) }
    val pct = if (s.progress.total == 0) 0f else s.progress.current.toFloat() / s.progress.total
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Tz.BgTop, 1f to Tz.BgMid)).padding(horizontal = 24.dp).padding(top = 16.dp, bottom = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color.White).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Text("‹", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Tz.TealDark)
            }
            Column(Modifier.weight(1f)) {
                Text(I18nStore.t("diagnostic.qOf", "Pregunta {a} de {b}").replace("{a}", "${s.progress.current}").replace("{b}", "${s.progress.total}"),
                    fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Tz.TealDark)
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(999.dp)).background(Tz.Track)) {
                    Box(Modifier.fillMaxWidth(pct).fillMaxHeight().clip(RoundedCornerShape(999.dp)).background(Brush.horizontalGradient(listOf(Tz.Teal2, Tz.Teal))))
                }
            }
        }
        Text(s.question.prompt, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Tz.Ink, lineHeight = 30.sp, modifier = Modifier.padding(top = 26.dp))
        Text(I18nStore.t("diagnostic.qHint", "Toca la opción correcta"), fontSize = 14.sp, color = Tz.Sub, modifier = Modifier.padding(top = 4.dp, bottom = 18.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            s.question.options.forEachIndexed { i, opt ->
                val isSel = selected == i
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(RoundedCornerShape(18.dp))
                        .background(if (isSel) Tz.SelBg else Color.White)
                        .clickable(enabled = !s.answering) { selected = i; onAnswer(i) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(opt, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (isSel) Tz.TealDark else Color(0xFF13524B), modifier = Modifier.weight(1f))
                    if (isSel) {
                        Box(Modifier.size(26.dp).clip(RoundedCornerShape(13.dp)).background(Tz.Teal), contentAlignment = Alignment.Center) {
                            Text("✓", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
        Text(I18nStore.t("diagnostic.dontKnow", "No lo sé"), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Tz.Sub,
            modifier = Modifier.align(Alignment.CenterHorizontally).heightIn(min = 48.dp).clickable(enabled = !s.answering, onClick = onSkip).padding(8.dp))
    }
}

@Composable
private fun ResultState(s: DiagnosticUiState.Result, onStart: () -> Unit) {
    val level = s.level.uppercase()
    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Tz.Teal, 1f to Tz.TealDark))
            .verticalScroll(rememberScrollState()).padding(horizontal = 28.dp).padding(top = 24.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HootMascot(Modifier.size(120.dp))
        Text(I18nStore.t("diagnostic.resEyebrow", "Tu nivel estimado").uppercase(), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp, color = Color(0xFFA9E6DA), modifier = Modifier.padding(top = 14.dp, bottom = 12.dp))
        Text(level, fontSize = 54.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf("A1", "A2", "B1", "B2", "C1", "C2").forEach { lv ->
                val on = lv == level
                Box(Modifier.size(width = 42.dp, height = 40.dp).clip(RoundedCornerShape(12.dp)).background(if (on) Color.White else Color.White.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                    Text(lv, fontSize = if (on) 15.sp else 14.sp, fontWeight = if (on) FontWeight.ExtraBold else FontWeight.Bold, color = if (on) Tz.TealDark else Color(0xFFBFE8DD))
                }
            }
        }
        Text(
            I18nStore.t("diagnostic.resDesc", "Entiendes frases cotidianas y mantienes conversaciones simples sobre temas conocidos."),
            fontSize = 15.sp, color = Color(0xFFE4F6F1), textAlign = TextAlign.Center, lineHeight = 22.sp, modifier = Modifier.padding(top = 22.dp).widthIn(max = 300.dp),
        )
        Spacer(Modifier.height(28.dp))
        Box(Modifier.fillMaxWidth().height(56.dp)) {
            Box(Modifier.fillMaxWidth().height(52.dp).align(Alignment.BottomCenter).clip(RoundedCornerShape(18.dp)).background(Color(0xFF073F39)))
            Box(Modifier.fillMaxWidth().height(52.dp).align(Alignment.TopCenter).clip(RoundedCornerShape(18.dp)).background(Color.White).clickable(onClick = onStart), contentAlignment = Alignment.Center) {
                Text(I18nStore.t("diagnostic.resCta", "Empezar a aprender"), color = Tz.TealDark, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ErrorState(msg: String, onRetry: () -> Unit, onLater: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Tz.BgTop, 1f to Tz.BgMid)).padding(horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        HootMascot(Modifier.size(110.dp), sad = true)
        Text(I18nStore.t("diagnostic.errTitle", "Algo salió mal"), fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, color = Tz.Ink, modifier = Modifier.padding(top = 24.dp))
        Text(msg.ifBlank { I18nStore.t("diagnostic.errSub", "No pudimos cargar la pregunta. Revisa tu conexión.") }, fontSize = 15.sp, color = Tz.Sub, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(34.dp))
        Hard3dT(Modifier.fillMaxWidth(), onClick = onRetry) {
            Text(I18nStore.t("common.retry", "Reintentar"), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(14.dp))
        Text(I18nStore.t("common.later", "Más tarde"), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Tz.Sub,
            modifier = Modifier.heightIn(min = 48.dp).clickable(onClick = onLater).padding(8.dp))
    }
}

@Composable
private fun Hard3dT(modifier: Modifier = Modifier, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(modifier.height(56.dp)) {
        Box(Modifier.fillMaxWidth().height(52.dp).align(Alignment.BottomCenter).clip(RoundedCornerShape(18.dp)).background(Tz.TealDark))
        Box(Modifier.fillMaxWidth().height(52.dp).align(Alignment.TopCenter).clip(RoundedCornerShape(18.dp)).background(Tz.Teal).clickable(onClick = onClick), contentAlignment = Alignment.Center) { content() }
    }
}
