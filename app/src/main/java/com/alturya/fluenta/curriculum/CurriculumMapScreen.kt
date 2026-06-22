package com.alturya.fluenta.curriculum

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.network.CurriculumUnit
import com.alturya.fluenta.network.Lesson

/* Paleta del kit Claude Design (Fluenta Mapa de Lecciones.dc.html) */
private object Cz {
    val BgTop = Color(0xFFF2FBF8)
    val BgMid = Color(0xFFE4F6F1)
    val BgBot = Color(0xFFCDEEE6)
    val Teal = Color(0xFF0E9D8E)
    val TealDark = Color(0xFF0A6F64)
    val Teal2 = Color(0xFF13B0A0)
    val Muted = Color(0xFF7C857F)
    val Sub = Color(0xFF5E726C)
    val NodeLocked = Color(0xFFE3ECE8)
    val LockBorder = Color(0xFFD2DED9)
    val LockInk = Color(0xFF9AA9A2)
    val ConnReached = Color(0xFF13B0A0)
    val ConnLocked = Color(0xFFCFDDD7)
    val Track = Color(0xFFD7EBE5)
    val Amber = Color(0xFFF6A623)
}

private val ZIGZAG = listOf(0.5f, 0.74f, 0.84f, 0.66f, 0.36f, 0.16f, 0.26f, 0.5f, 0.74f, 0.66f)

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
    val reloadSignal by com.alturya.fluenta.data.Session.reloadSignal.collectAsState()
    LaunchedEffect(reloadSignal) {
        if (previewState == null && reloadSignal > 0) vm.load()
    }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Cz.BgTop, 0.55f to Cz.BgMid, 1f to Cz.BgBot))) {
        Column(Modifier.fillMaxSize()) {
            // App bar
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(top = 16.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    I18nStore.t("curriculum.title", "Mi mapa de lecciones"),
                    fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Cz.TealDark,
                    modifier = Modifier.weight(1f),
                )
                if (state.l1 != null && state.l2 != null) {
                    Surface(shape = RoundedCornerShape(999.dp), color = Color.White, shadowElevation = 2.dp) {
                        Text(
                            "${state.l1?.uppercase()} → ${state.l2?.uppercase()}",
                            fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Cz.TealDark,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }

            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Cz.Teal, trackColor = Cz.BgBot)
                }
                state.error != null -> ErrorMap(state.error!!, onRetry = { vm.load() })
                state.units.isEmpty() -> EmptyMap(state.l1, state.l2)
                else -> {
                    val items = state.units.flatMap { unit ->
                        buildList {
                            add(UnitMarker(unit))
                            unit.lessons.forEach { add(LessonEntry(it, unit)) }
                        }
                    }
                    val activeLessonId = state.units.flatMap { it.lessons }.firstOrNull { !it.completed }?.id

                    LazyColumn(state = listState, contentPadding = PaddingValues(top = 8.dp, bottom = 90.dp)) {
                        items.forEachIndexed { globalIdx, item ->
                            item {
                                when (item) {
                                    is UnitMarker -> UnitHeaderCard(item.unit)
                                    is LessonEntry -> {
                                        val lessonIdx = items.take(globalIdx + 1).filterIsInstance<LessonEntry>().size - 1
                                        val xFraction = ZIGZAG[lessonIdx % ZIGZAG.size]
                                        val st = when {
                                            item.lesson.completed -> NodeState.DONE
                                            item.lesson.id == activeLessonId -> NodeState.CURRENT
                                            else -> NodeState.LOCKED
                                        }
                                        PathLessonNode(
                                            lesson = item.lesson,
                                            xFraction = xFraction,
                                            nodeState = st,
                                            onClick = {
                                                if (st == NodeState.LOCKED) return@PathLessonNode
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
}

private enum class NodeState { DONE, CURRENT, LOCKED }

private sealed class MapItem
private data class UnitMarker(val unit: CurriculumUnit) : MapItem()
private data class LessonEntry(val lesson: Lesson, val unit: CurriculumUnit) : MapItem()

@Composable
private fun UnitHeaderCard(unit: CurriculumUnit) {
    val done = unit.lessons.count { it.completed }
    val total = unit.lessons.size
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        shadowElevation = 4.dp,
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${I18nStore.t("curriculum.unit", "Unidad")} ${unit.number}".uppercase(),
                    fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, color = Cz.Teal,
                )
                Text(unit.title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Cz.TealDark, lineHeight = 22.sp, modifier = Modifier.padding(top = 2.dp))
                unit.description?.takeIf { it.isNotBlank() }?.let {
                    Text(it, fontSize = 12.sp, color = Cz.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp))
                }
            }
            if (total > 0) {
                Spacer(Modifier.width(12.dp))
                UnitProgressRing(done, total)
            }
        }
    }
}

@Composable
private fun UnitProgressRing(done: Int, total: Int) {
    val pct = if (total == 0) 0f else done.toFloat() / total
    Box(Modifier.size(50.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(50.dp)) {
            val stroke = 6.dp.toPx()
            drawArc(Cz.Track, -90f, 360f, false, style = Stroke(stroke, cap = StrokeCap.Round),
                size = Size(size.width - stroke, size.height - stroke),
                topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2))
            drawArc(Cz.Teal, -90f, 360f * pct, false, style = Stroke(stroke, cap = StrokeCap.Round),
                size = Size(size.width - stroke, size.height - stroke),
                topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2))
        }
        Text("$done/$total", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Cz.TealDark)
    }
}

@Composable
private fun PathLessonNode(lesson: Lesson, xFraction: Float, nodeState: NodeState, onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulse by transition.animateFloat(
        initialValue = 1f, targetValue = 1.10f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "scale",
    )
    val reached = nodeState != NodeState.LOCKED
    val rowHeight = if (nodeState == NodeState.CURRENT) 132.dp else 104.dp

    BoxWithConstraints(Modifier.fillMaxWidth().height(rowHeight)) {
        val maxW = maxWidth
        val nodeSize = if (nodeState == NodeState.CURRENT) 76.dp else 64.dp
        val offsetX = (maxW * xFraction) - nodeSize / 2

        // Conector vertical (detrás del nodo)
        Box(
            Modifier.width(6.dp).fillMaxHeight().align(Alignment.TopCenter)
                .padding(top = 0.dp)
                .background(if (reached) Cz.ConnReached else Cz.ConnLocked),
        )

        Column(
            Modifier.offset(x = offsetX.coerceIn(8.dp, maxW - nodeSize - 8.dp)).align(Alignment.CenterStart),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Burbuja "Empezar ▸" sobre el nodo actual
            if (nodeState == NodeState.CURRENT) {
                Surface(shape = RoundedCornerShape(14.dp), color = Color.White, shadowElevation = 8.dp) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(cleanTitle(lesson.title), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Cz.TealDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${I18nStore.t("common.start", "Empezar")} ▸", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Cz.Teal, modifier = Modifier.padding(top = 2.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Nodo 3D
            Box(
                Modifier.size(nodeSize + 5.dp).scale(if (nodeState == NodeState.CURRENT) pulse else 1f),
                contentAlignment = Alignment.TopCenter,
            ) {
                // sombra 3D
                if (nodeState != NodeState.LOCKED) {
                    Box(Modifier.size(nodeSize).align(Alignment.BottomCenter).clip(CircleShape).background(Cz.TealDark))
                }
                val face = when (nodeState) {
                    NodeState.DONE -> Cz.Teal
                    NodeState.CURRENT -> Cz.Teal2
                    NodeState.LOCKED -> Cz.NodeLocked
                }
                Box(
                    Modifier.size(nodeSize).align(Alignment.TopCenter).clip(CircleShape).background(face)
                        .then(if (nodeState == NodeState.CURRENT) Modifier.border(3.dp, Color.White, CircleShape)
                              else if (nodeState == NodeState.LOCKED) Modifier.border(2.dp, Cz.LockBorder, CircleShape)
                              else Modifier)
                        .clickable(enabled = nodeState != NodeState.LOCKED, onClick = onClick),
                    contentAlignment = Alignment.Center,
                ) {
                    when (nodeState) {
                        NodeState.DONE -> Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                        NodeState.CURRENT -> Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                        NodeState.LOCKED -> Icon(Icons.Default.Lock, contentDescription = null, tint = Cz.LockInk, modifier = Modifier.size(22.dp))
                    }
                }
            }

            // Caption (título) para nodos completados
            if (nodeState == NodeState.DONE) {
                Text(cleanTitle(lesson.title), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Cz.TealDark,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp).width(110.dp))
            }
        }
    }
}

private fun cleanTitle(t: String) = t.removePrefix("Conversación: ").removePrefix("Conversation: ").trim()

@Composable
private fun EmptyMap(l1: String?, l2: String?) {
    Column(Modifier.fillMaxSize().padding(36.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🦉", fontSize = 96.sp)
        Spacer(Modifier.height(20.dp))
        Text(I18nStore.t("curriculum.emptyTitle", "Tu mapa está vacío"), fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, color = Cz.TealDark, textAlign = TextAlign.Center)
        Text(I18nStore.t("curriculum.emptyHint", "Elige un idioma para empezar tu camino de aprendizaje."), fontSize = 15.sp, color = Cz.Sub, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp).widthIn(max = 280.dp))
        if (l1 != null && l2 != null) {
            Spacer(Modifier.height(6.dp))
            Text("${l1.uppercase()} → ${l2.uppercase()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Cz.Teal)
        }
    }
}

@Composable
private fun ErrorMap(msg: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(36.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🦉", fontSize = 84.sp)
        Spacer(Modifier.height(18.dp))
        Text(I18nStore.t("curriculum.errTitle", "No pudimos cargar tu mapa"), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Cz.TealDark, textAlign = TextAlign.Center)
        Text(msg, fontSize = 15.sp, color = Cz.Sub, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(24.dp))
        Box(Modifier.fillMaxWidth().height(56.dp)) {
            Box(Modifier.fillMaxWidth().height(52.dp).align(Alignment.BottomCenter).clip(RoundedCornerShape(18.dp)).background(Cz.TealDark))
            Box(Modifier.fillMaxWidth().height(52.dp).align(Alignment.TopCenter).clip(RoundedCornerShape(18.dp)).background(Cz.Teal).clickable(onClick = onRetry), contentAlignment = Alignment.Center) {
                Text(I18nStore.t("common.retry", "Reintentar"), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun lessonIconVector(type: String): androidx.compose.ui.graphics.vector.ImageVector = when (type) {
    "roleplay" -> Icons.Default.SportsEsports
    "translation" -> Icons.Default.Translate
    "free_chat" -> Icons.AutoMirrored.Filled.Chat
    "listening" -> Icons.AutoMirrored.Filled.VolumeUp
    "pronunciation" -> Icons.Default.MicNone
    else -> Icons.Default.School
}
