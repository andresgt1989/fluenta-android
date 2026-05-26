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
import com.alturya.fluenta.util.flag
import com.alturya.fluenta.util.langName
import com.alturya.fluenta.util.levelLabel
import com.alturya.fluenta.util.levelSystemName

@Composable
fun HomeScreen(onSeeMap: () -> Unit = {}, onPronunciation: () -> Unit = {}) {
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
            Text("Hola 👋", style = MaterialTheme.typography.headlineMedium)
            Text(
                "${flag(p?.l2)} Aprendiendo ${langName(p?.l2)} · ${levelLabel(p?.level, p?.levelSystem)} (${levelSystemName(p?.levelSystem)})",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        state.coachMessage?.let { msg ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Row(Modifier.padding(16.dp)) {
                    Text("🎓 ", style = MaterialTheme.typography.titleMedium)
                    Text(
                        msg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MiniStat("🔥", "${state.progress?.streakDays ?: 0}", "Racha", Modifier.weight(1f))
            MiniStat("⭐", "${state.progress?.totalXp ?: 0}", "XP", Modifier.weight(1f))
            MiniStat("✓", "${state.progress?.completedLessons ?: 0}", "Lecciones", Modifier.weight(1f))
        }

        val next = state.nextLesson
        if (next?.title != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Siguiente lección", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(next.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    next.unitTitle?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                val phone = p?.phone ?: ""
                val uri = Uri.parse("https://wa.me/$phone?text=" + Uri.encode("Quiero practicar"))
                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Practicar en WhatsApp") }

        OutlinedButton(
            onClick = onPronunciation,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Practicar pronunciación 🔊") }

        OutlinedButton(
            onClick = onSeeMap,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Ver mi mapa de lecciones") }
    }
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
