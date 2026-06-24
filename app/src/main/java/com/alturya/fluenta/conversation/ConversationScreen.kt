package com.alturya.fluenta.conversation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alturya.fluenta.R
import com.alturya.fluenta.audio.TtsPlayer
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.util.isRtl
import com.alturya.fluenta.util.langName
import kotlinx.coroutines.launch

// ── Tokens del kit Claude Design (locales para no tocar el theme compartido) ──
private val DcInk = Color(0xFF0F2E27)        // texto principal sobre claro
private val DcSlate = Color(0xFF5B7268)      // texto secundario
private val DcSurface = Color(0xFFF1FAF6)    // superficie mint clara (fondo pantalla)
private val DcMint = Color(0xFFCDEEE6)       // mint pill / acento
private val DcMintDeep = Color(0xFF06463A)   // texto sobre mint
private val DcAmber = Color(0xFFE08A00)      // transliteración (pinyin/romaji)
private val DcCoral = Color(0xFFE8554B)      // corrección "Mejor: …"
private val DcCoralBg = Color(0xFFFFE1DC)    // fondo de la corrección
private val DcOnline = Color(0xFF10B981)     // punto "en línea"

// Pantalla del wedge: conversación de voz abierta. El usuario habla (STT del
// dispositivo), el partner IA responde por voz (TTS), con correcciones suaves.
// Diseño FIEL a [9] ConversationScreen del kit de Claude Design.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    scenario: String? = null,
    goal: String? = null,
    lessonId: String? = null,
    guest: Boolean = false,
    l1: String = "es",
    l2: String = "en",
    onDone: () -> Unit,
    previewState: ConvoUiState? = null,   // solo para screenshots/preview
) {
    val vm: ConversationViewModel = viewModel()
    val vmState by vm.state.collectAsState()
    val state = previewState ?: vmState
    val context = LocalContext.current

    var hasMic by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasMic = granted
    }

    LaunchedEffect(Unit) {
        if (previewState != null) return@LaunchedEffect
        if (!hasMic) permLauncher.launch(Manifest.permission.RECORD_AUDIO)
        vm.start(scenario, goal, lessonId, guest, l1, l2)
    }

    Surface(color = DcSurface, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {

            // ── Cabecera: salir + mascota Hoot + estado en línea ──
            ConversationHeader(l2 = l2, onExit = { vm.end(); onDone() })
            HorizontalDivider(color = DcMint.copy(alpha = 0.6f))

            val listState = androidx.compose.foundation.lazy.rememberLazyListState()
            LaunchedEffect(state.messages.size) {
                if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
            }

            when {
                // ── CARGANDO ──
                state.phase == ConvoPhase.CONNECTING && state.messages.isEmpty() ->
                    ConnectingState(Modifier.weight(1f))

                // ── ERROR (sin nada que mostrar) ──
                state.phase == ConvoPhase.ERROR && state.messages.isEmpty() ->
                    ErrorState(
                        message = state.error ?: I18nStore.t("convo.error.start", "No se pudo iniciar la conversación. Revisa tu conexión."),
                        onRetry = { vm.retry() },
                        modifier = Modifier.weight(1f),
                    )

                // ── VACÍO: tu turno y aún sin mensajes ──
                state.messages.isEmpty() && state.phase != ConvoPhase.ENDED ->
                    YourTurnEmptyState(l2 = l2, suggestion = state.suggestion, modifier = Modifier.weight(1f))

                // ── NORMAL: transcripción ──
                else ->
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 16.dp),
                    ) {
                        val l2IsRtl = isRtl(l2)
                        items(state.messages) { msg -> MessageBubble(msg, isRtlL2 = l2IsRtl) }
                        if (state.partial.isNotBlank()) {
                            item { MessageBubble(ConvoMessage(fromPartner = false, text = state.partial), isRtlL2 = isRtl(l2)) }
                        }
                    }
            }

            // ── Pie: resumen final, o controles de micro ──
            if (state.phase == ConvoPhase.ENDED) {
                EndedSummary(guest = guest, state = state, onDone = onDone)
            } else if (!(state.messages.isEmpty() &&
                        (state.phase == ConvoPhase.CONNECTING || state.phase == ConvoPhase.ERROR))) {
                MicControls(
                    state = state,
                    l2 = l2,
                    hasMic = hasMic,
                    onMic = { listening -> if (listening) vm.stopListening() else vm.startListening() },
                    onGrantMic = { permLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                    onEnd = { vm.end() },
                    onRetry = { vm.retry() },
                )
            }
        }
    }
}

@Composable
private fun ConversationHeader(l2: String, onExit: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onExit, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.Close, contentDescription = I18nStore.t("convo.exit", "Salir"), tint = DcSlate)
        }
        Image(
            painter = painterResource(R.drawable.ic_fluenta_hola),
            contentDescription = null,
            modifier = Modifier.size(38.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                I18nStore.t("convo.tutor", "Tutor IA"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = DcInk,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(DcOnline))
                Spacer(Modifier.width(6.dp))
                Text(
                    I18nStore.t("convo.online", "En línea · {lang}").replace("{lang}", langName(l2)),
                    style = MaterialTheme.typography.labelMedium,
                    color = DcOnline,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun ConnectingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_fluenta_saluda),
            contentDescription = null,
            modifier = Modifier.size(112.dp),
        )
        Spacer(Modifier.height(22.dp))
        BouncingDots()
        Spacer(Modifier.height(18.dp))
        Text(
            I18nStore.t("convo.preparing", "Preparando tu conversación…"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = DcInk,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            I18nStore.t("convo.preparingHint", "Tu tutor de IA ya casi está listo para hablar contigo."),
            style = MaterialTheme.typography.bodyMedium,
            color = DcSlate,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BouncingDots() {
    val transition = rememberInfiniteTransition(label = "dots")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) { i ->
            val a by transition.animateFloat(
                initialValue = 0.4f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(600, delayMillis = i * 160), RepeatMode.Reverse),
                label = "dot$i",
            )
            Box(
                Modifier.size(11.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = a)),
            )
        }
    }
}

@Composable
private fun YourTurnEmptyState(l2: String, suggestion: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(36.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(84.dp).clip(CircleShape).background(DcMint),
            contentAlignment = Alignment.Center,
        ) { Text("👋", style = MaterialTheme.typography.displaySmall) }
        Spacer(Modifier.height(16.dp))
        Text(
            I18nStore.t("convo.yourTurnTitle", "Es tu turno"),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = DcInk,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            I18nStore.t("convo.yourTurnHint", "Saluda a tu tutor para empezar. Toca el micrófono y di algo en ${langName(l2)}."),
            style = MaterialTheme.typography.bodyMedium,
            color = DcSlate,
            textAlign = TextAlign.Center,
        )
        if (suggestion.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            SuggestionPill(suggestion)
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_fluenta_saluda),
            contentDescription = null,
            modifier = Modifier.size(104.dp),
        )
        Spacer(Modifier.height(20.dp))
        Text(
            I18nStore.t("convo.errorTitle", "Tu tutor se quedó sin conexión"),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = DcInk,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = DcSlate,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        com.alturya.fluenta.ui.FluentaButton(
            text = I18nStore.t("common.retry", "Reintentar"),
            onClick = onRetry,
        )
    }
}

@Composable
private fun SuggestionPill(suggestion: String) {
    Surface(color = DcMint, shape = RoundedCornerShape(99.dp)) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = DcMintDeep, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                I18nStore.t("convo.youCanSay", "Puedes decir: \"{s}\"").replace("{s}", suggestion),
                style = MaterialTheme.typography.labelLarge,
                color = DcMintDeep,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun MicControls(
    state: ConvoUiState,
    l2: String,
    hasMic: Boolean,
    onMic: (listening: Boolean) -> Unit,
    onGrantMic: () -> Unit,
    onEnd: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Sugerencia de andamiaje para que el principiante no se congele.
        if (state.suggestion.isNotBlank() &&
            (state.phase == ConvoPhase.YOUR_TURN || state.phase == ConvoPhase.SPEAKING)) {
            SuggestionPill(state.suggestion)
            state.suggestionTranslit.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, style = MaterialTheme.typography.labelSmall, color = DcAmber)
            }
            Spacer(Modifier.height(12.dp))
        }

        val statusText = when (state.phase) {
            ConvoPhase.CONNECTING -> I18nStore.t("convo.connecting", "Conectando…")
            ConvoPhase.SPEAKING -> I18nStore.t("convo.partnerSpeaking", "Escuchando a tu compañero…")
            ConvoPhase.LISTENING -> I18nStore.t("convo.listening", "Te escucho… habla en ${langName(l2)}")
            ConvoPhase.THINKING -> "…"
            ConvoPhase.YOUR_TURN -> I18nStore.t("convo.tapToSpeak", "Toca el micrófono y habla")
            ConvoPhase.ERROR -> state.error ?: I18nStore.t("common.errorGeneric", "Algo salió mal")
            ConvoPhase.ENDED -> ""
        }

        if (state.phase == ConvoPhase.ERROR) {
            Text(statusText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            com.alturya.fluenta.ui.FluentaButton(text = I18nStore.t("common.retry", "Reintentar"), onClick = onRetry)
            return@Column
        }

        val canTalk = hasMic && (state.phase == ConvoPhase.YOUR_TURN || state.phase == ConvoPhase.LISTENING)
        val listening = state.phase == ConvoPhase.LISTENING
        MicButton(enabled = canTalk, listening = listening, onClick = { onMic(listening) })
        Spacer(Modifier.height(8.dp))
        Text(statusText, style = MaterialTheme.typography.bodyMedium, color = DcSlate, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)

        if (!hasMic) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onGrantMic, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(I18nStore.t("convo.grantMic", "Permitir micrófono para hablar"))
            }
        }
        if (state.messages.size > 2 && state.phase == ConvoPhase.YOUR_TURN) {
            TextButton(onClick = onEnd) { Text(I18nStore.t("convo.end", "Terminar conversación")) }
        }
    }
}

@Composable
private fun MicButton(enabled: Boolean, listening: Boolean, onClick: () -> Unit) {
    // Anillo de pulso cuando está escuchando (eco del diseño 'pulsering').
    val transition = rememberInfiniteTransition(label = "mic")
    val pulse by transition.animateFloat(
        initialValue = 1f, targetValue = if (listening) 1.12f else 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "micPulse",
    )
    val face = when {
        !enabled -> MaterialTheme.colorScheme.surfaceVariant
        listening -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    val relief = Color(0xFF0A6F64)   // relieve 3D del kit
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(96.dp)) {
        // Base 3D (relieve)
        Box(
            Modifier.size(76.dp).scale(pulse).offset(y = 6.dp).clip(CircleShape)
                .background(if (listening) Color(0xFFC13B32) else relief),
        )
        // Cara
        Box(
            Modifier.size(76.dp).scale(pulse).clip(CircleShape).background(face),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = if (listening) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = I18nStore.t("convo.mic", "Hablar"),
                    tint = Color.White,
                    modifier = Modifier.size(38.dp),
                )
            }
        }
    }
}

@Composable
private fun EndedSummary(guest: Boolean, state: ConvoUiState, onDone: () -> Unit) {
    val gradient = Brush.verticalGradient(
        listOf(MaterialTheme.colorScheme.primary, DcMint, DcSurface),
    )
    Surface(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(24.dp)) {
        Column(
            Modifier.fillMaxWidth().background(gradient).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_fluenta_celebra),
                contentDescription = null,
                modifier = Modifier.size(96.dp),
            )
            Spacer(Modifier.height(12.dp))
            if (guest) {
                Text(
                    I18nStore.t("convo.justSpoke", "¡Acabas de hablar en otro idioma! 🎉"),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = DcMintDeep,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    I18nStore.plural(
                        "convo.guestSummary", state.spokenPhrases,
                        one = "Dijiste {n} frase. Crea tu cuenta gratis para guardar tu racha y seguir practicando.",
                        other = "Dijiste {n} frases. Crea tu cuenta gratis para guardar tu racha y seguir practicando.",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = DcMintDeep,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                com.alturya.fluenta.ui.FluentaButton(
                    text = I18nStore.t("convo.createAccount", "Crear cuenta gratis"),
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Text(
                    I18nStore.t("convo.complete", "¡Conversación completa!"),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = DcMintDeep,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    ConvoStat("${state.spokenPhrases}", I18nStore.t("convo.turns", "turnos"))
                    ConvoStat("+${state.xpEarned}", "XP")
                }
                Spacer(Modifier.height(16.dp))
                com.alturya.fluenta.ui.FluentaButton(
                    text = I18nStore.t("common.done", "Listo"),
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(14.dp))
                // Feedback de 1 toque del wedge (activación = fuga #1) — solo al
                // terminar, sin interrumpir. Handoff "Fluenta Feedback".
                com.alturya.fluenta.ui.FeedbackBar(screen = "conversation")
            }
        }
    }
}

@Composable
private fun ConvoStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = DcMintDeep)
        Text(label, style = MaterialTheme.typography.labelMedium, color = DcMintDeep, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MessageBubble(msg: ConvoMessage, isRtlL2: Boolean = false) {
    val align = if (msg.fromPartner) Alignment.Start else Alignment.End
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxWidth(), horizontalAlignment = align) {
        val bubbleShape = if (msg.fromPartner)
            RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 18.dp)
        else
            RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomEnd = 18.dp, bottomStart = 18.dp)
        Surface(
            color = if (msg.fromPartner) Color.White else MaterialTheme.colorScheme.primary,
            shape = bubbleShape,
            shadowElevation = if (msg.fromPartner) 2.dp else 0.dp,
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            val textDir = if (isRtlL2 && msg.fromPartner) TextDirection.Rtl else TextDirection.ContentOrLtr
            Row(
                Modifier.padding(start = 14.dp, end = if (msg.fromPartner) 4.dp else 14.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f, fill = false)) {
                    // L2 (idioma que se aprende)
                    Text(
                        msg.text,
                        color = if (msg.fromPartner) DcInk else MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleMedium.copy(textDirection = textDir),
                        fontWeight = FontWeight.Bold,
                    )
                    // Lectura latina (pinyin/romaji) debajo
                    if (!msg.translit.isNullOrBlank()) {
                        Text(
                            msg.translit!!,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (msg.fromPartner) DcAmber else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                        )
                    }
                }
                if (msg.fromPartner) {
                    IconButton(
                        onClick = { scope.launch { TtsPlayer.play(context, msg.text, com.alturya.fluenta.data.Session.l2 ?: "en") } },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = I18nStore.t("convo.hear", "Escuchar"),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
        // Corrección suave en CORAL — "Mejor: …"
        if (!msg.correction.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Surface(color = DcCoralBg, shape = RoundedCornerShape(14.dp), modifier = Modifier.widthIn(max = 320.dp)) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = DcCoral, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            I18nStore.t("convo.better", "Mejor: {s}").replace("{s}", msg.correction),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = DcCoral,
                        )
                    }
                    if (!msg.tip.isNullOrBlank()) {
                        Text(
                            msg.tip!!,
                            style = MaterialTheme.typography.labelMedium,
                            color = DcCoral.copy(alpha = 0.85f),
                        )
                    }
                }
            }
        }
        // Micro-enseñanza del script "a la par" (un carácter).
        if (!msg.scriptTip.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Surface(color = DcMint, shape = RoundedCornerShape(10.dp)) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = DcMintDeep, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(msg.scriptTip!!, style = MaterialTheme.typography.labelMedium, color = DcMintDeep)
                }
            }
        }
    }
}
