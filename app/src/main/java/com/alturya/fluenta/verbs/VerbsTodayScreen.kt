package com.alturya.fluenta.verbs

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.data.Session
import com.alturya.fluenta.network.ApiClient
import com.alturya.fluenta.network.DailyVerbCard
import kotlinx.coroutines.launch

@Composable
fun VerbsTodayScreen(previewState: VerbsTodayState? = null) {
    val vm: VerbsTodayViewModel = viewModel()
    val vmState by vm.state.collectAsState()
    val state = previewState ?: vmState
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (state.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    if (state.error != null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(8.dp))
            Text(state.error!!, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(20.dp))
            Button(onClick = vm::load) { Text(I18nStore.t("common.retry", "Reintentar")) }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text(I18nStore.t("verbs.title", "Verbos del día"), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() })
                Text(
                    I18nStore.t("verbs.intro", "Mínimo 10 verbos diarios con sus variantes. Domina 3 veces y se marca completo."),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        item { TodayProgressCard(state.achievedToday, state.target) }
        items(state.verbs) { verb ->
            VerbCard(
                verb = verb,
                marked = state.markedMastered.contains(verb.id),
                onPractice = { vm.markPracticed(verb.id) },
            )
        }
    }
}

@Composable
private fun TodayProgressCard(mastered: Int, target: Int) {
    val fraction = if (target == 0) 0f else (mastered.toFloat() / target).coerceIn(0f, 1f)
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (mastered >= target) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$mastered / $target", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text(I18nStore.t("verbs.masteredToday", "verbos dominados hoy"), style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth().height(8.dp),
            )
            if (mastered >= target) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(I18nStore.t("verbs.goalReached", "¡Meta de hoy alcanzada!"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun VerbCard(verb: DailyVerbCard, marked: Boolean, onPractice: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val mastered = marked || verb.alreadyMastered

    Card(
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (mastered) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                when {
                    mastered -> Icon(Icons.Default.CheckCircle, contentDescription = I18nStore.t("verbs.mastered", "Dominado"), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    verb.isReview -> Icon(Icons.Default.Refresh, contentDescription = I18nStore.t("verbs.review", "En repaso"), tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(28.dp))
                    else -> Text("•", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(verb.base, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(verb.translation, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) I18nStore.t("common.collapse", "Colapsar") else I18nStore.t("common.expand", "Expandir"),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
            if (expanded) {
                Spacer(Modifier.height(12.dp))
                // Regular vs Irregular badge — English-only: the "-ed" heuristic only
                // works for English; other languages have different conjugation patterns.
                if ((Session.l2 ?: "en") == "en") {
                    val isRegular = verb.variants.pastParticiple.endsWith("ed") ||
                        verb.variants.past.firstOrNull()?.endsWith("ed") == true
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                if (isRegular) I18nStore.t("verbs.regular", "Regular") else I18nStore.t("verbs.irregular", "Irregular"),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                }
                VariantRow(I18nStore.t("tense.present", "Presente"), verb.variants.present.joinToString(" · "))
                VariantRow(I18nStore.t("tense.past", "Pasado"), verb.variants.past.joinToString(" · "))
                VariantRow(I18nStore.t("tense.future", "Futuro"), verb.variants.future.joinToString(" · "))
                VariantRow(I18nStore.t("tense.conditional", "Condicional"), verb.variants.conditional.joinToString(" · "))
                VariantRow("-ing", verb.variants.gerund)
                VariantRow("-ed / participle", verb.variants.pastParticiple)
                Spacer(Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "\"${verb.example}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(12.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                if (!mastered) {
                    com.alturya.fluenta.ui.FluentaButton(
                        text = I18nStore.t("verbs.known", "Ya lo sé ✓"),
                        onClick = onPractice,
                        style = com.alturya.fluenta.ui.FluentaButtonStyle.Success,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun VariantRow(label: String, value: String) {
    Row(Modifier.padding(vertical = 2.dp)) {
        Text("$label: ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}
