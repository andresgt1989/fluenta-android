package com.alturya.fluenta.curriculum

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alturya.fluenta.network.CurriculumUnit
import com.alturya.fluenta.network.Lesson

@Composable
fun CurriculumMapScreen() {
    val vm: CurriculumViewModel = viewModel()
    val state by vm.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Mapa de aprendizaje",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(20.dp)
        )

        when {
            state.loading -> Box(
                Modifier.fillMaxSize(), contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            state.units.isEmpty() -> EmptyMap(state.l1, state.l2)

            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.units.forEach { unit ->
                    item { UnitHeader(unit) }
                    items(unit.lessons) { lesson -> LessonNode(lesson) }
                }
            }
        }
    }
}

@Composable
private fun EmptyMap(l1: String?, l2: String?) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Aún no hay lecciones para ${(l1 ?: "?").uppercase()} → ${(l2 ?: "?").uppercase()}",
            style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Practica en WhatsApp para empezar — tu mapa se irá llenando.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun UnitHeader(unit: CurriculumUnit) {
    Column(Modifier.padding(top = 16.dp, bottom = 4.dp)) {
        Text(
            "Unidad ${unit.number} · ${unit.title}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        unit.description?.let {
            Text(it, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun LessonNode(lesson: Lesson) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (lesson.completed)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (lesson.completed) "✓" else lessonIcon(lesson.type),
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(lesson.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Text(lessonTypeLabel(lesson.type), style = MaterialTheme.typography.bodySmall)
            }
            if (lesson.completed) {
                Text("Completada", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32))
            }
        }
    }
}

private fun lessonIcon(type: String): String = when (type) {
    "roleplay" -> "🎭"
    "translation" -> "🔤"
    "free_chat" -> "💬"
    "listening" -> "👂"
    "pronunciation" -> "🗣"
    else -> "★"
}

private fun lessonTypeLabel(type: String): String = when (type) {
    "roleplay" -> "Juego de roles"
    "translation" -> "Traducción"
    "free_chat" -> "Conversación libre"
    "listening" -> "Comprensión auditiva"
    "pronunciation" -> "Pronunciación"
    else -> type
}
