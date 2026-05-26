package com.alturya.fluenta.pronunciation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

private val SUGGESTED = listOf(
    "Nice to meet you" to "Encantado de conocerte",
    "Could you repeat that, please?" to "¿Podrías repetir, por favor?",
    "I would like to schedule a meeting" to "Me gustaría agendar una reunión",
    "Thank you for your help" to "Gracias por tu ayuda",
    "I think the same way" to "Pienso de la misma forma"
)

@Composable
fun PronunciationScreen() {
    val context = LocalContext.current
    val vm: PronunciationViewModel = viewModel()
    val state by vm.state.collectAsState()
    var custom by remember { mutableStateOf("") }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.error) { state.error?.let { snackbar.showSnackbar(it) } }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Pronunciación", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Escucha la pronunciación nativa. Escribe cualquier frase o usa las sugeridas.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            item {
                OutlinedTextField(
                    value = custom,
                    onValueChange = { custom = it },
                    label = { Text("Escribe una frase para escuchar") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { vm.listen(context, custom) }) {
                            if (state.playing == custom && custom.isNotBlank())
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            else Text("▶")
                        }
                    }
                )
            }
            item {
                Text("Frases útiles", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
            }
            items(SUGGESTED) { (en, es) ->
                PhraseCard(en, es, playing = state.playing == en) { vm.listen(context, en) }
            }
        }
    }
}

@Composable
private fun PhraseCard(en: String, es: String, playing: Boolean, onPlay: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(en, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Text(es, style = MaterialTheme.typography.bodySmall)
            }
            FilledIconButton(onClick = onPlay) {
                if (playing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("▶")
            }
        }
    }
}
