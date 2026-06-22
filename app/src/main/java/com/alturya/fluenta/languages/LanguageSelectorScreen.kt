package com.alturya.fluenta.languages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.network.LanguagePair
import com.alturya.fluenta.ui.FluentaButton
import com.alturya.fluenta.ui.ShimmerCard
import com.alturya.fluenta.ui.theme.FluentaTokens
import com.alturya.fluenta.util.autonym
import com.alturya.fluenta.util.flag
import com.alturya.fluenta.util.langName
import com.alturya.fluenta.util.levelSystemName

/**
 * Selector de idioma · kit Claude Design (LanguageSelector.dc.html).
 * Buscador (filtra el catálogo ya cargado — no inventa datos), secciones
 * "Recomendados"/"Más idiomas", patrón bilingüe (autónimo · nombre), badge de
 * currículo, y estados normal/cargando/vacío-búsqueda/error. Cableado intacto.
 *
 * 30 pares MVP world-ready (sección 6 de FICHA_TECNICA_FLUENTA.md) salen primero,
 * destacados, con el resto del catálogo abajo en "Más idiomas".
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
    "ar" to "es", "ar" to "fr", "ar" to "de", "ar" to "it", "ar" to "pt",
    "es" to "ar", "fr" to "ar", "de" to "ar", "it" to "ar", "pt" to "ar",
)

@Composable
fun LanguageSelectorScreen(onChanged: () -> Unit = {}, previewState: LanguagesState? = null) {
    val vm: LanguagesViewModel = viewModel()
    val vmState by vm.state.collectAsState()
    val state = previewState ?: vmState
    val snackbar = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current
    var query by remember { mutableStateOf("") }
    val selectLang: (String) -> Unit = { l2 ->
        com.alturya.fluenta.data.Analytics.track(
            context,
            com.alturya.fluenta.data.Analytics.LANGUAGE_CHANGE,
            mapOf("l2" to l2),
        )
        vm.select(l2, onChanged)
    }

    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = FluentaTokens.Surface,
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)) {
                Text(
                    I18nStore.t("lang.chooseTitle", "Elige un idioma"),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = FluentaTokens.Ink,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(14.dp))
                SearchField(query = query, onQueryChange = { query = it })
            }

            when {
                state.loading -> LoadingSkeleton()
                state.pairs.isEmpty() -> CatalogErrorState(onRetry = { vm.load() })
                else -> {
                    val q = query.trim()
                    val filtered = if (q.isEmpty()) state.pairs else state.pairs.filter {
                        autonym(it.l2).contains(q, ignoreCase = true) ||
                            langName(it.l2).contains(q, ignoreCase = true)
                    }
                    if (filtered.isEmpty()) {
                        SearchEmptyState(query = q)
                    } else {
                        val (priority, rest) = filtered.partition { (it.l1 to it.l2) in PRIORITY_PAIRS }
                        val priorityByL1 = priority.groupBy { it.l1 }
                        val restByL1 = rest.groupBy { it.l1 }

                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(11.dp),
                        ) {
                            if (priority.isNotEmpty()) {
                                item { SectionLabel(I18nStore.t("lang.recommended", "Recomendados"), FluentaTokens.BrandText) }
                                priorityByL1.forEach { (l1, pairs) ->
                                    item { FromLabel(l1) }
                                    items(pairs) { pair -> PairRow(pair, state.selecting == pair.l2) { selectLang(pair.l2) } }
                                }
                            }
                            if (rest.isNotEmpty()) {
                                item {
                                    Spacer(Modifier.height(8.dp))
                                    SectionLabel(I18nStore.t("lang.moreLanguages", "Más idiomas"), FluentaTokens.Muted)
                                }
                                restByL1.forEach { (l1, pairs) ->
                                    item { FromLabel(l1) }
                                    items(pairs) { pair -> PairRow(pair, state.selecting == pair.l2) { selectLang(pair.l2) } }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = if (query.isEmpty()) Color(0xFF9CB3AB) else FluentaTokens.Primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        I18nStore.t("lang.searchHint", "Buscar idioma…"),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF9CB3AB),
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = FluentaTokens.Ink),
                    cursorBrush = SolidColor(FluentaTokens.Primary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, color: Color) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 0.5.sp,
        color = color,
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
    )
}

@Composable
private fun FromLabel(l1: String) {
    Text(
        "${I18nStore.t("lang.from", "Desde")} ${langName(l1)}",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = FluentaTokens.Ink,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
    )
}

@Composable
private fun PairRow(pair: LanguagePair, selecting: Boolean, onClick: () -> Unit) {
    // Sin currículo aún = "próximamente": no dejamos entrar a una experiencia vacía.
    val available = pair.curriculumSeeded == true
    val borderColor = if (available) FluentaTokens.Border else FluentaTokens.Border
    Surface(
        onClick = onClick,
        enabled = available,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(2.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (available) Modifier else Modifier.alpha(0.72f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(flag(pair.l2), fontSize = 28.sp)
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                val auto = autonym(pair.l2)
                val localized = langName(pair.l2)
                Text(
                    if (localized.equals(auto, ignoreCase = true)) auto else "$auto · $localized",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = FluentaTokens.Ink,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    levelSystemName(pair.levelSystem),
                    style = MaterialTheme.typography.bodySmall,
                    color = FluentaTokens.Muted,
                )
            }
            if (selecting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = FluentaTokens.Primary)
            } else {
                CurriculumBadge(available)
            }
        }
    }
}

@Composable
private fun CurriculumBadge(available: Boolean) {
    val (label, bg, fg) = if (available) {
        Triple(I18nStore.t("lang.curriculumReady", "Con currículo"), FluentaTokens.Primary, Color.White)
    } else {
        Triple(I18nStore.t("lang.comingSoon", "Próximamente"), Color(0xFFE3EFEA), FluentaTokens.Muted)
    }
    Surface(shape = RoundedCornerShape(99.dp), color = bg) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = fg,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun LoadingSkeleton() {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        ShimmerCard(height = 14.dp, modifier = Modifier.fillMaxWidth(0.4f))
        repeat(2) { ShimmerCard(height = 64.dp) }
        Spacer(Modifier.height(10.dp))
        ShimmerCard(height = 14.dp, modifier = Modifier.fillMaxWidth(0.3f))
        repeat(3) { ShimmerCard(height = 64.dp) }
    }
}

@Composable
private fun SearchEmptyState(query: String) {
    CenteredState(
        icon = { CircleEmoji("🌐") },
        title = I18nStore.t("lang.searchEmptyTitle", "Sin coincidencias"),
        body = I18nStore.t("lang.searchEmptyBody", "No encontramos «$query». Revisa la ortografía o pídelo y lo añadimos.")
            .replace("\$query", query),
    )
}

@Composable
private fun CatalogErrorState(onRetry: () -> Unit) {
    CenteredState(
        icon = { CircleEmoji("📡") },
        title = I18nStore.t("lang.emptyTitle", "No cargaron los idiomas"),
        body = I18nStore.t("lang.emptyBody", "Algo falló al conectar. Inténtalo de nuevo."),
    ) {
        FluentaButton(
            text = I18nStore.t("common.retry", "Reintentar"),
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun CenteredState(
    icon: @Composable () -> Unit,
    title: String,
    body: String,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        icon()
        Spacer(Modifier.height(18.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = FluentaTokens.Ink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = FluentaTokens.Muted,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Spacer(Modifier.height(20.dp))
            action()
        }
    }
}

@Composable
private fun CircleEmoji(emoji: String) {
    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(Color(0xFFE3EFEA)),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, fontSize = 42.sp)
    }
}
