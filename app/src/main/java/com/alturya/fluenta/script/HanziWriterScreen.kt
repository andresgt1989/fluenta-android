package com.alturya.fluenta.script

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.tone.PinyinColors

// Pantalla DEDICADA de escritura de hanzi, a pantalla completa.
//
// Por qué existe: antes el dibujo vivía incrustado en una lista que scrolleaba, y el
// WebView del trazo secuestraba el gesto vertical → no se podía scrollear ni dibujar
// bien. Aquí el lienzo ocupa toda la pantalla (sin scroll alrededor) y se navega entre
// caracteres con ‹ ›. Reusa ScriptViewModel para no duplicar la carga de la lección.
//
// Estilo: kit de Claude Design ESMERALDA, alineado 1:1 con su hermana HanziReviewScreen
// (mismo palette + botones 3D). El pinyin se colorea por TONO (PinyinColors, reutilizable).
private object Hw {
    val Bg = Color(0xFFF1FAF6)
    val Emerald = Color(0xFF10B981)
    val EmeraldDark = Color(0xFF059669)
    val Ink = Color(0xFF0F2E27)
    val Muted = Color(0xFF5B7268)
    val Track = Color(0xFFCDEEE6)
    val Surface = Color(0xFFFFFFFF)
    val Border = Color(0xFFDCEEE7)
}

@Composable
fun HanziWriterScreen(l2: String, startGlyph: String? = null, onDone: () -> Unit = {}) {
    val vm: ScriptViewModel = viewModel()
    val state by vm.state.collectAsState()

    LaunchedEffect(l2) { vm.load(l2) }

    val items = state.lesson?.items.orEmpty()

    Column(Modifier.fillMaxSize().background(Hw.Bg)) {
        // ── chrome: cerrar + título ──
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(Hw.Surface).clickable { onDone() },
                contentAlignment = Alignment.Center,
            ) { Text("✕", fontSize = 17.sp, color = Hw.Muted, fontWeight = FontWeight.Bold) }
            Text(
                I18nStore.t("hanzi.writer.title", "Escribir trazos"),
                Modifier.weight(1f), textAlign = TextAlign.Center,
                fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Hw.Ink,
            )
            Spacer(Modifier.size(36.dp)) // equilibra el botón de cerrar
        }

        Box(Modifier.fillMaxSize()) {
            when {
                state.phase == ScriptPhase.LOADING ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center), color = Hw.Emerald)

                items.isEmpty() ->
                    Text(
                        I18nStore.t("hanzi.writer.empty", "No hay caracteres para practicar todavía."),
                        Modifier.align(Alignment.Center).padding(24.dp),
                        textAlign = TextAlign.Center, color = Hw.Muted,
                    )

                else -> {
                    var index by remember(items) {
                        mutableStateOf(items.indexOfFirst { it.glyph == startGlyph }.coerceAtLeast(0))
                    }
                    val item = items[index]

                    Column(
                        Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Pista de lectura: pinyin coloreado por tono + significado.
                        Spacer(Modifier.height(8.dp))
                        Text(
                            item.romanization, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
                            color = PinyinColors.ofMarked(item.romanization),
                        )
                        item.meaning?.takeIf { it.isNotBlank() }?.let {
                            Text(it, fontSize = 16.sp, color = Hw.Muted)
                        }

                        // Lienzo de trazos: ocupa el espacio libre, sin scroll alrededor.
                        // key(glyph) fuerza recrear el WebView al cambiar de carácter.
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            key(item.glyph) { StrokeWriter(glyph = item.glyph) }
                        }

                        // Navegación entre caracteres (botones 3D del kit).
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            NavBtn(I18nStore.t("common.prev", "‹ Anterior"), enabled = index > 0) { if (index > 0) index-- }
                            Text("${index + 1} / ${items.size}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Hw.Muted)
                            NavBtn(I18nStore.t("common.next", "Siguiente ›"), enabled = index < items.lastIndex) { if (index < items.lastIndex) index++ }
                        }
                    }
                }
            }

            // Feedback colapsado (handoff "Fluenta Feedback" · estado 5: pantalla de
            // FOCO). Pill discreto en esquina que no tapa el trazado; respeta el tope 72h.
            if (items.isNotEmpty() && state.phase != ScriptPhase.LOADING) {
                com.alturya.fluenta.ui.FeedbackPill(
                    screen = "hanzi_writer",
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                )
            }
        }
    }
}

/** Botón de navegación 3D del kit esmeralda (borde inferior oscuro). */
@Composable
private fun NavBtn(text: String, enabled: Boolean, onClick: () -> Unit) {
    val alpha = if (enabled) 1f else 0.4f
    Box(Modifier.height(46.dp)) {
        Box(Modifier.matchParentSize().padding(top = 4.dp).clip(RoundedCornerShape(14.dp)).background(Hw.EmeraldDark.copy(alpha = alpha)))
        Box(
            Modifier.fillMaxHeight().padding(bottom = 4.dp).clip(RoundedCornerShape(14.dp))
                .background(Hw.Emerald.copy(alpha = alpha)).clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 22.dp),
            contentAlignment = Alignment.Center,
        ) { Text(text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1) }
    }
}
