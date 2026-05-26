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

@Composable
fun ProgressScreen() {
    val vm: ProgressViewModel = viewModel()
    val state by vm.state.collectAsState()

    if (state.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Tu progreso", style = MaterialTheme.typography.headlineMedium)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("🔥", "${state.progress?.streakDays ?: 0}", "Racha", Modifier.weight(1f))
                StatCard("⭐", "${state.progress?.totalXp ?: 0}", "XP", Modifier.weight(1f))
                StatCard("✓", "${state.progress?.completedLessons ?: 0}", "Lecciones", Modifier.weight(1f))
            }
        }
        item {
            Text(
                "Tablero de errores",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                "Lo que más repasar — basado en tus conversaciones.",
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (state.errors.isEmpty()) {
            item {
                Text(
                    "Aún no hay errores registrados. ¡Sigue practicando!",
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
private fun ErrorRow(err: ErrorItem) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            err.errorCategory?.let {
                Text(it.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(4.dp))
            Row {
                Text("❌ ", style = MaterialTheme.typography.bodyMedium)
                Text(err.original ?: "", style = MaterialTheme.typography.bodyMedium)
            }
            Row {
                Text("✅ ", style = MaterialTheme.typography.bodyMedium)
                Text(err.corrected ?: "", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
            err.frequency?.let {
                Spacer(Modifier.height(4.dp))
                Text("Repetido $it ${if (it == 1) "vez" else "veces"}", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
