package com.alturya.fluenta.script

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alturya.fluenta.data.I18nStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Repaso espaciado de caracteres (T3). Consume la cola local de [HanziSrsStore]:
// recall activo (pista = romanización/significado) → trazo a trazo con StrokeWriter
// → autoevaluación que reprograma la carta. La cola se congela al entrar para que
// calificar no reordene la pantalla bajo los pies del usuario.
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
    var reviewed by remember { mutableStateOf(0) }

    val q = queue
    if (q == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val total = q.size
    val current = q.getOrNull(index)

    if (current == null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(80.dp))
            Spacer(Modifier.height(12.dp))
            Text(
                if (total == 0) I18nStore.t("hanzi.review.empty", "Nada que repasar hoy. ¡Vas al día!")
                else I18nStore.t("hanzi.review.done", "¡Repaso de caracteres completado!"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            if (total > 0) {
                Spacer(Modifier.height(8.dp))
                Text("$reviewed / $total", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(24.dp))
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(I18nStore.t("common.back", "Volver"))
            }
        }
        return
    }

    fun grade(remembered: Boolean) {
        val glyph = current.glyph
        if (previewQueue == null) {
            scope.launch { HanziSrsStore.grade(context, l2, glyph, remembered, now) }
        }
        reviewed += 1
        index += 1
        revealed = false
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("${I18nStore.t("hanzi.review.title", "Repaso de caracteres")} · ${index + 1}/$total", style = MaterialTheme.typography.labelMedium)
        LinearProgressIndicator(progress = { index.toFloat() / total.toFloat() }, modifier = Modifier.fillMaxWidth().height(8.dp))

        // Pista de recall: romanización + significado, SIN mostrar aún el glifo.
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(20.dp)) {
                Text(I18nStore.t("hanzi.review.prompt", "¿Qué carácter es?"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                if (current.romanization.isNotBlank()) {
                    Text(current.romanization, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                if (current.meaning.isNotBlank()) {
                    Text(current.meaning, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (revealed) {
            // Trazo a trazo del carácter correcto (orden + radicales).
            StrokeWriter(glyph = current.glyph)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                com.alturya.fluenta.ui.FluentaButton(
                    text = I18nStore.t("repaso.failed", "Fallé"),
                    onClick = { grade(false) },
                    style = com.alturya.fluenta.ui.FluentaButtonStyle.Neutral,
                    modifier = Modifier.weight(1f),
                )
                com.alturya.fluenta.ui.FluentaButton(
                    text = I18nStore.t("repaso.remembered", "Lo recordé"),
                    onClick = { grade(true) },
                    style = com.alturya.fluenta.ui.FluentaButtonStyle.Success,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            com.alturya.fluenta.ui.FluentaButton(
                text = I18nStore.t("hanzi.review.reveal", "Ver carácter y practicar"),
                onClick = { revealed = true },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
