package com.alturya.fluenta.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(I18nStore.t("home.greeting", "Hola 👋"), style = MaterialTheme.typography.headlineMedium)
            Text(
                "${flag(p?.l2)} ${I18nStore.t("home.learning", "Aprendiendo")} ${langName(p?.l2)} · ${levelLabel(p?.level, p?.levelSystem)} (${levelSystemName(p?.levelSystem)})",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        state.coachMessage?.let { msg ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row {
                        Text("🎓 ", style = MaterialTheme.typography.titleMedium)
                        Text(
                            msg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                    state.affectiveState?.let { st ->
                        Spacer(Modifier.height(8.dp))
                        Text(affectiveLabel(st), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondary)
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MiniStat("🔥", "${state.progress?.streakDays ?: 0}", I18nStore.t("home.streak", "Racha"), Modifier.weight(1f))
            MiniStat("⭐", "${state.progress?.totalXp ?: 0}", I18nStore.t("home.xp", "XP"), Modifier.weight(1f))
            MiniStat("✓", "${state.progress?.completedLessons ?: 0}", I18nStore.t("home.lessons", "Lecciones"), Modifier.weight(1f))
        }

        val next = state.nextLesson
        if (next?.title != null && next.id != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                onClick = { onStartLesson(next.id) },
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(I18nStore.t("home.nextLesson", "Siguiente lección"), style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(next.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    next.unitTitle?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(I18nStore.t("home.tapToStart", "▶ Toca para empezar"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Primary CTA: start the next lesson in-app (Duolingo-grade), with WhatsApp as the
        // refuerzo/coach button. Lesson plays in-app, then user goes back to WhatsApp to apply.
        if (next?.id != null) {
            Button(
                onClick = { onStartLesson(next.id) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(I18nStore.t("home.startLesson", "▶ Empezar lección")) }
        }

        OutlinedButton(
            onClick = {
                val url = state.practiceWaUrl ?: return@OutlinedButton
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            },
            enabled = state.practiceWaUrl != null,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(I18nStore.t("home.practiceWhatsapp", "💬 Continuar en WhatsApp")) }

        OutlinedButton(
            onClick = onPronunciation,
            modifier = Modifier.fillMaxWidth()
        ) { Text(I18nStore.t("home.practicePronunciation", "Practicar pronunciación 🔊")) }

        OutlinedButton(
            onClick = onPlayMatch,
            modifier = Modifier.fillMaxWidth()
        ) { Text(I18nStore.t("home.matchGame", "Juego: emparejar vocabulario 🎮")) }

        OutlinedButton(
            onClick = onSeeMap,
            modifier = Modifier.fillMaxWidth()
        ) { Text(I18nStore.t("home.lessonMap", "Ver mi mapa de lecciones")) }
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

@Composable
private fun MiniStat(icon: String, value: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}
