package com.alturya.fluenta.conversation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import com.alturya.fluenta.audio.TtsPlayer
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.alturya.fluenta.util.isRtl
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.util.langName

// Pantalla del wedge: conversación de voz abierta. El usuario habla (STT del
// dispositivo), el partner IA responde por voz (TTS), con correcciones suaves.
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(I18nStore.t("convo.title", "Conversación")) },
                navigationIcon = {
                    TextButton(onClick = {
                        vm.end()
                        onDone()
                    }) { Text(I18nStore.t("convo.exit", "Salir")) }
                },
            )
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {

            // Transcript
            val listState = androidx.compose.foundation.lazy.rememberLazyListState()
            LaunchedEffect(state.messages.size) {
                if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
            }
            if (state.phase == ConvoPhase.CONNECTING && state.messages.isEmpty()) {
                // Estado de carga con personalidad (no pantalla en blanco — bug visto en Test Lab).
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(com.alturya.fluenta.R.drawable.ic_fluenta_saluda),
                        contentDescription = null,
                        modifier = Modifier.size(96.dp),
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        I18nStore.t("convo.preparing", "Preparando tu conversación…"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        I18nStore.t("convo.preparingHint", "Tu tutor de IA ya casi está listo para hablar contigo."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    Spacer(Modifier.height(24.dp))
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
            ) {
                val l2IsRtl = isRtl(l2)
                items(state.messages) { msg ->
                    MessageBubble(msg, isRtlL2 = l2IsRtl)
                }
                if (state.partial.isNotBlank()) {
                    item {
                        MessageBubble(ConvoMessage(fromPartner = false, text = state.partial))
                    }
                }
            }
            }

            // Ended summary
            if (state.phase == ConvoPhase.ENDED) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (guest) {
                            // Momento "aha": acaba de hablar inglés. PICO DE VALOR → conversión.
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(I18nStore.t("convo.justSpoke", "¡Acabas de hablar en inglés!"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                I18nStore.t("convo.guestSummary", "Dijiste {n} {plural}. Crea tu cuenta gratis para guardar tu racha y seguir practicando.")
                                    .replace("{n}", "${state.spokenPhrases}")
                                    .replace("{plural}", if (state.spokenPhrases == 1) I18nStore.t("convo.phraseSingular", "frase") else I18nStore.t("convo.phrasePlural", "frases")),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                            Spacer(Modifier.height(14.dp))
                            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                                Text(I18nStore.t("convo.createAccount", "Crear cuenta gratis"))
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(I18nStore.t("convo.complete", "¡Conversación completa!"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(I18nStore.t("convo.spokeSummary", "Hablaste {n} veces · +{xp} XP").replace("{n}", "${state.spokenPhrases}").replace("{xp}", "${state.xpEarned}"), style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text(I18nStore.t("common.done", "Listo")) }
                        }
                    }
                }
            } else {
                // Status + mic control
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val statusText = when (state.phase) {
                        ConvoPhase.CONNECTING -> I18nStore.t("convo.connecting", "Conectando…")
                        ConvoPhase.SPEAKING -> I18nStore.t("convo.partnerSpeaking", "Escuchando a tu compañero…")
                        ConvoPhase.LISTENING -> I18nStore.t("convo.listening", "Te escucho… habla en ${langName(l2)}")
                        ConvoPhase.THINKING -> "…"
                        ConvoPhase.YOUR_TURN -> I18nStore.t("convo.yourTurn", "Tu turno — toca el micro y habla")
                        ConvoPhase.ERROR -> state.error ?: I18nStore.t("common.errorGeneric", "Algo salió mal")
                        ConvoPhase.ENDED -> ""
                    }
                    Text(statusText, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    state.error?.takeIf { state.phase != ConvoPhase.ERROR }?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    if (state.phase == ConvoPhase.ERROR) {
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { vm.retry() }) { Text(I18nStore.t("common.retry", "Reintentar")) }
                    }
                    // Scaffolding: frase sugerida para que el principiante no se congele.
                    if (state.suggestion.isNotBlank() &&
                        (state.phase == ConvoPhase.YOUR_TURN || state.phase == ConvoPhase.SPEAKING)) {
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(I18nStore.t("convo.youCanSay", "Puedes decir: \"{s}\"").replace("{s}", state.suggestion), style = MaterialTheme.typography.bodyMedium)
                                }
                                if (state.suggestionTranslit.isNotBlank()) {
                                    Text(
                                        state.suggestionTranslit,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))

                    val canTalk = hasMic && (state.phase == ConvoPhase.YOUR_TURN || state.phase == ConvoPhase.LISTENING)
                    val listening = state.phase == ConvoPhase.LISTENING
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .shadow(elevation = 8.dp, shape = CircleShape)
                            .clip(CircleShape)
                            .background(
                                when {
                                    !canTalk -> MaterialTheme.colorScheme.surfaceVariant
                                    listening -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.primary
                                }
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        TextButton(
                            enabled = canTalk,
                            onClick = { if (listening) vm.stopListening() else vm.startListening() },
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Icon(
                                imageVector = if (listening) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(44.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (!hasMic) {
                        OutlinedButton(
                            onClick = { permLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(I18nStore.t("convo.grantMic", "Permitir micrófono para hablar"))
                        }
                    }
                    if (state.messages.size > 2 && state.phase == ConvoPhase.YOUR_TURN) {
                        TextButton(onClick = { vm.end() }) { Text(I18nStore.t("convo.end", "Terminar conversación")) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: ConvoMessage, isRtlL2: Boolean = false) {
    val align = if (msg.fromPartner) Alignment.Start else Alignment.End
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxWidth(), horizontalAlignment = align) {
        Surface(
            color = if (msg.fromPartner) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(16.dp),
        ) {
            // Partner messages in RTL languages (Arabic, Hebrew, Persian, Urdu)
            // need explicit paragraph direction so Bidi renders them correctly.
            val textDir = if (isRtlL2 && msg.fromPartner) TextDirection.Rtl else TextDirection.ContentOrLtr
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    msg.text,
                    modifier = Modifier.padding(start = 14.dp, end = if (msg.fromPartner) 2.dp else 14.dp, top = 10.dp, bottom = 10.dp),
                    color = if (msg.fromPartner) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.bodyMedium.copy(textDirection = textDir),
                )
                // Hear the partner speak the target language — essential for listening practice.
                if (msg.fromPartner) {
                    IconButton(
                        onClick = { scope.launch { TtsPlayer.play(context, msg.text, com.alturya.fluenta.data.Session.l2 ?: "en") } },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = I18nStore.t("convo.hear", "Escuchar"),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
        // Lectura latina (romaji/pinyin/…) para que se pueda leer un script no-latino.
        if (!msg.translit.isNullOrBlank()) {
            Text(
                msg.translit!!,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        if (!msg.correction.isNullOrBlank()) {
            Spacer(Modifier.height(3.dp))
            Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(10.dp)) {
                Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            I18nStore.t("convo.better", "Mejor: {s}").replace("{s}", msg.correction),
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                    if (!msg.tip.isNullOrBlank()) {
                        Text(
                            msg.tip!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                        )
                    }
                }
            }
        }
        // Micro-enseñanza del script "a la par" (un carácter), cuando el alumno
        // aún no lee bien. Se desvanece sola al subir el dominio (backend).
        if (!msg.scriptTip.isNullOrBlank()) {
            Spacer(Modifier.height(3.dp))
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(10.dp)) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(msg.scriptTip!!, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }
    }
}
