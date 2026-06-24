package com.alturya.fluenta.tone

import android.speech.tts.TextToSpeech
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import java.util.Locale

// Tokens del handoff de Claude Design (Fluenta Lección Chino.dc.html).
private object Tc {
    val Teal = Color(0xFF0E9D8E)
    val TealDark = Color(0xFF0A6F64)
    val Ink = Color(0xFF15201D)
    val Sub = Color(0xFF5C6562)
    val Surface = Color(0xFFFBFCFB)
    val MintTop = Color(0xFFE4F6F1)
    val MintBot = Color(0xFFCDEEE6)
    val HeroInk = Color(0xFF0A4039)
    val Border = Color(0xFFDCE5E2)
    val Track = Color(0xFFE7EEEB)
    val TrackFill = Color(0xFFAFD9D1)
    val Green = Color(0xFF22A06B)
    val GreenBg = Color(0xFFDCF5E9)
    val Coral = Color(0xFFE8554B)
    val CoralBg = Color(0xFFFFE1DC)
    val Muted = Color(0xFF9AA39E)
}

/** Una sílaba con tono para entrenar. `pinyin` sin marca; `tone` 1..4. */
data class ToneWord(val hanzi: String, val pinyin: String, val tone: Int, val meaning: String) {
    val marked: String get() = mark(pinyin, tone)
}

private val STARTER = listOf(
    ToneWord("妈", "ma", 1, "mamá"),
    ToneWord("马", "ma", 3, "caballo"),
    ToneWord("骂", "ma", 4, "regañar"),
    ToneWord("你", "ni", 3, "tú"),
    ToneWord("好", "hao", 3, "bueno"),
    ToneWord("是", "shi", 4, "ser"),
)

@Composable
fun ToneTrainerScreen(words: List<ToneWord>? = null, onDone: () -> Unit = {}) {
    val context = LocalContext.current
    // Vocab REAL es→zh por defecto (HSK1 balanceado por tono); estable entre recomposiciones.
    val deck = remember(words) { (words ?: ToneVocab.session(perTone = 2)).ifEmpty { STARTER } }
    // TTS chino nativo (audio real sin backend).
    var ready by remember { mutableStateOf(false) }
    val tts = remember {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) { engine?.language = Locale.CHINESE; ready = true }
        }
        engine
    }
    DisposableEffect(Unit) { onDispose { tts?.stop(); tts?.shutdown() } }
    fun say(text: String, slow: Boolean = false) {
        if (!ready) return
        tts?.setSpeechRate(if (slow) 0.5f else 0.9f)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tone")
    }

    var idx by remember { mutableStateOf(0) }
    var phase by remember { mutableStateOf("present") } // present | quiz
    var picked by remember { mutableStateOf<Int?>(null) }
    val w = deck[idx.coerceIn(0, deck.lastIndex)]
    val total = deck.size

    LaunchedEffect(idx, phase, ready) { if (ready) say(w.hanzi) }

    fun next() {
        picked = null
        if (idx + 1 >= total) onDone() else { idx += 1; phase = "present" }
    }

    Column(Modifier.fillMaxSize().background(Tc.Surface).padding(top = 12.dp)) {
        // chrome: cerrar + progreso segmentado
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Tc.Muted, modifier = Modifier.size(24.dp).clickable { onDone() })
            Spacer(Modifier.width(12.dp))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(total) { i ->
                    Box(Modifier.weight(1f).height(7.dp).clip(RoundedCornerShape(999.dp)).background(if (i < idx || (i == idx && phase == "quiz")) Tc.Teal else Tc.Track))
                }
            }
        }

        if (phase == "present") {
            // ── 1 · Presentación: hanzi + pinyin + significado + escuchar ──
            Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 26.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("PALABRA NUEVA · UNIDAD 1", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Tc.TealDark, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(18.dp))
                Box(Modifier.size(168.dp).clip(CircleShape).background(Tc.MintBot), contentAlignment = Alignment.Center) {
                    Text(w.hanzi, fontSize = 104.sp, fontWeight = FontWeight.Bold, color = Tc.HeroInk)
                }
                Spacer(Modifier.height(18.dp))
                Text(w.marked, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = PinyinColors.of(w.tone))
                Text(w.meaning, fontSize = 17.sp, color = Tc.Sub, modifier = Modifier.padding(top = 2.dp))
                Spacer(Modifier.height(24.dp))
                Surface(shape = RoundedCornerShape(999.dp), color = Tc.Teal, shadowElevation = 0.dp, modifier = Modifier.clickable { say(w.hanzi) }) {
                    Row(Modifier.padding(horizontal = 22.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Escuchar", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                Text("Voz nativa · toca para repetir", fontSize = 12.sp, color = Tc.Muted, modifier = Modifier.padding(top = 12.dp))
            }
            Box(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 26.dp)) {
                Hard3dBtn("Continuar  →") { phase = "quiz" }
            }
        } else {
            // ── 2/3 · Entrenador de tono: elige + feedback ──
            Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 22.dp)) {
                Spacer(Modifier.height(14.dp))
                Text("¿Qué tono escuchas?", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Tc.Ink, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Text("Toca para oír, luego elige la curva.", fontSize = 13.sp, color = Tc.Sub, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 18.dp))
                // botón de audio grande
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(96.dp).clip(CircleShape).background(Tc.Teal).clickable { say(w.hanzi) }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Oír", tint = Color.White, modifier = Modifier.size(46.dp))
                    }
                }
                Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
                    Surface(shape = RoundedCornerShape(999.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, Tc.Border), modifier = Modifier.clickable { say(w.hanzi, slow = true) }) {
                        Text("⏯  Más lento", color = Tc.Sub, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp))
                    }
                }
                Box(Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(w.hanzi, fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Tc.Ink)
                        Text("  ${w.pinyin}?", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Tc.Muted)
                    }
                }
                Spacer(Modifier.height(18.dp))
                // 2x2 opciones de tono
                val tones = listOf(1, 2, 3, 4)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (row in 0..1) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            for (col in 0..1) {
                                val t = tones[row * 2 + col]
                                ToneOption(t, w.pinyin, picked, w.tone, Modifier.weight(1f)) {
                                    if (picked == null) { picked = t; if (t == w.tone) say(w.hanzi) }
                                }
                            }
                        }
                    }
                }
            }
            // pie: feedback + continuar
            Box(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 22.dp)) {
                when (picked) {
                    null -> Text("Toca una opción para responder", fontSize = 12.sp, color = Tc.Muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    w.tone -> Hard3dBtn("✓ ¡Correcto!  →", color = Tc.Green, dark = Color(0xFF158052)) { next() }
                    else -> Column {
                        Text("Era el ${w.tone}.º tono: ${w.marked}", fontSize = 13.sp, color = Tc.Coral, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp))
                        Hard3dBtn("Continuar  →") { next() }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToneOption(tone: Int, pinyin: String, picked: Int?, correct: Int, modifier: Modifier, onClick: () -> Unit) {
    val labels = mapOf(1 to "1.ER TONO", 2 to "2.º TONO", 3 to "3.ER TONO", 4 to "4.º TONO")
    val isPicked = picked == tone
    val isCorrect = tone == correct
    val border = when {
        picked != null && isCorrect -> Tc.Green
        isPicked && !isCorrect -> Tc.Coral
        else -> Tc.Border
    }
    val bg = when {
        picked != null && isCorrect -> Tc.GreenBg
        isPicked && !isCorrect -> Tc.CoralBg
        else -> Color.White
    }
    val curveColor = if (picked != null && isCorrect) Tc.Green else if (isPicked && !isCorrect) Tc.Coral else Tc.Teal
    Column(
        modifier.heightIn(min = 84.dp).clip(RoundedCornerShape(16.dp)).background(bg)
            .border(1.5.dp, border, RoundedCornerShape(16.dp)).clickable(enabled = picked == null, onClick = onClick)
            .padding(vertical = 13.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(labels[tone] ?: "", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Tc.Sub, letterSpacing = 0.6.sp)
        ToneCurve(tone, curveColor, Modifier.size(width = 44.dp, height = 22.dp))
        Text(mark(pinyin, tone), fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = PinyinColors.of(tone))
    }
}

/** Dibuja la curva del tono (— / ∨ \) como en el diseño. */
@Composable
private fun ToneCurve(tone: Int, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height; val sw = 3.dp.toPx()
        val path = when (tone) {
            1 -> listOf(Offset(w * 0.1f, h * 0.3f), Offset(w * 0.9f, h * 0.3f))
            2 -> listOf(Offset(w * 0.12f, h * 0.8f), Offset(w * 0.88f, h * 0.2f))
            3 -> listOf(Offset(w * 0.1f, h * 0.3f), Offset(w * 0.45f, h * 0.85f), Offset(w * 0.9f, h * 0.28f))
            else -> listOf(Offset(w * 0.12f, h * 0.2f), Offset(w * 0.88f, h * 0.8f))
        }
        for (i in 0 until path.size - 1) {
            drawLine(color, path[i], path[i + 1], strokeWidth = sw, cap = StrokeCap.Round)
        }
    }
}

@Composable
private fun Hard3dBtn(text: String, color: Color = Tc.Teal, dark: Color = Tc.TealDark, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(54.dp)) {
        Box(Modifier.fillMaxWidth().height(50.dp).align(Alignment.BottomCenter).clip(RoundedCornerShape(16.dp)).background(dark))
        Box(Modifier.fillMaxWidth().height(50.dp).align(Alignment.TopCenter).clip(RoundedCornerShape(16.dp)).background(color).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
            Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

/** Coloca la marca de tono sobre la vocal principal del pinyin. */
private fun mark(pinyin: String, tone: Int): String {
    if (tone < 1 || tone > 4) return pinyin
    val marks = mapOf(
        'a' to "āáǎà", 'e' to "ēéěè", 'i' to "īíǐì", 'o' to "ōóǒò", 'u' to "ūúǔù", 'ü' to "ǖǘǚǜ",
    )
    // prioridad de vocal: a, o, e, luego la última i/u/ü
    val target = when {
        pinyin.contains('a') -> 'a'
        pinyin.contains('o') -> 'o'
        pinyin.contains('e') -> 'e'
        pinyin.contains('ü') -> 'ü'
        pinyin.contains('u') -> 'u'
        pinyin.contains('i') -> 'i'
        else -> return pinyin
    }
    val rep = marks[target]?.get(tone - 1) ?: return pinyin
    return pinyin.replaceFirst(target, rep)
}
