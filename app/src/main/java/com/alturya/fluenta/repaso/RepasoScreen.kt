package com.alturya.fluenta.repaso

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.alturya.fluenta.data.Session
import com.alturya.fluenta.util.isRtl
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.network.ApiClient
import com.alturya.fluenta.network.ErrorItem
import com.alturya.fluenta.network.ErrorReviewBody
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

@Composable
fun RepasoScreen(onDone: () -> Unit, previewState: RepasoState? = null) {
    val vm: RepasoViewModel = viewModel()
    val vmState by vm.state.collectAsState()
    val state = previewState ?: vmState

    if (state.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val total = state.queue.size
    val current = state.queue.getOrNull(state.index)

    if (current == null) {
        // Nada pendiente, o repaso terminado.
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = if (total == 0) Icons.Default.EmojiEvents else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(80.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                if (total == 0) I18nStore.t("repaso.empty", "Nada que repasar hoy. ¡Vas al día!")
                else I18nStore.t("repaso.done", "¡Repaso completado!"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            if (total > 0) {
                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${state.reviewedCount} / $total",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            I18nStore.t("repaso.reviewedOf", "errores repasados"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        val xpEarned = state.reviewedCount * 5
                        if (xpEarned > 0) {
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFEAB308), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "+$xpEarned XP",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
            if (total == 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    I18nStore.t("repaso.emptyHint", "Sigue aprendiendo cosas nuevas y volverán aquí para no olvidarlas."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(24.dp))
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(
                    if (total == 0) I18nStore.t("repaso.goLesson", "Ir a mi lección de hoy →")
                    else I18nStore.t("common.back", "Volver")
                )
            }
        }
        return
    }

    val l2TextDir = if (isRtl(Session.l2)) TextDirection.Rtl else TextDirection.ContentOrLtr
    Box(Modifier.fillMaxSize().padding(20.dp)) {
        // ── Content scrollable (top) ───────────────────────────────────────
        Column(Modifier.fillMaxWidth()) {
            Text(
                "${I18nStore.t("repaso.title", "Repaso")} · ${state.index + 1}/$total",
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { state.index.toFloat() / total.toFloat() },
                modifier = Modifier.fillMaxWidth().height(8.dp),
            )
            Spacer(Modifier.height(20.dp))
            if (!state.revealed) {
                Text(
                    I18nStore.t("repaso.thinkFirst", "¿Recuerdas la forma correcta? Piénsala antes de revelar."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
            }

            // Contexto: el error previo del usuario
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(Modifier.padding(20.dp)) {
                    // F4.3 — source badge: shows if error came from WhatsApp or lesson
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        current.errorCategory?.let {
                            Text(it.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(8.dp))
                        }
                        val isWa = current.source == "wa"
                        Surface(
                            color = if (isWa) Color(0xFF25D366).copy(alpha = 0.18f)
                            else MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.extraSmall,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Icon(
                                    imageVector = if (isWa) Icons.AutoMirrored.Filled.Chat else Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = if (isWa) Color(0xFF1A9E50) else MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(12.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    if (isWa) "WhatsApp" else "App",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isWa) Color(0xFF1A9E50) else MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        I18nStore.t("repaso.yourMistake", "Tu error anterior:"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        current.original ?: "—",
                        style = MaterialTheme.typography.titleLarge.copy(textDirection = l2TextDir),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            if (state.revealed) {
                Spacer(Modifier.height(12.dp))
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            I18nStore.t("repaso.correct", "Correcto:"),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            current.corrected ?: "—",
                            style = MaterialTheme.typography.headlineSmall.copy(textDirection = l2TextDir),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }

        // ── Buttons pinned to bottom ───────────────────────────────────────
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
        if (!state.revealed) {
            com.alturya.fluenta.ui.FluentaButton(
                text = I18nStore.t("repaso.reveal", "Ver corrección"),
                onClick = vm::reveal,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                com.alturya.fluenta.ui.FluentaButton(
                    text = I18nStore.t("repaso.failed", "Fallé"),
                    onClick = { vm.answer(false) },
                    style = com.alturya.fluenta.ui.FluentaButtonStyle.Neutral,
                    modifier = Modifier.weight(1f),
                )
                com.alturya.fluenta.ui.FluentaButton(
                    text = I18nStore.t("repaso.remembered", "Lo recordé"),
                    onClick = { vm.answer(true) },
                    style = com.alturya.fluenta.ui.FluentaButtonStyle.Success,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        }  // Column bottom buttons
    }  // Box
}
