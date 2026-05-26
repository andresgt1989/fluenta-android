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
import com.alturya.fluenta.network.LanguagePair
import com.alturya.fluenta.util.flag
import com.alturya.fluenta.util.langName
import com.alturya.fluenta.util.levelSystemName

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
                "Elige tu idioma",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(20.dp)
            )

            if (state.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                val byL1 = state.pairs.groupBy { it.l1 }
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    byL1.forEach { (l1, pairs) ->
                        item {
                            Text(
                                "Desde ${langName(l1)}",
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
                        if (pair.curriculumSeeded == true) append(" · con currículo")
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (selecting) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        }
    }
}
