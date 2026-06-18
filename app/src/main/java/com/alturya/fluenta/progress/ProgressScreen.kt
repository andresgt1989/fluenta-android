package com.alturya.fluenta.progress

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.ui.ShimmerCard
import kotlinx.coroutines.launch
import com.alturya.fluenta.network.ErrorItem
import com.alturya.fluenta.util.levelLabel
import com.alturya.fluenta.util.levelSystemName

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
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ShimmerCard(height = 80.dp)
            ShimmerCard(height = 200.dp)
            ShimmerCard(height = 96.dp)
        }
        return
    }

    val p = state.profile
    Box(Modifier.fillMaxSize().nestedScroll(pullState.nestedScrollConnection)) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text(I18nStore.t("progress.title", "Tu progreso"), style = MaterialTheme.typography.headlineMedium) }

        // Weekly league leaderboard (retention mechanic) — backend ranks Sundays.
        item { LeagueCard() }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text(I18nStore.t("progress.currentLevel", "Nivel actual"), style = MaterialTheme.typography.labelMedium)
                    Text(
                        "${levelLabel(p?.level, p?.levelSystem)} · ${levelSystemName(p?.levelSystem)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Icons.Default.LocalFireDepartment, "${state.progress?.streakDays ?: 0}", I18nStore.t("home.streak", "Racha"), Modifier.weight(1f))
                StatCard(Icons.Default.Star, "${state.progress?.totalXp ?: 0}", I18nStore.t("home.xp", "XP"), Modifier.weight(1f))
                StatCard(Icons.AutoMirrored.Filled.MenuBook, "${state.progress?.completedLessons ?: 0}", I18nStore.t("home.lessons", "Lecciones"), Modifier.weight(1f))
            }
        }

        // SRS mastery summary — shows how many errors the user has consolidated
        val masteredCount = state.errors.count { it.masteredAt != null }
        val totalErrors = state.errors.size
        if (totalErrors > 0) {
            item {
                Surface(
                    color = if (masteredCount > 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = if (masteredCount > 0) Color(0xFFEAB308) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "$masteredCount/$totalErrors ${I18nStore.t("progress.erroresMastered", "errores dominados")}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(Modifier.weight(1f))
                        LinearProgressIndicator(
                            progress = { masteredCount.toFloat() / totalErrors },
                            modifier = Modifier.width(72.dp).height(6.dp),
                            color = Color(0xFFEAB308),
                            trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                        )
                    }
                }
            }
        }

        // Error pattern analysis — shows AI personalisation visibly to the user
        val unmasteredErrors = state.errors.filter { it.masteredAt == null }
        val totalReviews = state.errors.sumOf { it.reviewCount ?: 0 }
        if (unmasteredErrors.size >= 3) {
            item { ErrorPatternCard(unmasteredErrors) }
        }
        if (totalReviews > 0) {
            item {
                Text(
                    I18nStore.t("progress.totalReviews", "Repasos completados: $totalReviews").replace("$totalReviews", "$totalReviews"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        // Subskills radar
        item {
            Text(I18nStore.t("progress.skillsTitle", "Tus habilidades"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
        }
        val hasSkillData = state.skills.any { (it.total) > 0 } || state.taskSuccessRate > 0
        if (hasSkillData) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column {
                        SkillsRadar(state.skills)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            if (state.wpm > 0) {
                                MiniMetric("${state.wpm}", I18nStore.t("progress.wpm", "palabras/min"))
                            }
                            MiniMetric("${state.taskSuccessRate}%", I18nStore.t("progress.successRate", "éxito tareas"))
                        }
                    }
                }
            }
        } else {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Text(
                        I18nStore.t("progress.radarEmpty", "Tu radar de habilidades aparecerá aquí cuando completes lecciones. Mide gramática, vocabulario, pronunciación y fluidez."),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        }

        // SRS review board
        item {
            Text(I18nStore.t("progress.srsTitle", "Repaso (SRS)"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))
            Text(
                I18nStore.t("progress.srsSubtitle", "Errores a consolidar — repaso espaciado."),
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (state.errors.isEmpty()) {
            item {
                Text(
                    I18nStore.t("progress.errorsEmpty", "Aún no hay errores registrados. ¡Sigue practicando en WhatsApp!"),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        } else {
            items(state.errors) { err -> ErrorRow(err) }
        }
    }  // LazyColumn
    PullToRefreshContainer(state = pullState, modifier = Modifier.align(Alignment.TopCenter))
    }  // Box
}

@Composable
private fun StatCard(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun MiniMetric(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ErrorRow(err: ErrorItem) {
    val mastered = err.masteredAt != null
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                err.errorType?.let {
                    AssistChip(onClick = {}, label = { Text(skillLabel(it)) })
                    Spacer(Modifier.width(8.dp))
                }
                Spacer(Modifier.weight(1f))
                if (mastered) {
                    Text(I18nStore.t("progress.mastered", "Dominado ✓"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                } else {
                    Text(I18nStore.t("progress.toReview", "Por repasar"), style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Close, contentDescription = I18nStore.t("progress.wrongAnswer", "Tu respuesta"), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(err.original ?: "", style = MaterialTheme.typography.bodyMedium)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = I18nStore.t("progress.correctAnswer", "Forma correcta"), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(err.corrected ?: "", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
            val reviews = err.reviewCount ?: 0
            if (reviews > 0) {
                Spacer(Modifier.height(4.dp))
                Text("Repasado $reviews ${if (reviews == 1) "vez" else "veces"}", style = MaterialTheme.typography.labelSmall)
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
                        style = MaterialTheme.typography.labelSmall,
                        color = if (daysLeft == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
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
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    I18nStore.t("progress.patterns", "Tus patrones de error"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(10.dp))
            byType.entries
                .sortedByDescending { it.value.size }
                .take(3)
                .forEach { (type, errs) ->
                    val pct = (errs.size * 100) / total
                    val barColor = when (type) {
                        "grammar" -> MaterialTheme.colorScheme.error
                        "vocab" -> MaterialTheme.colorScheme.tertiary
                        "pronunciation" -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.primary
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp),
                    ) {
                        Text(
                            skillLabel(type),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(96.dp),
                        )
                        LinearProgressIndicator(
                            progress = { pct / 100f },
                            modifier = Modifier.weight(1f).height(6.dp),
                            color = barColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "$pct%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(32.dp),
                            color = barColor,
                        )
                    }
                }
            Spacer(Modifier.height(4.dp))
            Text(
                I18nStore.t("progress.patternsHint", "Enfócate en el área con más errores para progresar más rápido"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
