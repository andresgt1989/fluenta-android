package com.alturya.fluenta.curriculum

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.network.CurriculumUnit
import com.alturya.fluenta.network.Lesson

// Zigzag column offsets (as fraction of available width, centred on 0.5)
private val ZIGZAG = listOf(0.15f, 0.35f, 0.5f, 0.65f, 0.85f, 0.65f, 0.5f, 0.35f, 0.15f, 0.35f)

@Composable
fun CurriculumMapScreen(
    onStartLesson: (String) -> Unit = {},
    onConversationLesson: (String) -> Unit = {},
    previewState: CurriculumState? = null,
) {
    val vm: CurriculumViewModel = viewModel()
    val vmState by vm.state.collectAsState()
    val state = previewState ?: vmState
    val listState = rememberLazyListState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                Text(
                    I18nStore.t("curriculum.title", "Tu camino de aprendizaje"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                if (state.l1 != null && state.l2 != null) {
                    Text(
                        "${state.l1?.uppercase()} → ${state.l2?.uppercase()}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                    )
                }
            }
        }

        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("😕", style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(8.dp))
                    Text(state.error!!, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { vm.load() }) { Text(I18nStore.t("common.retry", "Reintentar")) }
                }
            }
            state.units.isEmpty() -> EmptyMap(state.l1, state.l2)
            else -> {
                // Flatten all lessons with their unit info, then render as zigzag path
                val allLessons = state.units.flatMap { unit ->
                    buildList {
                        add(UnitMarker(unit))
                        unit.lessons.forEach { add(LessonEntry(it, unit)) }
                    }
                }

                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(top = 20.dp, bottom = 80.dp),
                ) {
                    allLessons.forEachIndexed { globalIdx, item ->
                        item {
                            when (item) {
                                is UnitMarker -> UnitSectionHeader(item.unit)
                                is LessonEntry -> {
                                    val lessonIdx = allLessons.take(globalIdx + 1)
                                        .filterIsInstance<LessonEntry>().size - 1
                                    val xFraction = ZIGZAG[lessonIdx % ZIGZAG.size]
                                    PathLessonNode(
                                        lesson = item.lesson,
                                        xFraction = xFraction,
                                        onClick = {
                                            // Una lección de roleplay/free_chat ES conversación,
                                            // no un quiz: enrutamos al motor de conversación real.
                                            if (item.lesson.type == "roleplay" || item.lesson.type == "free_chat")
                                                onConversationLesson(item.lesson.id)
                                            else onStartLesson(item.lesson.id)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private sealed class MapItem
private data class UnitMarker(val unit: CurriculumUnit) : MapItem()
private data class LessonEntry(val lesson: Lesson, val unit: CurriculumUnit) : MapItem()

@Composable
private fun UnitSectionHeader(unit: CurriculumUnit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.tertiary,
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Text(
                "${I18nStore.t("curriculum.unit", "Unidad")} ${unit.number} · ${unit.title}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                textAlign = TextAlign.Center,
            )
        }
        unit.description?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PathLessonNode(lesson: Lesson, xFraction: Float, onClick: () -> Unit) {
    val isCompleted = lesson.completed
    // First incomplete lesson is the "active" one (slight pulse animation)
    val transition = rememberInfiniteTransition(label = "pulse_$xFraction")
    val pulse by transition.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "scale",
    )

    val nodeColor = when {
        isCompleted -> Color(0xFF1BB6A6)  // teal — completed
        else -> MaterialTheme.colorScheme.primary  // active/available
    }
    val iconText = when {
        isCompleted -> "✓"
        else -> lessonIcon(lesson.type)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp),
    ) {
        val maxW = maxWidth
        val offsetX = (maxW * xFraction) - 36.dp  // centre 72dp circle on xFraction

        // Connecting path line (background, drawn behind node)
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(96.dp)
                .align(Alignment.Center)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )

        // Lesson node circle
        Column(
            modifier = Modifier
                .offset(x = offsetX.coerceIn(4.dp, maxW - 76.dp))
                .align(Alignment.CenterStart)
                .scale(if (!isCompleted) pulse else 1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(nodeColor)
                    .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                Text(iconText, fontSize = 26.sp, color = Color.White)
            }
            Spacer(Modifier.height(2.dp))
            Text(
                lesson.title,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 2,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(88.dp),
            )
        }
    }
}

@Composable
private fun EmptyMap(l1: String?, l2: String?) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🗺️", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(12.dp))
        Text(I18nStore.t("curriculum.emptyTitle", "Tu mapa se irá llenando"),
            style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text("${(l1 ?: "?").uppercase()} → ${(l2 ?: "?").uppercase()}",
            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text(
            I18nStore.t("curriculum.emptyHint", "Completa tu primera lección para ver el camino completo."),
            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
        )
    }
}

private fun lessonIcon(type: String): String = when (type) {
    "roleplay" -> "🎭"
    "translation" -> "🔤"
    "free_chat" -> "💬"
    "listening" -> "👂"
    "pronunciation" -> "🗣"
    else -> "⭐"
}

private fun lessonTypeLabel(type: String): String = when (type) {
    "roleplay" -> I18nStore.t("lessonType.roleplay", "Juego de roles")
    "translation" -> I18nStore.t("lessonType.translation", "Traducción")
    "free_chat" -> I18nStore.t("lessonType.free_chat", "Conversación libre")
    "listening" -> I18nStore.t("lessonType.listening", "Comprensión auditiva")
    "pronunciation" -> I18nStore.t("lessonType.pronunciation", "Pronunciación")
    else -> type
}
