package com.alturya.fluenta.repaso

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
                _state.value = RepasoState(loading = false, error = "No se pudo cargar el repaso. Reintenta.")
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
fun RepasoScreen(onDone: () -> Unit) {
    val vm: RepasoViewModel = viewModel()
    val state by vm.state.collectAsState()

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
            Text(if (total == 0) "🎉" else "✅", style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(12.dp))
            Text(
                if (total == 0) I18nStore.t("repaso.empty", "Nada que repasar hoy. ¡Vas al día!")
                else I18nStore.t("repaso.done", "¡Repaso completado!"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            if (total > 0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "${I18nStore.t("repaso.reviewed", "Repasaste")} ${state.reviewedCount}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(24.dp))
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(I18nStore.t("common.back", "Volver"))
            }
        }
        return
    }

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text(
            "${I18nStore.t("repaso.title", "Repaso")} · ${state.index + 1}/$total",
            style = MaterialTheme.typography.labelMedium,
        )
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { state.index.toFloat() / total.toFloat() },
            modifier = Modifier.fillMaxWidth().height(8.dp),
        )
        Spacer(Modifier.height(24.dp))

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
                        Text(
                            if (isWa) "💬 WhatsApp" else "📱 App",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isWa) Color(0xFF1A9E50)
                            else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    I18nStore.t("repaso.yourMistake", "Tu error anterior:"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    current.original ?: "—",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        Spacer(Modifier.weight(1f))

        if (!state.revealed) {
            Button(
                onClick = vm::reveal,
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) { Text(I18nStore.t("repaso.reveal", "Ver corrección")) }
        } else {
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
                    Spacer(Modifier.height(4.dp))
                    Text(
                        current.corrected ?: "—",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { vm.answer(false) },
                    modifier = Modifier.weight(1f).height(52.dp),
                ) { Text(I18nStore.t("repaso.failed", "❌ Fallé")) }
                Button(
                    onClick = { vm.answer(true) },
                    modifier = Modifier.weight(1f).height(52.dp),
                ) { Text(I18nStore.t("repaso.remembered", "✅ Lo recordé")) }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
