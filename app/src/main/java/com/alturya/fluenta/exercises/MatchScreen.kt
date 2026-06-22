package com.alturya.fluenta.exercises

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.ui.FeedbackBar

/* Paleta del kit Claude Design (7 Match.dc.html) */
private object Mz {
    val Bg = Color(0xFFF1FAF6)
    val Emerald = Color(0xFF10B981)
    val EmeraldDark = Color(0xFF059669)
    val Ink = Color(0xFF0F2E27)
    val Muted = Color(0xFF5B7268)
    val Track = Color(0xFFCDEEE6)
    val MatchedBg = Color(0xFFD6F4E4)
    val MatchedInk = Color(0xFF0B7B53)
    val WrongBg = Color(0xFFFDE7E4)
    val WrongBorder = Color(0xFFE8554B)
    val WrongInk = Color(0xFFC13B32)
    val TileBorder = Color(0xFFE7EFEB)
    val Amber = Color(0xFFE08A00)
}

@Composable
fun MatchScreen(onDone: () -> Unit = {}, previewState: MatchState? = null) {
    val vm: MatchViewModel = viewModel()
    val vmState by vm.state.collectAsState()
    val state = previewState ?: vmState
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(state.wrongFlash) {
        if (state.wrongFlash != null) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    LaunchedEffect(state.won) {
        if (state.won) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    Box(Modifier.fillMaxSize().background(Mz.Bg)) {
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Mz.Emerald, trackColor = Mz.Track)
            }
            state.empty -> LockedState(onDone)
            state.error != null -> ErrorState(state.error!!, onRetry = { vm.load() })
            state.won -> WinPanel(state.attempts, state.answerKey.size, onAgain = { vm.load() }, onDone = onDone)
            else -> GameBoard(state, vm, onDone)
        }
    }
}

@Composable
private fun GameBoard(state: MatchState, vm: MatchViewModel, onDone: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 26.dp).padding(top = 14.dp)) {
        // Top bar: close + progress + count
        val progress by animateFloatAsState(
            targetValue = if (state.answerKey.isEmpty()) 0f else state.matched.size.toFloat() / state.answerKey.size,
            label = "match_progress"
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("✕", fontSize = 22.sp, color = Mz.Muted, modifier = Modifier.clickable(onClick = onDone))
            Box(Modifier.weight(1f).height(12.dp).clip(RoundedCornerShape(99.dp)).background(Mz.Track)) {
                Box(Modifier.fillMaxWidth(progress).fillMaxHeight().clip(RoundedCornerShape(99.dp)).background(Mz.Emerald))
            }
            Text("${state.matched.size}/${state.answerKey.size}", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Mz.Ink)
        }
        Spacer(Modifier.height(14.dp))
        Text(I18nStore.t("match.title", "Empareja los pares"), fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = Mz.Ink,
            modifier = Modifier.semantics { heading() })
        Text(I18nStore.t("match.subtitle", "Toca una palabra y su traducción"), fontSize = 14.sp, color = Mz.Muted, modifier = Modifier.padding(top = 2.dp))
        Spacer(Modifier.height(18.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            // Left column — L2
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.leftItems.forEach { item ->
                    val matched = state.matched.contains(item)
                    val selected = state.selectedLeft == item
                    val wrong = state.wrongFlash?.first == item
                    TileMz(item, matched = matched, selected = selected, wrong = wrong, enabled = !matched) { vm.tapLeft(item) }
                }
            }
            // Right column — L1
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.rightItems.forEach { item ->
                    val matched = state.matched.any { state.answerKey[it] == item }
                    val wrong = state.wrongFlash?.second == item
                    TileMz(item, matched = matched, selected = false, wrong = wrong, enabled = !matched) { vm.tapRight(item) }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        // Legend
        Row(Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            LegendDot(Mz.Emerald, I18nStore.t("match.legendSelected", "Seleccionado"))
            Spacer(Modifier.width(10.dp))
            LegendDot(Mz.Emerald.copy(alpha = 0.5f), I18nStore.t("match.legendCorrect", "Correcto"))
            Spacer(Modifier.width(10.dp))
            LegendDot(Mz.WrongBorder, I18nStore.t("match.legendWrong", "Error"))
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Spacer(Modifier.width(5.dp))
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Mz.Muted)
    }
}

@Composable
private fun TileMz(text: String, matched: Boolean, selected: Boolean, wrong: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val face: Color; val border: Color; val ink: Color; val shadow: Color; val alpha: Float
    when {
        matched -> { face = Mz.MatchedBg; border = Mz.Emerald; ink = Mz.MatchedInk; shadow = Color.Transparent; alpha = 0.65f }
        wrong -> { face = Mz.WrongBg; border = Mz.WrongBorder; ink = Mz.WrongInk; shadow = Color.Transparent; alpha = 1f }
        selected -> { face = Mz.Emerald; border = Mz.Emerald; ink = Color.White; shadow = Mz.EmeraldDark; alpha = 1f }
        else -> { face = Color.White; border = Mz.TileBorder; ink = Mz.Ink; shadow = Mz.TileBorder; alpha = 1f }
    }
    val depth = if (shadow == Color.Transparent) 0.dp else 3.dp
    Box(Modifier.fillMaxWidth().height(64.dp + depth)) {
        if (depth > 0.dp) {
            Box(Modifier.fillMaxWidth().height(64.dp).align(Alignment.BottomCenter).clip(RoundedCornerShape(15.dp)).background(shadow))
        }
        Box(
            Modifier.fillMaxWidth().height(64.dp).align(Alignment.TopCenter)
                .clip(RoundedCornerShape(15.dp))
                .background(face.copy(alpha = if (alpha < 1f) alpha else 1f))
                .border(2.dp, border.copy(alpha = if (alpha < 1f) alpha else 1f), RoundedCornerShape(15.dp))
                .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (matched) "$text ✓" else text,
                color = ink.copy(alpha = if (alpha < 1f) alpha else 1f),
                fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }
}

@Composable
private fun WinPanel(attempts: Int, total: Int, onAgain: () -> Unit, onDone: () -> Unit) {
    val accuracy = if (attempts > 0) (total * 100 / attempts) else 100
    val xp = total * 10
    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Mz.Emerald, 0.45f to Color(0xFF34C79B), 1f to Mz.Track)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        val transition = rememberInfiniteTransition(label = "owl")
        val dy by transition.animateFloat(
            initialValue = 0f, targetValue = -8f,
            animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "owl_dy",
        )
        AnimatedVisibility(visible = true, enter = fadeIn() + scaleIn()) {
            Text("🦉", fontSize = 96.sp, modifier = Modifier.offset(y = dy.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(I18nStore.t("match.completedTitle", "¡Todos emparejados!"), fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 30.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            WinStat("🎯", "$accuracy%", I18nStore.t("match.accuracy", "Precisión"), Mz.MatchedInk, Modifier.weight(1f))
            WinStat("🧩", "$total", I18nStore.t("match.pairs", "Pares"), Mz.Ink, Modifier.weight(1f))
            WinStat("⚡", "+$xp", "XP", Mz.Amber, Modifier.weight(1f))
        }
        Spacer(Modifier.weight(1f))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)).background(Color.White).padding(horizontal = 26.dp).padding(top = 22.dp, bottom = 26.dp)) {
            Hard3dMz(modifier = Modifier.fillMaxWidth(), face = Mz.Emerald, shadow = Mz.EmeraldDark, height = 56.dp, onClick = onAgain) {
                Text(I18nStore.t("match.playAgain", "Jugar de nuevo"), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(14.dp))
            Text(I18nStore.t("match.backToMap", "Volver al mapa"), color = Mz.Muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().clickable(onClick = onDone))
            Spacer(Modifier.height(10.dp))
            FeedbackBar(surface = "match")
        }
    }
}

@Composable
private fun WinStat(emoji: String, value: String, label: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.95f)).padding(vertical = 16.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, fontSize = 22.sp)
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = valueColor, modifier = Modifier.padding(top = 4.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Mz.Muted)
    }
}

@Composable
private fun LockedState(onDone: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(104.dp).clip(RoundedCornerShape(52.dp)).background(Color(0xFFE3EFEA)), contentAlignment = Alignment.Center) {
            Text("🔒", fontSize = 46.sp)
        }
        Spacer(Modifier.height(18.dp))
        Text(I18nStore.t("match.lockedTitle", "Aún no disponible"), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Mz.Ink, textAlign = TextAlign.Center)
        Text(I18nStore.t("match.locked", "Completa lecciones para desbloquear este juego de vocabulario."), fontSize = 15.sp, color = Mz.Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(22.dp))
        Hard3dMz(modifier = Modifier.fillMaxWidth(), face = Mz.Emerald, shadow = Mz.EmeraldDark, height = 56.dp, onClick = onDone) {
            Text(I18nStore.t("match.keepLearning", "Seguir aprendiendo"), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ErrorState(msg: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("🦉", fontSize = 84.sp)
        Spacer(Modifier.height(16.dp))
        Text(I18nStore.t("match.errorTitle", "No se pudo iniciar el juego"), fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = Mz.Ink, textAlign = TextAlign.Center)
        Text(msg, fontSize = 15.sp, color = Mz.Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(20.dp))
        Hard3dMz(modifier = Modifier.fillMaxWidth(), face = Mz.Emerald, shadow = Mz.EmeraldDark, height = 52.dp, onClick = onRetry) {
            Text(I18nStore.t("common.retry", "Reintentar"), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Hard3dMz(
    face: Color,
    shadow: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 56.dp,
    content: @Composable () -> Unit,
) {
    val depth = 4.dp
    Box(modifier.height(height + depth)) {
        Box(Modifier.fillMaxWidth().height(height).align(Alignment.BottomCenter).clip(RoundedCornerShape(14.dp)).background(shadow))
        Box(
            Modifier.fillMaxWidth().height(height).align(Alignment.TopCenter).clip(RoundedCornerShape(14.dp)).background(face).clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { content() }
    }
}
