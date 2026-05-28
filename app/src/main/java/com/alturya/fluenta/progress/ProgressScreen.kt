package com.alturya.fluenta.progress

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alturya.fluenta.network.ErrorItem
import com.alturya.fluenta.util.levelLabel
import com.alturya.fluenta.util.levelSystemName

@Composable
fun ProgressScreen() {
    val vm: ProgressViewModel = viewModel()
    val state by vm.state.collectAsState()

    if (state.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val p = state.profile
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Tu progreso", style = MaterialTheme.typography.headlineMedium) }

        // Weekly league leaderboard (retention mechanic) — backend ranks Sundays.
        item { LeagueCard() }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("Nivel actual", style = MaterialTheme.typography.labelMedium)
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
                StatCard("🔥", "${state.progress?.streakDays ?: 0}", "Racha", Modifier.weight(1f))
                StatCard("⭐", "${state.progress?.totalXp ?: 0}", "XP", Modifier.weight(1f))
                StatCard("✓", "${state.progress?.completedLessons ?: 0}", "Lecciones", Modifier.weight(1f))
            }
        }

        // Subskills radar
        item {
            Text("Tus habilidades", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
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
                            MiniMetric("${state.wpm}", "palabras/min")
                            MiniMetric("${state.taskSuccessRate}%", "éxito tareas")
                        }
                    }
                }
            }
        } else {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Text(
                        "Tu radar de habilidades aparecerá aquí cuando completes lecciones. " +
                            "Mide gramática, vocabulario, pronunciación y fluidez.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        }

        // SRS review board
        item {
            Text("Repaso (SRS)", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))
            Text(
                "Errores a consolidar — repaso espaciado.",
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (state.errors.isEmpty()) {
            item {
                Text(
                    "Aún no hay errores registrados. ¡Sigue practicando en WhatsApp!",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        } else {
            items(state.errors) { err -> ErrorRow(err) }
        }
    }
}

@Composable
private fun StatCard(icon: String, value: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, style = MaterialTheme.typography.titleLarge)
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
                    Text("Dominado ✓", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                } else {
                    Text("Por repasar", style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row { Text("❌ "); Text(err.original ?: "", style = MaterialTheme.typography.bodyMedium) }
            Row { Text("✅ "); Text(err.corrected ?: "", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium) }
            val reviews = err.reviewCount ?: 0
            if (reviews > 0) {
                Spacer(Modifier.height(4.dp))
                Text("Repasado $reviews ${if (reviews == 1) "vez" else "veces"}", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private fun skillLabel(type: String): String = when (type) {
    "grammar" -> "Gramática"
    "vocab" -> "Vocabulario"
    "pronunciation" -> "Pronunciación"
    "fluency" -> "Fluidez"
    else -> type
}
