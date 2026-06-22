package com.alturya.fluenta.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.ui.ShimmerCard
import com.alturya.fluenta.network.ErrorItem
import com.alturya.fluenta.util.levelLabel
import com.alturya.fluenta.util.levelSystemName

/* Paleta del kit Claude Design (10 Progress.dc.html) */
private object Pz {
    val Bg = Color(0xFFE3EFEA)
    val Card = Color(0xFFF1FAF6)
    val Emerald = Color(0xFF10B981)
    val EmeraldDark = Color(0xFF059669)
    val Mint = Color(0xFF34C79B)
    val Ink = Color(0xFF0F2E27)
    val HeaderInk = Color(0xFF063D32)
    val Muted = Color(0xFF5B7268)
    val Amber = Color(0xFFE08A00)
    val MasteredBg = Color(0xFFD6F4E4)
    val MasteredInk = Color(0xFF0B7B53)
    val DueBg = Color(0xFFFFF3DC)
    val DueInk = Color(0xFFB86E00)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(previewState: ProgressState? = null) {
    val vm: ProgressViewModel = viewModel()
    val vmState by vm.state.collectAsState()
    val state = previewState ?: vmState
    val pullState = rememberPullToRefreshState()
    if (pullState.isRefreshing) {
        LaunchedEffect(Unit) {
            if (previewState == null) vm.load()
            pullState.endRefresh()
        }
    }

    if (state.loading) {
        Column(Modifier.fillMaxSize().background(Pz.Bg).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ShimmerCard(height = 110.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ShimmerCard(height = 74.dp, modifier = Modifier.weight(1f))
                ShimmerCard(height = 74.dp, modifier = Modifier.weight(1f))
                ShimmerCard(height = 74.dp, modifier = Modifier.weight(1f))
            }
            ShimmerCard(height = 160.dp)
        }
        return
    }

    val p = state.profile
    Box(Modifier.fillMaxSize().background(Pz.Bg).nestedScroll(pullState.nestedScrollConnection)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Hero esmeralda: saludo + nivel + 3 stat pills ──
            item {
                Column(
                    Modifier.fillMaxWidth()
                        .background(Brush.verticalGradient(0f to Pz.Emerald, 1f to Pz.Mint))
                        .padding(horizontal = 24.dp).padding(top = 20.dp, bottom = 22.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(Modifier.size(56.dp).clip(RoundedCornerShape(28.dp)).background(Color.White.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
                            Text("🦉", fontSize = 26.sp)
                        }
                        Column {
                            Text(I18nStore.t("progress.title", "Tu progreso"), fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
                                modifier = Modifier.semantics { heading() })
                            Text(
                                "${levelLabel(p?.level, p?.levelSystem)} · ${levelSystemName(p?.levelSystem)}",
                                fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Pz.HeaderInk,
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        HeroStat("🔥", "${state.progress?.streakDays ?: 0}", I18nStore.t("home.streak", "Racha"), Pz.Ink, Modifier.weight(1f))
                        HeroStat("⚡", "${state.progress?.totalXp ?: 0}", I18nStore.t("home.xp", "XP total"), Pz.Amber, Modifier.weight(1f))
                        HeroStat("📚", "${state.progress?.completedLessons ?: 0}", I18nStore.t("home.lessons", "Lecciones"), Pz.Ink, Modifier.weight(1f))
                    }
                }
            }

            // Weekly league leaderboard (retention mechanic)
            item { Box(Modifier.padding(horizontal = 20.dp)) { LeagueCard() } }

            // ── SRS: errores dominados / para repasar hoy ──
            val masteredCount = state.errors.count { it.masteredAt != null }
            val dueToday = state.errors.count { e ->
                e.masteredAt == null && run {
                    val d = e.nextReviewAt
                    d == null || runCatching {
                        java.time.temporal.ChronoUnit.DAYS.between(java.time.Instant.now(), java.time.Instant.parse(d)) <= 0
                    }.getOrDefault(true)
                }
            }
            if (state.errors.isNotEmpty()) {
                item {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SrsCard("$masteredCount", I18nStore.t("progress.erroresMastered", "Errores dominados"), Pz.MasteredBg, Pz.MasteredInk, Modifier.weight(1f))
                        SrsCard("$dueToday", I18nStore.t("progress.dueToday", "Para repasar hoy"), Pz.DueBg, Pz.DueInk, Modifier.weight(1f))
                    }
                }
            }

            // Error pattern analysis
            val unmasteredErrors = state.errors.filter { it.masteredAt == null }
            if (unmasteredErrors.size >= 3) {
                item { Box(Modifier.padding(horizontal = 20.dp)) { ErrorPatternCard(unmasteredErrors) } }
            }

            // ── Habilidades (radar) ──
            item {
                Text(I18nStore.t("progress.skillsTitle", "Tus habilidades"), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Pz.Ink,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp))
            }
            val hasSkillData = state.skills.any { it.total > 0 } || state.taskSuccessRate > 0
            if (hasSkillData) {
                item {
                    Surface(Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(18.dp), color = Color.White) {
                        Column {
                            SkillsRadar(state.skills)
                            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                                if (state.wpm > 0) MiniMetric("${state.wpm}", I18nStore.t("progress.wpm", "palabras/min"))
                                MiniMetric("${state.taskSuccessRate}%", I18nStore.t("progress.successRate", "éxito tareas"))
                            }
                        }
                    }
                }
            } else {
                item {
                    Surface(Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(18.dp), color = Color.White) {
                        Text(
                            I18nStore.t("progress.radarEmpty", "Tu radar de habilidades aparecerá aquí cuando completes lecciones. Mide gramática, vocabulario, pronunciación y fluidez."),
                            fontSize = 14.sp, color = Pz.Muted, modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            }

            // ── SRS review board ──
            item {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    Text(I18nStore.t("progress.srsTitle", "Repaso inteligente (SRS)"), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Pz.Ink, modifier = Modifier.padding(top = 6.dp))
                    Text(I18nStore.t("progress.srsSubtitle", "Errores a consolidar — repaso espaciado."), fontSize = 13.sp, color = Pz.Muted)
                }
            }
            if (state.errors.isEmpty()) {
                item {
                    Text(
                        I18nStore.t("progress.errorsEmpty", "Aún no hay errores registrados. ¡Sigue practicando!"),
                        fontSize = 14.sp, color = Pz.Muted, modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            } else {
                items(state.errors) { err -> Box(Modifier.padding(horizontal = 20.dp)) { ErrorRow(err) } }
            }
        }
        PullToRefreshContainer(state = pullState, modifier = Modifier.align(Alignment.TopCenter))
    }
}

@Composable
private fun HeroStat(emoji: String, value: String, label: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.95f)).padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, fontSize = 22.sp)
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = valueColor)
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Pz.Muted, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SrsCard(value: String, label: String, bg: Color, ink: Color, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(bg).padding(14.dp)) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = ink)
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ink)
    }
}

@Composable
private fun MiniMetric(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Pz.Ink)
        Text(label, fontSize = 11.sp, color = Pz.Muted)
    }
}

@Composable
private fun ErrorRow(err: ErrorItem) {
    val mastered = err.masteredAt != null
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Color.White) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                err.errorType?.let {
                    Surface(shape = RoundedCornerShape(99.dp), color = Pz.MasteredBg) {
                        Text(skillLabel(it), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Pz.MasteredInk, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                }
                Spacer(Modifier.weight(1f))
                Text(
                    if (mastered) I18nStore.t("progress.mastered", "Dominado ✓") else I18nStore.t("progress.toReview", "Por repasar"),
                    fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    color = if (mastered) Pz.EmeraldDark else Pz.Muted,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Close, contentDescription = I18nStore.t("progress.wrongAnswer", "Tu respuesta"), tint = Color(0xFFE8554B), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(err.original ?: "", fontSize = 14.sp, color = Pz.Ink)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = I18nStore.t("progress.correctAnswer", "Forma correcta"), tint = Pz.Emerald, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(err.corrected ?: "", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Pz.Ink)
            }
            val reviews = err.reviewCount ?: 0
            if (reviews > 0) {
                Spacer(Modifier.height(4.dp))
                Text(I18nStore.plural("progress.reviewedCount", reviews, one = "Repasado {n} vez", other = "Repasado {n} veces"), fontSize = 11.sp, color = Pz.Muted)
            }
            err.nextReviewAt?.let { dateStr ->
                val daysLeft = runCatching {
                    val instant = java.time.Instant.parse(dateStr)
                    java.time.temporal.ChronoUnit.DAYS.between(java.time.Instant.now(), instant).toInt().coerceAtLeast(0)
                }.getOrNull()
                if (daysLeft != null && !mastered) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (daysLeft == 0) I18nStore.t("srs.reviewToday", "Repasar hoy")
                        else I18nStore.t("srs.nextReview", "Próximo repaso en $daysLeft días").replace("$daysLeft", "$daysLeft"),
                        fontSize = 11.sp,
                        color = if (daysLeft == 0) Pz.Emerald else Pz.Muted,
                    )
                }
            }
        }
    }
}

private fun skillLabel(type: String): String = when (type) {
    "grammar" -> I18nStore.t("skill.grammar", "Gramática")
    "vocab" -> I18nStore.t("skill.vocabulary", "Vocabulario")
    "pronunciation" -> I18nStore.t("skill.pronunciation", "Pronunciación")
    "fluency" -> I18nStore.t("skill.fluency", "Fluidez")
    else -> type
}

@Composable
private fun ErrorPatternCard(errors: List<com.alturya.fluenta.network.ErrorItem>) {
    val byType = errors.groupBy { it.errorType ?: "other" }
    val total = errors.size
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = Color.White) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = Pz.Emerald, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(I18nStore.t("progress.patterns", "Tus patrones de error"), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Pz.Ink)
            }
            Spacer(Modifier.height(10.dp))
            byType.entries.sortedByDescending { it.value.size }.take(3).forEach { (type, errs) ->
                val pct = (errs.size * 100) / total
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(skillLabel(type), fontSize = 13.sp, color = Pz.Ink, modifier = Modifier.width(96.dp))
                    LinearProgressIndicator(
                        progress = { pct / 100f },
                        modifier = Modifier.weight(1f).height(6.dp),
                        color = Pz.Emerald,
                        trackColor = Pz.MasteredBg,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("$pct%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Pz.Emerald, modifier = Modifier.width(32.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(I18nStore.t("progress.patternsHint", "Enfócate en el área con más errores para progresar más rápido"), fontSize = 11.sp, color = Pz.Muted)
        }
    }
}
