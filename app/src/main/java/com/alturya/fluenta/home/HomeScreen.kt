package com.alturya.fluenta.home

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.progress.LeagueViewModel
import com.alturya.fluenta.util.flag
import com.alturya.fluenta.util.langName
import com.alturya.fluenta.util.levelIndex
import com.alturya.fluenta.util.levelLabel
import com.alturya.fluenta.util.levelLadderShort
import com.alturya.fluenta.util.levelSystemName

@Composable
fun HomeScreen(
    onSeeMap: () -> Unit = {},
    onPronunciation: () -> Unit = {},
    onPlayMatch: () -> Unit = {},
    onStartLesson: (String) -> Unit = {},
    onChangeLanguage: () -> Unit = {},
    onRepaso: () -> Unit = {},
    onLevelTest: () -> Unit = {},
    onConversation: () -> Unit = {},
    onConversationLesson: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val vm: HomeViewModel = viewModel()
    val state by vm.state.collectAsState()
    val p = state.profile

    if (state.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Header: saludo + chip de idioma (tap para cambiar) ─────────────────
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(com.alturya.fluenta.R.drawable.ic_fluenta_hola),
                    contentDescription = null,
                    modifier = Modifier.size(52.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    I18nStore.t("home.greeting", "Hola 👋"),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(8.dp))
            Surface(
                onClick = onChangeLanguage,
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${flag(p?.l2)} ${langName(p?.l2)} · ${levelLabel(p?.level, p?.levelSystem)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "⇄ ${I18nStore.t("home.changeLanguage", "cambiar")}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        // ── Viaje de nivel (CEFR/HSK/JLPT) — dónde estás y a dónde vas ─────────
        LevelJourneyCard(level = p?.level, levelSystem = p?.levelSystem, onLevelTest = onLevelTest)

        // ── Streak hero + stats ───────────────────────────────────────────────
        StatsHero(
            streak = state.progress?.streakDays ?: 0,
            xp = state.progress?.totalXp ?: 0,
            lessons = state.progress?.completedLessons ?: 0,
        )

        // ── Meta diaria (daily goal ring) ─────────────────────────────────────
        val prog = state.progress
        if (prog != null && prog.dailyGoalXp > 0) {
            DailyGoalBar(
                todayXp = prog.todayXp,
                goalXp = prog.dailyGoalXp,
                pct = prog.dailyGoalPct,
            )
        }

        // ── Coach message ─────────────────────────────────────────────────────
        state.coachMessage?.let { msg ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "🎓 $msg",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    state.affectiveState?.let { st ->
                        Spacer(Modifier.height(6.dp))
                        Text(
                            affectiveLabel(st),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
        }

        // ── Dominant CTA: next lesson ─────────────────────────────────────────
        val next = state.nextLesson
        if (next?.title != null && next.id != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
                onClick = {
                    if (next.lessonType == "roleplay" || next.lessonType == "free_chat")
                        onConversationLesson(next.id)
                    else onStartLesson(next.id)
                },
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        I18nStore.t("home.todayTask", "Tu tarea de hoy"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        next.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    next.unitTitle?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.onPrimary,
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Text(
                            I18nStore.t("home.startLesson", "▶ Empezar lección"),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        )
                    }
                }
            }
        } else {
            // No assigned next lesson yet (fresh account / unseeded pair): never leave
            // the user without a primary action — send them into their plan.
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                onClick = onSeeMap,
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        I18nStore.t("home.startHereTitle", "Empieza tu primera lección"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        I18nStore.t("home.startHereSubtitle", "Abre tu plan y da el primer paso de hoy."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                    )
                }
            }
        }

        // ── Repaso (SRS) — acción diaria primaria de retención ────────────────
        val dueCount = state.progress?.cardsDueToday ?: 0
        Card(
            onClick = onRepaso,
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (dueCount > 0) MaterialTheme.colorScheme.tertiaryContainer
                else MaterialTheme.colorScheme.secondaryContainer
            ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("🔁", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (dueCount > 0)
                            I18nStore.t("home.reviewDue", "Repasar ($dueCount vencen hoy)")
                                .replace("\$dueCount", "$dueCount")
                        else I18nStore.t("home.reviewTitle", "Repasar tus errores"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        if (dueCount > 0) I18nStore.t("home.reviewUrgent", "¡No dejes que se olviden! Toca aquí")
                        else I18nStore.t("home.reviewSubtitle", "Refuerza lo que se te olvida (SRS)"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                Text("›", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }

        // ── Liga semanal teaser — visible desde Home para enganchar competencia ─
        LeagueTeaserCard()

        // ── Secundario: COLAPSADO por defecto (coach enfocado, no menú) ────────
        var showMore by remember { mutableStateOf(false) }
        Surface(
            onClick = { showMore = !showMore },
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    "${I18nStore.t("home.morePractice", "Más práctica")} ${if (showMore) "▴" else "▾"}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Button(
            onClick = onConversation,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("🗣️  " + I18nStore.t("home.conversation", "Conversar en inglés")) }

        if (showMore) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionCard("🔊", I18nStore.t("home.pronunciationShort", "Pronunciación"), onClick = onPronunciation)
                ActionCard("🎮", I18nStore.t("home.gameShort", "Juego"), onClick = onPlayMatch)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionCard("🗺", I18nStore.t("home.mapShort", "Mi mapa"), onClick = onSeeMap)
                ActionCard(
                    "💬",
                    I18nStore.t("home.whatsappShort", "WhatsApp"),
                    enabled = state.practiceWaUrl != null,
                    onClick = {
                        state.practiceWaUrl?.let { url ->
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun StatsHero(streak: Int, xp: Int, lessons: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Animated flame for the streak — gentle pulse.
            val transition = rememberInfiniteTransition(label = "flame")
            val flameScale by transition.animateFloat(
                initialValue = 1f,
                targetValue = 1.18f,
                animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
                label = "flameScale",
            )
            HeroStat(
                icon = "🔥",
                value = "$streak",
                label = I18nStore.t("home.streak", "Racha"),
                iconScale = if (streak > 0) flameScale else 1f,
            )
            HeroStat("⭐", "$xp", I18nStore.t("home.xp", "XP"))
            HeroStat("✓", "$lessons", I18nStore.t("home.lessons", "Lecciones"))
        }
    }
}

@Composable
private fun HeroStat(icon: String, value: String, label: String, iconScale: Float = 1f) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.scale(iconScale))
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
        )
    }
}

@Composable
private fun RowScope.ActionCard(
    icon: String,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.weight(1f).height(92.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(icon, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DailyGoalBar(todayXp: Int, goalXp: Int, pct: Int) {
    val fraction = (pct / 100f).coerceIn(0f, 1f)
    val done = pct >= 100
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (done) MaterialTheme.colorScheme.tertiaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        ),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (done) "🎯 ${I18nStore.t("home.goalDone", "¡Meta de hoy completada!")}"
                    else "🎯 ${I18nStore.t("home.dailyGoal", "Meta de hoy")}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "$todayXp / $goalXp XP",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth().height(10.dp),
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            )
        }
    }
}

@Composable
private fun LeagueTeaserCard() {
    val vm: LeagueViewModel = viewModel()
    val state by vm.state.collectAsState()
    val league = state.league ?: return

    val tierEmoji = when (league.tier) {
        "bronze" -> "🥉"; "silver" -> "🥈"; "gold" -> "🥇"; "diamond" -> "💎"; else -> "🏅"
    }
    val tierLabel = when (league.tier) {
        "bronze" -> I18nStore.t("league.tierBronze", "Liga Bronce")
        "silver" -> I18nStore.t("league.tierSilver", "Liga Plata")
        "gold" -> I18nStore.t("league.tierGold", "Liga Oro")
        "diamond" -> I18nStore.t("league.tierDiamond", "Liga Diamante")
        else -> I18nStore.t("league.default", "Liga")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(tierEmoji, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(tierLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                league.myRank?.let {
                    Text(
                        "#$it · ${league.myWeeklyXp} XP ${I18nStore.t("league.thisWeek", "esta semana")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            val topN = league.promotionCutoff
            Text(
                I18nStore.t("league.topNPromo", "Top $topN sube").replace("$topN", "$topN"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun affectiveLabel(state: String): String = when (state) {
    "new" -> I18nStore.t("affective.new", "Empezando ✨")
    "motivated" -> I18nStore.t("affective.motivated", "En racha 🔥")
    "returning" -> I18nStore.t("affective.returning", "De regreso 👋")
    "at_risk" -> I18nStore.t("affective.atRisk", "Te extrañamos — vamos suave hoy")
    "steady" -> I18nStore.t("affective.steady", "Constante 🌱")
    else -> ""
}

// ── Viaje de nivel (ficha §7-BIS): "estás en B1 → meta C1" ────────────────────
// Sin nivel (test omitido) → nudge para hacer el test. Con nivel → escalera del
// marco del par (CEFR/JLPT/HSK/TOPIK) con la banda actual resaltada.
@Composable
private fun LevelJourneyCard(level: String?, levelSystem: String?, onLevelTest: () -> Unit) {
    val idx = levelIndex(level)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp)) {
            if (idx < 0) {
                Text(
                    "🎯 ${I18nStore.t("home.discoverLevel", "Descubre tu nivel")}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    I18nStore.t("home.discoverLevelHint", "Haz un test corto y ajustamos las lecciones a tu nivel real."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = onLevelTest, modifier = Modifier.fillMaxWidth()) {
                    Text(I18nStore.t("home.takeLevelTest", "Hacer el test de nivel"))
                }
            } else {
                val ladder = levelLadderShort(levelSystem)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "🧭 ${I18nStore.t("home.yourJourney", "Tu camino")}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${I18nStore.t("home.goal", "meta")} ${levelLabel("c1", levelSystem)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ladder.forEachIndexed { i, lbl ->
                        val current = i == idx
                        val done = i < idx
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = when {
                                current -> MaterialTheme.colorScheme.primary
                                done -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surface
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                lbl,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                color = when {
                                    current -> MaterialTheme.colorScheme.onPrimary
                                    done -> MaterialTheme.colorScheme.onPrimaryContainer
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "${I18nStore.t("home.youAreHere", "Estás en")} ${levelLabel(level, levelSystem)}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
