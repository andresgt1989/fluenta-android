package com.alturya.fluenta.repaso

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.data.Session
import com.alturya.fluenta.network.ApiClient
import com.alturya.fluenta.network.ErrorItem
import com.alturya.fluenta.network.ErrorReviewBody
import com.alturya.fluenta.util.isRtl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/* ──────────────────────────────────────────────────────────────────────────
 * [11] RepasoScreen · SRS de errores — port FIEL de "11 Repaso.dc.html".
 * Error previo (oculto) → "Mostrar respuesta" → corrección revelada (verde) →
 * Fallé / Lo recordé. Estados: recall / revelado / fin(XP) / vacío / cargando /
 * error. Cableado existente intacto (RepasoViewModel: getErrors → reviewError).
 * ────────────────────────────────────────────────────────────────────────── */

data class RepasoState(
    val loading: Boolean = true,
    val queue: List<ErrorItem> = emptyList(),
    val index: Int = 0,
    val revealed: Boolean = false,
    val reviewedCount: Int = 0,
    val error: String? = null,
)

class RepasoViewModel : ViewModel() {
    private val _state = MutableStateFlow(RepasoState())
    val state = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = RepasoState(loading = true)
        viewModelScope.launch {
            try {
                val res = ApiClient.api.getErrors()
                // Repasamos los no dominados, con corrección real, en el orden del SRS del backend.
                val due = res.errors.filter {
                    it.masteredAt == null && !it.id.isNullOrBlank() && !it.corrected.isNullOrBlank()
                }
                _state.value = RepasoState(loading = false, queue = due)
            } catch (e: Exception) {
                _state.value = RepasoState(loading = false, error = I18nStore.t("repaso.error.load", "No se pudo cargar el repaso. Reintenta."))
            }
        }
    }

    fun reveal() = _state.update { it.copy(revealed = true) }

    fun answer(remembered: Boolean) {
        val s = _state.value
        val id = s.queue.getOrNull(s.index)?.id ?: return
        viewModelScope.launch {
            try { ApiClient.api.reviewError(id, ErrorReviewBody(remembered)) } catch (_: Exception) { /* best-effort */ }
        }
        _state.update { it.copy(index = it.index + 1, revealed = false, reviewedCount = it.reviewedCount + 1) }
    }
}

private object Rz {
    val Bg = Color(0xFFF1FAF6)
    val Emerald = Color(0xFF10B981)
    val EmeraldDark = Color(0xFF059669)
    val Ink = Color(0xFF0F2E27)
    val Muted = Color(0xFF5B7268)
    val Track = Color(0xFFCDEEE6)
    val FailBadgeBg = Color(0xFFFDE7E4)
    val FailBadgeInk = Color(0xFFC13B32)
    val CorrectBg = Color(0xFFD6F4E4)
    val CorrectInk = Color(0xFF0B7B53)
    val TipBg = Color(0xFFFFF3DC)
    val TipInk = Color(0xFF8A6A2A)
    val FailShadow = Color(0xFFE0E8E4)
    val FailBorder = Color(0xFFE7EFEB)
    val Dark = Color(0xFF0F2E27)
    val DoneTitle = Color(0xFF063D32)
    val DoneSub = Color(0xFF0B5A48)
    val Amber = Color(0xFFE08A00)
}

@Composable
fun RepasoScreen(onDone: () -> Unit, previewState: RepasoState? = null) {
    val vm: RepasoViewModel = viewModel()
    val vmState by vm.state.collectAsState()
    val state = previewState ?: vmState

    // Conteo local dominado/repetir para la pantalla de fin (el VM solo cuenta total).
    var remembered by remember { mutableStateOf(0) }
    var failed by remember { mutableStateOf(0) }
    fun grade(ok: Boolean) {
        if (ok) remembered += 1 else failed += 1
        vm.answer(ok)
    }

    // ── CARGANDO ──────────────────────────────────────────────────────────────
    if (state.loading) {
        Box(Modifier.fillMaxSize().background(Rz.Bg), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
                CircularProgressIndicator(color = Rz.Emerald, trackColor = Rz.Track, strokeWidth = 5.dp, modifier = Modifier.size(54.dp))
                Text(I18nStore.t("repaso.loading", "Buscando tus errores…"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Rz.Muted)
            }
        }
        return
    }

    // ── ERROR ─────────────────────────────────────────────────────────────────
    if (state.error != null) {
        CenteredMascotState(
            title = I18nStore.t("repaso.error.title", "No cargó tu repaso"),
            body = I18nStore.t("repaso.error.body", "Revisa tu conexión e inténtalo de nuevo."),
            ctaText = I18nStore.t("common.retry", "Reintentar"),
            ctaFilled = true,
            onCta = { vm.load() },
        )
        return
    }

    val total = state.queue.size
    val current = state.queue.getOrNull(state.index)

    // ── VACÍO / FIN ─────────────────────────────────────────────────────────--
    if (current == null) {
        if (total == 0) {
            CenteredMascotState(
                title = I18nStore.t("repaso.emptyTitle", "Nada que repasar hoy"),
                body = I18nStore.t("repaso.emptyBody", "¡Vas al día! Vuelve mañana o haz una lección nueva para sumar XP."),
                ctaText = I18nStore.t("repaso.newLesson", "Nueva lección"),
                ctaFilled = false,
                onCta = onDone,
            )
        } else {
            FinishedState(total = total, remembered = remembered, failed = failed, onDone = onDone)
        }
        return
    }

    val l2Dir = if (isRtl(Session.l2)) TextDirection.Rtl else TextDirection.ContentOrLtr

    // ── RECALL / REVELADO ──────────────────────────────────────────────────────
    Column(Modifier.fillMaxSize().background(Rz.Bg).padding(horizontal = 26.dp)) {
        Spacer(Modifier.height(14.dp))
        TopBarRz(index = state.index, total = total, onClose = onDone)

        if (!state.revealed) {
            Spacer(Modifier.height(16.dp))
            Text(I18nStore.t("repaso.recallLabel", "REPASO · LO QUE FALLASTE"), color = Rz.EmeraldDark, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(18.dp))
            Column(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(22.dp)).background(Color.White).padding(24.dp, 24.dp)) {
                Row(Modifier.clip(RoundedCornerShape(99.dp)).background(Rz.FailBadgeBg).padding(horizontal = 11.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("✕ ${I18nStore.t("repaso.youMissed", "Lo fallaste antes")}", color = Rz.FailBadgeInk, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(Modifier.height(22.dp))
                Text(I18nStore.t("repaso.yourMistake", "Tu error anterior:"), color = Rz.Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(current.original ?: "—", color = Rz.Ink, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 31.sp, style = TextStyle(textDirection = l2Dir))
                Spacer(Modifier.weight(1f))
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🧠", fontSize = 36.sp, color = Color(0xFFC3D2CC))
                    Text(I18nStore.t("repaso.recallHint", "Recuerda la forma correcta…"), color = Color(0xFFC3D2CC), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
                }
                Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(18.dp))
            Hard3dRz(modifier = Modifier.fillMaxWidth(), face = Rz.Emerald, shadow = Rz.EmeraldDark, onClick = vm::reveal) {
                Text(I18nStore.t("repaso.reveal", "Mostrar respuesta"), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(26.dp))
        } else {
            Spacer(Modifier.height(18.dp))
            Column(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(22.dp)).background(Color.White).padding(24.dp, 24.dp)) {
                Text(current.original ?: "—", color = Rz.Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold, style = TextStyle(textDirection = l2Dir))
                Spacer(Modifier.height(14.dp))
                // Tarjeta de respuesta correcta (verde)
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Rz.CorrectBg).border(2.dp, Rz.Emerald, RoundedCornerShape(16.dp)).padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(Modifier.clip(RoundedCornerShape(99.dp)).background(Rz.Emerald).padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text("✓ ${I18nStore.t("repaso.correctAnswer", "Respuesta correcta")}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Text(current.corrected ?: "—", color = Rz.CorrectInk, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 34.sp, textAlign = TextAlign.Center, style = TextStyle(textDirection = l2Dir), modifier = Modifier.padding(top = 12.dp))
                }
                // Tip de gramática (si el backend etiquetó la categoría del error)
                current.errorCategory?.takeIf { it.isNotBlank() }?.let { cat ->
                    Row(Modifier.fillMaxWidth().padding(top = 14.dp).clip(RoundedCornerShape(14.dp)).background(Rz.TipBg).padding(13.dp), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                        Text("💡", fontSize = 18.sp)
                        Text(cat.replaceFirstChar { it.uppercase() }, color = Rz.TipInk, fontSize = 13.sp, lineHeight = 19.sp)
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Hard3dRz(modifier = Modifier.weight(1f), face = Color.White, shadow = Rz.FailShadow, border = Rz.FailBorder, height = 58.dp, onClick = { grade(false) }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(I18nStore.t("repaso.failed", "Fallé"), color = Rz.Muted, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                        Text(I18nStore.t("repaso.failSub", "repetir pronto"), color = Rz.Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Hard3dRz(modifier = Modifier.weight(1f), face = Rz.Emerald, shadow = Rz.EmeraldDark, height = 58.dp, onClick = { grade(true) }) {
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
private fun TopBarRz(index: Int, total: Int, onClose: () -> Unit) {
    val shown = (index + 1).coerceAtMost(total)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("✕", fontSize = 22.sp, color = Rz.Muted, modifier = Modifier.clickable(onClick = onClose))
        Box(Modifier.weight(1f).height(12.dp).clip(RoundedCornerShape(99.dp)).background(Rz.Track)) {
            Box(Modifier.fillMaxWidth(if (total == 0) 0f else shown / total.toFloat()).fillMaxHeight().clip(RoundedCornerShape(99.dp)).background(Rz.Emerald))
        }
        Text("$shown/$total", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Rz.Ink)
    }
}

@Composable
private fun Hard3dRz(
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
private fun FinishedState(total: Int, remembered: Int, failed: Int, onDone: () -> Unit) {
    val xp = remembered * 5
    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Rz.Emerald, 0.7f to Rz.Track, 1f to Rz.Bg)).padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(120.dp).clip(RoundedCornerShape(60.dp)).background(Color.White), contentAlignment = Alignment.Center) {
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(32.dp)).background(Rz.Emerald), contentAlignment = Alignment.Center) {
                Text("✓", fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
        }
        Spacer(Modifier.height(22.dp))
        Text(I18nStore.t("repaso.doneTitle", "¡Repaso completado!"), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Rz.DoneTitle, textAlign = TextAlign.Center)
        Text(I18nStore.t("repaso.doneSub", "Recuerdos reforzados hoy"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Rz.DoneSub, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(22.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatRz("⚡", "+$xp", "XP", Rz.Amber, Modifier.weight(1f))
            StatRz("🧠", "$remembered", I18nStore.t("repaso.mastered", "Dominados"), Rz.CorrectInk, Modifier.weight(1f))
            StatRz("🔁", "$failed", I18nStore.t("repaso.repeat", "Repetir"), Rz.Ink, Modifier.weight(1f))
        }
        Spacer(Modifier.weight(1f))
        Hard3dRz(modifier = Modifier.fillMaxWidth(), face = Rz.Dark, shadow = Color(0x40000000), onClick = onDone) {
            Text(I18nStore.t("common.finish", "Terminar"), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(26.dp))
    }
}

@Composable
private fun StatRz(emoji: String, value: String, label: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.95f)).padding(vertical = 16.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, fontSize = 22.sp)
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = valueColor, modifier = Modifier.padding(top = 4.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Rz.Muted)
    }
}

@Composable
private fun CenteredMascotState(title: String, body: String, ctaText: String, ctaFilled: Boolean, onCta: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Rz.Bg).padding(horizontal = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        com.alturya.fluenta.ui.HootMascot(Modifier.size(104.dp))
        Spacer(Modifier.height(18.dp))
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Rz.Ink, textAlign = TextAlign.Center)
        Text(body, fontSize = 15.sp, color = Rz.Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(18.dp))
        if (ctaFilled) {
            Hard3dRz(modifier = Modifier.fillMaxWidth(), face = Rz.Emerald, shadow = Rz.EmeraldDark, onClick = onCta) {
                Text(ctaText, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Box(
                Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White).border(2.dp, Rz.Emerald, RoundedCornerShape(12.dp)).clickable(onClick = onCta).padding(horizontal = 26.dp, vertical = 12.dp),
            ) { Text(ctaText, color = Rz.EmeraldDark, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
        }
    }
}
