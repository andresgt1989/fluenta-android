package com.alturya.fluenta.script

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alturya.fluenta.data.I18nStore

// Pantalla DEDICADA de escritura de hanzi, a pantalla completa.
//
// Por qué existe: antes el dibujo vivía incrustado en una lista que scrolleaba, y el
// WebView del trazo secuestraba el gesto vertical → no se podía scrollear ni dibujar
// bien. Aquí el lienzo ocupa toda la pantalla (sin scroll alrededor) y se navega entre
// caracteres con ‹ ›. Reusa ScriptViewModel para no duplicar la carga de la lección.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HanziWriterScreen(l2: String, startGlyph: String? = null, onDone: () -> Unit = {}) {
    val vm: ScriptViewModel = viewModel()
    val state by vm.state.collectAsState()

    LaunchedEffect(l2) { vm.load(l2) }

    val items = state.lesson?.items.orEmpty()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(I18nStore.t("hanzi.writer.title", "Escribir trazos")) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Default.Close, contentDescription = I18nStore.t("common.close", "Cerrar"))
                    }
                },
            )
        },
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            when {
                state.phase == ScriptPhase.LOADING ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))

                items.isEmpty() ->
                    Text(
                        I18nStore.t("hanzi.writer.empty", "No hay caracteres para practicar todavía."),
                        Modifier.align(Alignment.Center).padding(24.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                else -> {
                    var index by remember(items) {
                        mutableStateOf(items.indexOfFirst { it.glyph == startGlyph }.coerceAtLeast(0))
                    }
                    val item = items[index]

                    Column(
                        Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Pista de lectura: romanización + significado (sin idioma puente).
                        Spacer(Modifier.height(8.dp))
                        Text(item.romanization, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        item.meaning?.takeIf { it.isNotBlank() }?.let {
                            Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        // Lienzo de trazos: ocupa el espacio libre, sin scroll alrededor.
                        // key(glyph) fuerza recrear el WebView al cambiar de carácter.
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            key(item.glyph) { StrokeWriter(glyph = item.glyph) }
                        }

                        // Navegación entre caracteres.
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FilledTonalButton(onClick = { if (index > 0) index-- }, enabled = index > 0) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(I18nStore.t("common.prev", "Anterior"), maxLines = 1)
                            }
                            Text("${index + 1} / ${items.size}", style = MaterialTheme.typography.labelLarge)
                            FilledTonalButton(onClick = { if (index < items.lastIndex) index++ }, enabled = index < items.lastIndex) {
                                Text(I18nStore.t("common.next", "Siguiente"), maxLines = 1)
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // Feedback colapsado (handoff "Fluenta Feedback" · estado 5: pantalla de
            // FOCO). Pill discreto en esquina que no tapa el trazado; respeta el tope 72h.
            if (items.isNotEmpty() && state.phase != ScriptPhase.LOADING) {
                com.alturya.fluenta.ui.FeedbackPill(
                    screen = "hanzi_writer",
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                )
            }
        }
    }
}
