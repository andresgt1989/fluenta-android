package com.alturya.fluenta.script

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alturya.fluenta.data.I18nStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/* ──────────────────────────────────────────────────────────────────────────
 * [13] HanziReviewScreen · SRS de caracteres
 * Implementación FIEL del mockup de Claude Design ("13 HanziReview.dc.html").
 * Recall (pinyin+significado, sin glifo) → revelar → Fallé / Lo recordé.
 * El CABLEADO existente (HanziSrsStore.due/grade, onDone) se adapta a la
 * pantalla nueva; los datos vienen de HanziCard(glyph, romanization, meaning).
 * Paleta esmeralda + botones 3D tomados 1:1 del diseño.
 * ────────────────────────────────────────────────────────────────────────── */

private object Hz {
    val Bg = Color(0xFFF1FAF6)
    val Emerald = Color(0xFF10B981)
    val EmeraldDark = Color(0xFF059669)
    val Ink = Color(0xFF0F2E27)
    val Muted = Color(0xFF5B7268)
    val Track = Color(0xFFCDEEE6)
    val Amber = Color(0xFFE08A00)
    val ChipBg = Color(0xFFCDEEE6)
    val ChipText = Color(0xFF06463A)
    val PlaceholderBg = Color(0xFFF6FCFA)
    val PlaceholderInk = Color(0xFFC3D2CC)
    val FailShadow = Color(0xFFE0E8E4)
    val FailBorder = Color(0xFFE7EFEB)
    val Dark = Color(0xFF0F2E27)
    val DoneTitle = Color(0xFF063D32)
    val DoneSub = Color(0xFF0B5A48)
}

@Composable
fun HanziReviewScreen(l2: String, onDone: () -> Unit = {}, previewQueue: List<HanziCard>? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val now = remember { System.currentTimeMillis() }

    var queue by remember { mutableStateOf(previewQueue) }
    LaunchedEffect(l2) {
        if (previewQueue == null) queue = HanziSrsStore.due(context, l2, now).first()
    }

    var index by remember { mutableStateOf(0) }
    var revealed by remember { mutableStateOf(false) }
    var remembered by remember { mutableStateOf(0) }
    var failed by remember { mutableStateOf(0) }

    val q = queue
    // ── Estado CARGANDO ──────────────────────────────────────────────────────
    if (q == null) {
        Box(Modifier.fillMaxSize().background(Hz.Bg), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
                CircularProgressIndicator(color = Hz.Emerald, trackColor = Hz.Track, strokeWidth = 5.dp, modifier = Modifier.size(54.dp))
                Text(I18nStore.t("hanzi.review.loading", "Cargando caracteres a repasar…"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Hz.Muted)
            }
        }
        return
    }

    val total = q.size
    val current = q.getOrNull(index)

    // ── Estado VACÍO / COMPLETADO ────────────────────────────────────────────
    if (current == null) {
        if (total == 0) EmptyState(onLearnNew = onDone)
        else CompletedState(total = total, remembered = remembered, failed = failed, onDone = onDone)
        return
    }

    fun grade(rememberedIt: Boolean) {
        val glyph = current.glyph
        if (previewQueue == null) {
            scope.launch { HanziSrsStore.grade(context, l2, glyph, rememberedIt, now) }
        }
        if (rememberedIt) remembered += 1 else failed += 1
        index += 1
        revealed = false
    }

    // ── Estados RECALL / REVELADO ─────────────────────────────────────────────
    Column(Modifier.fillMaxSize().background(Hz.Bg).padding(horizontal = 26.dp)) {
        Spacer(Modifier.height(14.dp))
        TopBar(index = index, total = total, onClose = onDone)

        if (!revealed) {
            Spacer(Modifier.height(16.dp))
            Text(
                I18nStore.t("hanzi.review.recallLabel", "RECUERDA EL CARÁCTER"),
                color = Hz.EmeraldDark, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 0.5.sp,
            )
            Spacer(Modifier.height(18.dp))
            // Tarjeta de recall (sin glifo)
            Column(
                Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(22.dp)).background(Color.White).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    Modifier.size(150.dp).clip(RoundedCornerShape(20.dp))
                        .background(Hz.PlaceholderBg).border(3.dp, Hz.Track, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center,
                ) { Text("？", fontSize = 54.sp, color = Hz.PlaceholderInk) }
                Spacer(Modifier.height(18.dp))
                if (current.romanization.isNotBlank()) {
                    Text(current.romanization, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Hz.Amber)
                }
                if (current.meaning.isNotBlank()) {
                    Text("«${current.meaning}»", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Hz.Ink, modifier = Modifier.padding(top = 4.dp))
                }
                Spacer(Modifier.height(18.dp))
                Text(I18nStore.t("hanzi.review.hint", "¿Puedes visualizar el trazo?"), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF9CB3AB))
            }
            Spacer(Modifier.height(18.dp))
            Hard3dButton(
                modifier = Modifier.fillMaxWidth(), face = Hz.Emerald, shadow = Hz.EmeraldDark, onClick = { revealed = true },
            ) { Text(I18nStore.t("hanzi.review.reveal", "Revelar trazos"), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(26.dp))
        } else {
            Spacer(Modifier.height(18.dp))
            Column(
                Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(22.dp)).background(Color.White).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Glifo en bloque esmeralda con sombra 3D
                Box(Modifier.size(155.dp)) {
                    Box(Modifier.size(150.dp).align(Alignment.BottomCenter).clip(RoundedCornerShape(20.dp)).background(Hz.EmeraldDark))
                    Box(Modifier.size(150.dp).align(Alignment.TopCenter).clip(RoundedCornerShape(20.dp)).background(Hz.Emerald), contentAlignment = Alignment.Center) {
                        Text(current.glyph, fontSize = 96.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                }
                Spacer(Modifier.height(16.dp))
                // Chip pinyin · significado
                Row(
                    Modifier.clip(RoundedCornerShape(99.dp)).background(Hz.ChipBg).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val label = listOf(current.romanization, current.meaning).filter { it.isNotBlank() }.joinToString(" · ")
                    Text("🔊 $label", color = Hz.ChipText, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(Modifier.height(16.dp))
                // Orden de trazos (datos reales vía StrokeWriter)
                Text(I18nStore.t("hanzi.review.strokeOrder", "Orden de trazos"), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Hz.Muted, modifier = Modifier.align(Alignment.Start))
                Spacer(Modifier.height(8.dp))
                StrokeWriter(glyph = current.glyph, modifier = Modifier.fillMaxWidth().weight(1f, fill = false).height(150.dp))
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Hard3dButton(
                    modifier = Modifier.weight(1f), face = Color.White, shadow = Hz.FailShadow, border = Hz.FailBorder, height = 58.dp, onClick = { grade(false) },
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(I18nStore.t("repaso.failed", "Fallé"), color = Hz.Muted, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                        Text(I18nStore.t("hanzi.review.failSub", "repetir pronto"), color = Hz.Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Hard3dButton(
                    modifier = Modifier.weight(1f), face = Hz.Emerald, shadow = Hz.EmeraldDark, height = 58.dp, onClick = { grade(true) },
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(I18nStore.t("repaso.remembered", "Lo recordé"), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                        Text("+5 XP", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(26.dp))
        }
    }
}

/* ----------------------------- Subcomponentes ----------------------------- */

@Composable
private fun TopBar(index: Int, total: Int, onClose: () -> Unit) {
    val shown = (index + 1).coerceAtMost(total)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("✕", fontSize = 22.sp, color = Hz.Muted, modifier = Modifier.clickable(onClick = onClose))
        Box(Modifier.weight(1f).height(12.dp).clip(RoundedCornerShape(99.dp)).background(Hz.Track)) {
            Box(Modifier.fillMaxWidth(if (total == 0) 0f else shown / total.toFloat()).fillMaxHeight().clip(RoundedCornerShape(99.dp)).background(Hz.Emerald))
        }
        Text("$shown/$total", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Hz.Ink)
    }
}

@Composable
private fun Hard3dButton(
    face: Color,
    shadow: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 56.dp,
    border: Color? = null,
    content: @Composable () -> Unit,
) {
    val depth = 4.dp
    Box(modifier.height(height + depth)) {
        Box(Modifier.fillMaxWidth().height(height).align(Alignment.BottomCenter).clip(RoundedCornerShape(14.dp)).background(shadow))
        Box(
            Modifier.fillMaxWidth().height(height).align(Alignment.TopCenter).clip(RoundedCornerShape(14.dp)).background(face)
                .then(if (border != null) Modifier.border(2.dp, border, RoundedCornerShape(14.dp)) else Modifier)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { content() }
    }
}

@Composable
private fun CompletedState(total: Int, remembered: Int, failed: Int, onDone: () -> Unit) {
    val xp = remembered * 5
    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Hz.Emerald, 0.7f to Hz.Track, 1f to Hz.Bg)).padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(120.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
            Box(Modifier.size(64.dp).clip(CircleShape).background(Hz.Emerald), contentAlignment = Alignment.Center) {
                Text("✓", fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
        }
        Spacer(Modifier.height(22.dp))
        Text(I18nStore.t("hanzi.review.doneTitle", "¡Repaso al día!"), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Hz.DoneTitle, textAlign = TextAlign.Center)
        Text(
            I18nStore.t("hanzi.review.doneSub", "Revisaste %d de %d caracteres").replace("%d de %d", "$total de $total"),
            fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Hz.DoneSub, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp),
        )
        Spacer(Modifier.height(22.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("✅", "$remembered", I18nStore.t("hanzi.review.statRemembered", "Recordados"), Color(0xFF0B7B53), Modifier.weight(1f))
            StatCard("🔁", "$failed", I18nStore.t("hanzi.review.statRepeat", "A repetir"), Hz.Amber, Modifier.weight(1f))
            StatCard("⚡", "+$xp", "XP", Hz.Ink, Modifier.weight(1f))
        }
        Spacer(Modifier.weight(1f))
        Hard3dButton(modifier = Modifier.fillMaxWidth(), face = Hz.Dark, shadow = Color(0x40000000), onClick = onDone) {
            Text(I18nStore.t("common.finish", "Terminar"), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(26.dp))
    }
}

@Composable
private fun StatCard(emoji: String, value: String, label: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.95f)).padding(vertical = 16.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, fontSize = 22.sp)
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = valueColor, modifier = Modifier.padding(top = 4.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Hz.Muted)
    }
}

@Composable
private fun EmptyState(onLearnNew: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Hz.Bg).padding(horizontal = 26.dp)) {
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("←", fontSize = 22.sp, color = Hz.Muted, modifier = Modifier.clickable(onClick = onLearnNew))
            Text(I18nStore.t("hanzi.review.title", "Repaso de hanzi"), fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = Hz.Ink)
        }
        Column(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("🦉", fontSize = 84.sp)
            Spacer(Modifier.height(18.dp))
            Text(I18nStore.t("hanzi.review.emptyTitle", "Nada que repasar hoy"), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Hz.Ink, textAlign = TextAlign.Center)
            Text(
                I18nStore.t("hanzi.review.emptyBody", "Tus caracteres están frescos. Vuelve mañana para mantener la memoria a largo plazo."),
                fontSize = 15.sp, color = Hz.Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp),
            )
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White).border(2.dp, Hz.Emerald, RoundedCornerShape(12.dp))
                    .clickable(onClick = onLearnNew).padding(horizontal = 26.dp, vertical = 12.dp),
            ) { Text(I18nStore.t("hanzi.review.learnNew", "Aprender nuevos"), color = Hz.EmeraldDark, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
        }
    }
}
