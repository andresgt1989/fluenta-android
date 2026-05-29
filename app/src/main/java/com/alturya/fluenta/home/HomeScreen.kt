package com.alturya.fluenta.home

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.util.flag
import com.alturya.fluenta.util.langName
import com.alturya.fluenta.util.levelLabel
import com.alturya.fluenta.util.levelSystemName

@Composable
fun HomeScreen(
    onSeeMap: () -> Unit = {},
    onPronunciation: () -> Unit = {},
    onPlayMatch: () -> Unit = {},
    onStartLesson: (String) -> Unit = {},
    onChangeLanguage: () -> Unit = {},
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
            Text(
                I18nStore.t("home.greeting", "Hola 👋"),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
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

        // ── Streak hero + stats ───────────────────────────────────────────────
        StatsHero(
            streak = state.progress?.streakDays ?: 0,
            xp = state.progress?.totalXp ?: 0,
            lessons = state.progress?.completedLessons ?: 0,
        )

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
                onClick = { onStartLesson(next.id) },
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
        }

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

private fun affectiveLabel(state: String): String = when (state) {
    "new" -> I18nStore.t("affective.new", "Empezando ✨")
    "motivated" -> I18nStore.t("affective.motivated", "En racha 🔥")
    "returning" -> I18nStore.t("affective.returning", "De regreso 👋")
    "at_risk" -> I18nStore.t("affective.atRisk", "Te extrañamos — vamos suave hoy")
    "steady" -> I18nStore.t("affective.steady", "Constante 🌱")
    else -> ""
}
