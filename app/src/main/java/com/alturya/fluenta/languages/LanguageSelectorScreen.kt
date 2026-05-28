package com.alturya.fluenta.languages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.network.LanguagePair
import com.alturya.fluenta.util.flag
import com.alturya.fluenta.util.langName
import com.alturya.fluenta.util.levelSystemName

/**
 * 30 pares MVP world-ready (sección 6 de FICHA_TECNICA_FLUENTA.md).
 * Aparecen primero, destacados, con el resto del catálogo abajo en "más idiomas".
 */
private val PRIORITY_PAIRS: Set<Pair<String, String>> = setOf(
    "es" to "en", "pt" to "en", "en" to "es",
    "en" to "fr", "en" to "de", "en" to "it", "en" to "pt",
    "en" to "ja", "en" to "ko", "en" to "zh",
    "en" to "ar", "en" to "ru", "en" to "pl", "en" to "sv", "en" to "nl", "en" to "tr",
    "es" to "fr", "es" to "it", "es" to "pt", "es" to "de",
    "fr" to "en", "de" to "en", "it" to "en",
    "ar" to "en", "hi" to "en", "zh" to "en", "ja" to "en", "ko" to "en",
    "ru" to "en", "tr" to "en",
    // Árabe en ambas direcciones (desde y hacia idiomas mayores)
    "ar" to "es", "ar" to "fr", "ar" to "de", "ar" to "it", "ar" to "pt",
    "es" to "ar", "fr" to "ar", "de" to "ar", "it" to "ar", "pt" to "ar",
)

@Composable
fun LanguageSelectorScreen(onChanged: () -> Unit = {}) {
    val vm: LanguagesViewModel = viewModel()
    val state by vm.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it) }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            Text(
                I18nStore.t("lang.chooseTitle", "Elige tu idioma"),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(20.dp)
            )

            if (state.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                // Priority MVP pairs first (sección 6 de FICHA_TECNICA_FLUENTA.md),
                // luego "more languages" con el resto del catálogo.
                val (priority, rest) = state.pairs.partition {
                    (it.l1 to it.l2) in PRIORITY_PAIRS
                }
                val priorityByL1 = priority.groupBy { it.l1 }
                val restByL1 = rest.groupBy { it.l1 }

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (priority.isNotEmpty()) {
                        item {
                            Text(
                                I18nStore.t("lang.recommended", "Recomendados para ti"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                            )
                        }
                        priorityByL1.forEach { (l1, pairs) ->
                            item {
                                Text(
                                    "${I18nStore.t("lang.from", "Desde")} ${langName(l1)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                )
                            }
                            items(pairs) { pair -> PairRow(pair, state.selecting == pair.l2) { vm.select(pair.l2, onChanged) } }
                        }
                    }
                    if (rest.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider()
                            Text(
                                I18nStore.t("lang.moreLanguages", "Más idiomas"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                            )
                        }
                        restByL1.forEach { (l1, pairs) ->
                            item {
                                Text(
                                    "${I18nStore.t("lang.from", "Desde")} ${langName(l1)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                )
                            }
                            items(pairs) { pair -> PairRow(pair, state.selecting == pair.l2) { vm.select(pair.l2, onChanged) } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PairRow(pair: LanguagePair, selecting: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(flag(pair.l2), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(langName(pair.l2), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Text(
                    buildString {
                        append(levelSystemName(pair.levelSystem))
                        if (pair.curriculumSeeded == true) append(" · ${I18nStore.t("lang.curriculumReady", "con currículo")}")
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (selecting) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        }
    }
}
