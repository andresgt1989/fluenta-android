package com.alturya.fluenta.gamification

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.data.StreakStore
import java.time.LocalDate

// ── Tokens exactos del kit Claude Design (Fluenta Gamificación.dc.html · P1) ──
private val Ink = Color(0xFF15201D)
private val Muted = Color(0xFF5C6562)
private val Teal = Color(0xFF0E9D8E)
private val Amber = Color(0xFFE08A00)
private val AmberInk = Color(0xFF9A5E00)
private val AmberInk2 = Color(0xFFA87B2E)
private val HeroTop = Color(0xFFFFF6E6)
private val HeroBottom = Color(0xFFFFE9C2)
private val DayActiveBg = Color(0xFFFFE9C2)
private val DayEmptyBg = Color(0xFFF1F5F3)
private val DayFrozenBg = Color(0xFFE7EEEB)
private val Frost = Color(0xFF3D8BCD)
private val FrostBg = Color(0xFFE3F1FB)
private val FrostCardBg = Color(0xFFF3F9FD)
private val FrostBorder = Color(0xFFDCEAF3)
private val CardBorder = Color(0xFFEAF0ED)
private val MutedGray = Color(0xFF9AA39E)
private val Track = Color(0xFFFFE9C2)

/**
 * Racha · detalle (handoff Gamificación · pantalla 1). Conectada a progreso REAL:
 * `streakDays`/`todayXp` vienen del backend (vía nav-args desde HOME); la mejor
 * racha y los escudos se persisten client-side en [StreakStore].
 */
@Composable
fun StreakScreen(
    streakDays: Int,
    todayXp: Int,
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val savedBest by remember { StreakStore.best(context) }.collectAsState(initial = 0)
    val shields by remember { StreakStore.shields(context) }.collectAsState(initial = 1)

    val studiedToday = todayXp > 0
    val best = Streak.bestStreak(savedBest, streakDays)
    val nextMs = Streak.nextMilestone(streakDays)
    val msProgress = Streak.milestoneProgress(streakDays)
    val todayIdx = LocalDate.now().dayOfWeek.value - 1 // 0=Lun..6=Dom
    val week = Streak.weekTracker(todayIdx, streakDays, studiedToday)
    val dayLabels = listOf("L", "M", "X", "J", "V", "S", "D")

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFFBFCFB))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 8.dp, bottom = 24.dp),
    ) {
        // ── Top bar ───────────────────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack, contentDescription = I18nStore.t("common.back", "Atrás"),
                tint = MutedGray, modifier = Modifier.size(24.dp).clickable(onClick = onBack),
            )
            Spacer(Modifier.width(8.dp))
            Text(I18nStore.t("streak.title", "Tu racha"), color = Ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        }

        Spacer(Modifier.height(6.dp))

        // ── Hero flame ────────────────────────────────────────────────────────
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.verticalGradient(listOf(HeroTop, HeroBottom)))
                .padding(vertical = 24.dp, horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(96.dp).clip(CircleShape).background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Amber, modifier = Modifier.size(60.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text("$streakDays", color = AmberInk, fontSize = 46.sp, fontWeight = FontWeight.ExtraBold)
            Text(I18nStore.t("streak.daysInARow", "días seguidos"), color = AmberInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            if (best > 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    I18nStore.t("streak.best", "Tu mejor racha: {n} días").replace("{n}", "$best"),
                    color = AmberInk2, fontSize = 12.5.sp,
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        // ── Rastreador semanal ────────────────────────────────────────────────
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            week.forEachIndexed { i, state ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .then(
                                when (state) {
                                    Streak.DayState.ACTIVE -> Modifier.background(DayActiveBg)
                                    Streak.DayState.TODAY -> Modifier.background(Color.White).border(2.5.dp, Amber, CircleShape)
                                    Streak.DayState.FUTURE -> Modifier.background(DayEmptyBg)
                                    Streak.DayState.EMPTY -> Modifier.background(DayFrozenBg)
                                }
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        when (state) {
                            Streak.DayState.ACTIVE ->
                                Icon(Icons.Default.LocalFireDepartment, null, tint = Amber, modifier = Modifier.size(20.dp))
                            Streak.DayState.TODAY ->
                                Icon(Icons.Default.Bolt, null, tint = Amber, modifier = Modifier.size(18.dp))
                            Streak.DayState.EMPTY ->
                                Icon(Icons.Default.AcUnit, null, tint = MutedGray, modifier = Modifier.size(16.dp))
                            Streak.DayState.FUTURE -> {}
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    val isToday = state == Streak.DayState.TODAY
                    Text(
                        if (isToday) I18nStore.t("streak.today", "HOY") else dayLabels[i],
                        color = if (isToday) Amber else MutedGray,
                        fontSize = 11.sp,
                        fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Bold,
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // ── Escudo de racha ───────────────────────────────────────────────────
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(FrostCardBg)
                .border(1.dp, FrostBorder, RoundedCornerShape(16.dp))
                .padding(13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(FrostBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.AcUnit, null, tint = Frost, modifier = Modifier.size(23.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(I18nStore.t("streak.shield.title", "Escudo de racha"), color = Ink, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                Text(
                    I18nStore.t("streak.shield.body", "Tienes {n} · perdona un día sin estudiar").replace("{n}", "$shields"),
                    color = Muted, fontSize = 12.5.sp,
                )
            }
            Text("$shields", color = Frost, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
        }

        Spacer(Modifier.height(12.dp))

        // ── Próximo hito ──────────────────────────────────────────────────────
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                .padding(13.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    I18nStore.t("streak.nextMilestone", "Próximo hito · {n} días").replace("{n}", "$nextMs"),
                    color = Ink, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold,
                )
                Text("$streakDays/$nextMs", color = Amber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(9.dp))
            LinearProgressIndicator(
                progress = { msProgress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(999.dp)),
                color = Amber,
                trackColor = Track,
            )
        }
    }
}
