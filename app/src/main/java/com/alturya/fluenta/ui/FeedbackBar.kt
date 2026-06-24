package com.alturya.fluenta.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.alturya.fluenta.data.Analytics
import com.alturya.fluenta.data.FeedbackStore
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.network.ApiClient
import com.alturya.fluenta.network.FeedbackBody
import kotlinx.coroutines.launch

// Handoff "Fluenta Feedback" (Claude Design) 1:1 — control de 1 toque de baja
// fricción por pantalla:
//   1) tres caras 😞😐😊 (sentiment) → envío instantáneo;
//   2) si es negativo, hoja de "¿por qué?" con chips de motivo + texto opcional;
//   3) confirmación de gracias.
// Fire-and-forget a POST /api/feedback con { screen, sentiment, reason }. Nunca
// bloquea ni rompe el flujo. Colócalo SIEMPRE fuera de la tarea principal.

// Paleta del handoff.
private val FbTeal = Color(0xFF0E9D8E)
private val FbTealDark = Color(0xFF0A6F64)
private val FbInk = Color(0xFF15201D)
private val FbMuted = Color(0xFF5C6562)
private val FbBorder = Color(0xFFEAF0ED)
private val FbChipBorder = Color(0xFFDCE5E2)
private val FbNegBg = Color(0xFFFFE1DC)
private val FbNeuBg = Color(0xFFFFE9C2)
private val FbPosBg = Color(0xFFCDEEE6)

private const val POSITIVE = "positive"
private const val NEUTRAL = "neutral"
private const val NEGATIVE = "negative"

@Composable
fun FeedbackBar(
    screen: String,
    modifier: Modifier = Modifier,
    prompt: String = I18nStore.t("feedback.howScreen", "¿Cómo va esta pantalla?"),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var done by remember(screen) { mutableStateOf(false) }
    var askReason by remember(screen) { mutableStateOf(false) }
    var dismissed by remember(screen) { mutableStateOf(false) }
    // Tope de frecuencia: si ya se opinó en esta pantalla hace < 72 h, no preguntar.
    var throttled by remember(screen) { mutableStateOf(false) }
    LaunchedEffect(screen) { throttled = !FeedbackStore.canAskScreen(context, screen) }

    fun send(sentiment: String, reason: String? = null, comment: String? = null) {
        scope.launch {
            runCatching {
                ApiClient.api.sendFeedback(
                    FeedbackBody(surface = screen, screen = screen, sentiment = sentiment, reason = reason, comment = comment),
                )
            }
            FeedbackStore.markScreen(context, screen) // arma el cooldown de 72 h
        }
        // Espejo en analítica para correlacionar con time-on-screen / drop-off.
        Analytics.track(
            context,
            Analytics.BUTTON_TAP,
            buildMap { put("target", "feedback_$sentiment"); put("screen", screen) },
        )
    }

    if (dismissed || throttled) return

    // Barra discreta blanca, nunca tapa el contenido.
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, FbBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (done) {
            Text(
                I18nStore.t("feedback.thanks", "¡Gracias por tu opinión!"),
                modifier = Modifier.weight(1f),
                color = FbTealDark,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
            )
        } else {
            Text(prompt, modifier = Modifier.weight(1f), color = FbMuted, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            FaceButton("😞", FbNegBg) { askReason = true }
            FaceButton("😐", FbNeuBg) { send(NEUTRAL); done = true }
            FaceButton("😊", FbPosBg) { send(POSITIVE); done = true }
            Text("✕", color = Color(0xFFC2CAC5), fontSize = 16.sp, modifier = Modifier.clickable { dismissed = true })
        }
    }

    if (askReason) {
        ReasonSheet(
            onDismiss = { askReason = false },
            onSubmit = { reason, comment ->
                send(NEGATIVE, reason = reason, comment = comment)
                askReason = false
                done = true
            },
        )
    }
}

@Composable
private fun FaceButton(emoji: String, activeBg: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(activeBg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, fontSize = 19.sp)
    }
}

// Hoja inferior "¿Qué falló aquí?" — motivo opcional, nunca obligatorio.
@Composable
private fun ReasonSheet(onDismiss: () -> Unit, onSubmit: (reason: String?, comment: String?) -> Unit) {
    val reasons = listOf(
        I18nStore.t("feedback.reason.confusing", "Confuso"),
        I18nStore.t("feedback.reason.tooHard", "Muy difícil"),
        I18nStore.t("feedback.reason.bug", "Error / bug"),
        I18nStore.t("feedback.reason.slow", "Lento"),
        I18nStore.t("feedback.reason.audio", "Audio"),
        I18nStore.t("feedback.reason.other", "Otro"),
    )
    var selected by remember { mutableStateOf<String?>(null) }
    var comment by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .padding(22.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(FbNegBg),
                    contentAlignment = Alignment.Center,
                ) { Text("😞", fontSize = 22.sp) }
                Spacer(Modifier.width(11.dp))
                Column {
                    Text(I18nStore.t("feedback.whatFailed", "¿Qué falló aquí?"), color = FbInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(I18nStore.t("feedback.optional", "Opcional · nos ayuda a mejorar"), color = Color(0xFF8A938E), fontSize = 12.5.sp)
                }
            }
            Spacer(Modifier.height(16.dp))

            // Chips de motivo (wrap).
            FlowChips(reasons, selected) { selected = if (selected == it) null else it }
            Spacer(Modifier.height(14.dp))

            // Texto opcional.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF8FAF9))
                    .border(1.dp, FbChipBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 13.dp),
            ) {
                if (comment.isEmpty()) {
                    Text(I18nStore.t("feedback.tellMore", "Cuéntanos más… (opcional)"), color = Color(0xFF9AA39E), fontSize = 13.5.sp)
                }
                BasicTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    textStyle = TextStyle(color = FbInk, fontSize = 13.5.sp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF1F5F3))
                        .clickable { onSubmit(null, null) }
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                ) { Text(I18nStore.t("feedback.skip", "Omitir"), color = FbMuted, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(FbTeal)
                        .clickable { onSubmit(selected, comment.ifBlank { null }) }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(I18nStore.t("feedback.send", "Enviar"), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun FlowChips(items: List<String>, selected: String?, onPick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { label ->
                    val active = selected == label
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (active) Color(0xFFEAF7F4) else Color.White)
                            .border(if (active) 1.5.dp else 1.dp, if (active) FbTeal else FbChipBorder, RoundedCornerShape(999.dp))
                            .clickable { onPick(label) }
                            .padding(horizontal = 13.dp, vertical = 9.dp),
                    ) {
                        Text(label, color = if (active) FbTealDark else Color(0xFF374039), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ===================== ESTADO 4: CSAT 0-10 (fin de sesión) =====================
// NPS-style del handoff: Hoot + "¿qué probabilidad hay de que recomiendes Fluenta?"
// + escala 0-10 + chips de "qué te gusta". Se pide MÁX 1 vez / 7 días
// (guardar antes con FeedbackStore.canAskCsat). Manda rating + motivo a /api/feedback.
@Composable
fun CsatSheet(screen: String = "session_end", onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var score by remember { mutableStateOf<Int?>(null) }
    val liked = remember { mutableStateListOf<String>() }

    val likeOptions = listOf(
        I18nStore.t("feedback.like.hoot", "Hoot"),
        I18nStore.t("feedback.like.tones", "Tonos"),
        I18nStore.t("feedback.like.streaks", "Rachas"),
        I18nStore.t("feedback.like.conversation", "Conversación"),
    )

    fun submit() {
        val rating = score ?: return
        scope.launch {
            runCatching {
                ApiClient.api.sendFeedback(
                    FeedbackBody(
                        surface = "csat",
                        screen = screen,
                        rating = rating,
                        reason = liked.joinToString(",").ifBlank { null },
                    ),
                )
            }
            FeedbackStore.markCsat(context) // arma el cooldown de 7 días
        }
        Analytics.track(
            context,
            Analytics.BUTTON_TAP,
            buildMap { put("target", "csat_submit"); put("score", rating.toString()) },
        )
        onClose()
    }

    Dialog(onDismissRequest = onClose) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HootMascot(modifier = Modifier.size(78.dp))
            Spacer(Modifier.height(14.dp))
            Text(
                I18nStore.t("feedback.csat.q", "¿Qué probabilidad hay de que recomiendes Fluenta?"),
                color = FbInk, fontSize = 20.sp, fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(I18nStore.t("feedback.csat.sub", "Solo esta vez · 10 segundos"), color = Color(0xFF8A938E), fontSize = 13.sp)
            Spacer(Modifier.height(20.dp))
            ScoreRow(0..5, score) { score = it }
            Spacer(Modifier.height(6.dp))
            ScoreRow(6..10, score) { score = it }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(I18nStore.t("feedback.csat.low", "Nada probable"), color = Color(0xFF9AA39E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(I18nStore.t("feedback.csat.high", "Muy probable"), color = Color(0xFF9AA39E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(18.dp))
            Text(
                I18nStore.t("feedback.csat.likeTitle", "¿QUÉ ES LO QUE MÁS TE GUSTA?"),
                modifier = Modifier.fillMaxWidth(), color = Color(0xFF6B746F), fontSize = 12.sp, fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(9.dp))
            FlowChipsMulti(likeOptions, liked)
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (score != null) FbTeal else Color(0xFFBFD6D1))
                    .clickable(enabled = score != null) { submit() }
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center,
            ) { Text(I18nStore.t("feedback.send", "Enviar"), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun ScoreRow(range: IntRange, selected: Int?, onPick: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        range.forEach { n ->
            val active = selected == n
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (active) FbTeal else Color.White)
                    .border(if (active) 2.dp else 1.5.dp, if (active) FbTeal else FbChipBorder, RoundedCornerShape(9.dp))
                    .clickable { onPick(n) },
                contentAlignment = Alignment.Center,
            ) {
                Text(n.toString(), color = if (active) Color.White else FbMuted, fontSize = if (active) 15.sp else 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FlowChipsMulti(items: List<String>, selected: MutableList<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { label ->
                    val active = selected.contains(label)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (active) Color(0xFFEAF7F4) else Color.White)
                            .border(if (active) 1.5.dp else 1.dp, if (active) FbTeal else FbChipBorder, RoundedCornerShape(999.dp))
                            .clickable { if (active) selected.remove(label) else selected.add(label) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text(label, color = if (active) FbTealDark else Color(0xFF374039), fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ===================== ESTADO 5: control colapsado en esquina =====================
// Para pantallas de FOCO (trazado de hanzi, etc.) donde una barra distrae: pill
// "Opinar" que al tocar despliega la barra de 1 toque. Mismo tope de 72 h.
@Composable
fun FeedbackPill(screen: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var expanded by remember(screen) { mutableStateOf(false) }
    var throttled by remember(screen) { mutableStateOf(false) }
    LaunchedEffect(screen) { throttled = !FeedbackStore.canAskScreen(context, screen) }
    if (throttled) return

    if (expanded) {
        FeedbackBar(screen = screen, modifier = modifier)
    } else {
        Row(
            modifier = modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White)
                .border(1.dp, FbBorder, RoundedCornerShape(999.dp))
                .clickable { expanded = true }
                .padding(start = 10.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text("✎", color = FbTeal, fontSize = 18.sp)
            Text(I18nStore.t("feedback.opine", "Opinar"), color = FbTealDark, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
        }
    }
}
