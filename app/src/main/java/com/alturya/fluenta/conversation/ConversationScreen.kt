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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel

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
                title = { Text("Conversación") },
                navigationIcon = {
                    TextButton(onClick = {
                        vm.end()
                        onDone()
                    }) { Text("Salir") }
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
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
            ) {
                items(state.messages) { msg ->
                    MessageBubble(msg)
                }
                if (state.partial.isNotBlank()) {
                    item {
                        MessageBubble(ConvoMessage(fromPartner = false, text = state.partial))
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
                            Text("¡Acabas de hablar en inglés! 🎉", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Dijiste ${state.spokenPhrases} ${if (state.spokenPhrases == 1) "frase" else "frases"}. Crea tu cuenta gratis para guardar tu racha y seguir practicando.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                            Spacer(Modifier.height(14.dp))
                            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                                Text("Crear cuenta gratis")
                            }
                        } else {
                            Text("¡Conversación completa! 🎉", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Text("Hablaste ${state.spokenPhrases} veces · +${state.xpEarned} XP", style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Listo") }
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
                        ConvoPhase.CONNECTING -> "Conectando…"
                        ConvoPhase.SPEAKING -> "🔊 Escuchando a tu compañero…"
                        ConvoPhase.LISTENING -> "🎙️ Te escucho… habla en inglés"
                        ConvoPhase.THINKING -> "…"
                        ConvoPhase.YOUR_TURN -> "Tu turno — toca el micro y habla"
                        ConvoPhase.ERROR -> state.error ?: "Algo salió mal"
                        ConvoPhase.ENDED -> ""
                    }
                    Text(statusText, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    state.error?.takeIf { state.phase != ConvoPhase.ERROR }?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    if (state.phase == ConvoPhase.ERROR) {
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { vm.retry() }) { Text("Reintentar") }
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
                                Text(
                                    "💡 Puedes decir: \"${state.suggestion}\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
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
                            .size(84.dp)
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
                        ) {
                            Text(if (listening) "⏹" else "🎤", style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (!hasMic) {
                        Text("Necesitas dar permiso de micrófono para hablar.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                    if (state.messages.size > 2 && state.phase == ConvoPhase.YOUR_TURN) {
                        TextButton(onClick = { vm.end() }) { Text("Terminar conversación") }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: ConvoMessage) {
    val align = if (msg.fromPartner) Alignment.Start else Alignment.End
    Column(Modifier.fillMaxWidth(), horizontalAlignment = align) {
        Surface(
            color = if (msg.fromPartner) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(
                msg.text,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                color = if (msg.fromPartner) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary,
            )
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
            Surface(color = Color(0xFFFFF4E5), shape = RoundedCornerShape(10.dp)) {
                Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text(
                        "✏️ Mejor: ${msg.correction}",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = Color(0xFF8A5A00),
                    )
                    if (!msg.tip.isNullOrBlank()) {
                        Text(
                            msg.tip!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFB07400),
                        )
                    }
                }
            }
        }
        // Micro-enseñanza del script "a la par" (un carácter), cuando el alumno
        // aún no lee bien. Se desvanece sola al subir el dominio (backend).
        if (!msg.scriptTip.isNullOrBlank()) {
            Spacer(Modifier.height(3.dp))
            Surface(color = Color(0xFFEDEBFF), shape = RoundedCornerShape(10.dp)) {
                Text(
                    "📖 ${msg.scriptTip}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF4030A0),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}
