package com.alturya.fluenta.onboarding

// Play-first onboarding (first run, before any account):
//   Step 0 — Welcome / value prop (hero con mascota Hoot)
//   Step 1 — "¿Qué idioma hablas?" (idioma base: es/en/pt)
//   Step 2 — "¿Qué quieres aprender?" (20 idiomas meta, menos el L1)
//   Step 3 — "¿Por qué aprendes …?" (motivación)
// On pick we hand (l1, l2) back to the nav graph, which opens a real guest lesson
// in that exact pair. L1 is inferred from the device locale.
//
// Diseño: kit Claude Design (Onboarding.dc.html) — gradiente hero verde→mint,
// progreso "PASO X DE 3", patrón bilingüe (autónimo · nombre localizado),
// seleccionar→Continuar, fichas de motivación con tile de color. Cableado intacto.

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alturya.fluenta.R
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.data.MotivationStore
import com.alturya.fluenta.ui.FluentaButton
import com.alturya.fluenta.ui.FluentaButtonStyle
import com.alturya.fluenta.ui.theme.FluentaTokens
import com.alturya.fluenta.util.autonym
import com.alturya.fluenta.util.flag
import com.alturya.fluenta.util.langName
import kotlinx.coroutines.launch

// Target languages offered up front: the 20 idiomas meta del MVP. The long tail
// still exists in the in-app selector; this is the curated set for the first-run
// pick. Todos tienen bandera + nombre localizado (ver util/LevelLabels.kt).
private val TARGET_LANGS = listOf(
    "en", "es", "pt", "fr", "de", "it", "ja", "zh", "ko", "ar",
    "ru", "hi", "tr", "nl", "sv", "pl", "el", "uk", "id", "vi",
)

// User (native) languages supported as L1 — courses exist from all three.
private val SOURCE_LANGS = listOf("es", "en", "pt")

// Tiles de motivación (kit): cada ficha lleva un cuadro de color suave (decorativo).
private val MOTIVATION_TILES = listOf(
    Color(0xFFCDEEE6), Color(0xFFFFF3DC), Color(0xFFFDE7E4), Color(0xFFE9E4FB),
    Color(0xFFD6F4E4), Color(0xFFE4ECFB),
)

@Composable
fun OnboardingScreen(onPicked: (l1: String, l2: String) -> Unit, onLogin: () -> Unit = {}) {
    // Device locale is just the DEFAULT — the user confirms/changes their
    // language explicitly (es/en/pt) instead of being locked to the detection.
    val detected = remember {
        java.util.Locale.getDefault().language.takeIf { it in SOURCE_LANGS } ?: "es"
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var l1 by remember { mutableStateOf(detected) }
    var l2 by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(0) }

    when (step) {
        0 -> WelcomeStep(onStart = { step = 1 }, onLogin = onLogin)
        1 -> SourceLanguageStep(selected = l1, onContinue = { l1 = it; step = 2 }, onBack = { step = 0 })
        2 -> LanguagePickStep(l1 = l1, onContinue = { l2 = it; step = 3 }, onBack = { step = 1 })
        // Paso de META / "por qué": personaliza la experiencia y crea compromiso
        // antes del primer minuto (lo que pedía el scorecard de onboarding).
        else -> MotivationStep(
            l2 = l2,
            onPick = { motivationId ->
                scope.launch { MotivationStore.set(context, motivationId) }
                onPicked(l1, l2)
            },
            onBack = { step = 2 },
        )
    }
}

// ── Paso 0 — Hero ────────────────────────────────────────────────────────────
@Composable
private fun WelcomeStep(onStart: () -> Unit, onLogin: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(FluentaTokens.HeroGradient),
    ) {
        // Mascota Hoot + value prop, centrados sobre el gradiente.
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(158.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_fluenta_hola),
                    contentDescription = null,
                    modifier = Modifier.size(112.dp),
                )
            }
            Spacer(Modifier.height(28.dp))
            // a11y: Ink (#0F2E27) sobre el mint del gradiente supera 9:1 (el blanco del
            // mockup quedaba ~2.5:1). Mantiene la identidad sin romper contraste.
            Text(
                "Fluenta",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold,
                color = FluentaTokens.Ink,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(12.dp))
            Text(
                I18nStore.t("onboarding.welcomeTagline", "Aprende cualquier idioma con tu mentor de IA"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = FluentaTokens.Ink.copy(alpha = 0.82f),
                textAlign = TextAlign.Center,
            )
        }
        FluentaButton(
            text = I18nStore.t("onboarding.start", "Empezar"),
            onClick = onStart,
            style = FluentaButtonStyle.Ink,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
        )
        // "Ya tengo cuenta · Entrar" — atajo a login (kit Claude Design).
        TextButton(
            onClick = onLogin,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp, bottom = 28.dp),
        ) {
            Text(
                I18nStore.t("onboarding.haveAccount", "Ya tengo cuenta"),
                style = MaterialTheme.typography.bodyMedium,
                color = FluentaTokens.Ink.copy(alpha = 0.85f),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                I18nStore.t("onboarding.login", "Entrar"),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.ExtraBold,
                color = FluentaTokens.Ink,
            )
        }
    }
}

// Scaffold común de los pasos: barra ← + progreso, etiqueta "PASO X DE N", título.
@Composable
private fun OnboardingStepScaffold(
    step: Int,
    total: Int,
    title: String,
    subtitle: String?,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(FluentaTokens.Surface)
            .statusBarsPadding(),
    ) {
        Column(Modifier.padding(start = 12.dp, end = 24.dp, top = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = I18nStore.t("common.back", "Atrás"),
                        tint = FluentaTokens.Muted,
                    )
                }
                Spacer(Modifier.width(4.dp))
                LinearProgressIndicator(
                    progress = { step.toFloat() / total.toFloat() },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(99.dp)),
                    color = FluentaTokens.Primary,
                    trackColor = FluentaTokens.Container,
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "${I18nStore.t("onboarding.step", "Paso")} $step ${I18nStore.t("common.of", "de")} $total".uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp,
                color = FluentaTokens.BrandText,
                modifier = Modifier.padding(start = 12.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = FluentaTokens.Ink,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .semantics { heading() },
            )
            if (subtitle != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = FluentaTokens.Muted,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
        content()
    }
}

@Composable
private fun SourceLanguageStep(selected: String, onContinue: (String) -> Unit, onBack: () -> Unit) {
    var sel by remember { mutableStateOf(selected) }
    OnboardingStepScaffold(
        step = 1, total = 3,
        title = I18nStore.t("onboarding.sourceTitle", "¿Qué idioma hablas?"),
        subtitle = I18nStore.t("onboarding.sourceSubtitle", "Te lo explicaremos todo en tu idioma."),
        onBack = onBack,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SOURCE_LANGS.forEach { code ->
                LangSelectRow(code = code, selected = code == sel, onClick = { sel = code })
            }
        }
        FluentaButton(
            text = I18nStore.t("common.continue", "Continuar"),
            onClick = { onContinue(sel) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
        )
    }
}

@Composable
private fun LanguagePickStep(l1: String, onContinue: (String) -> Unit, onBack: () -> Unit) {
    val targets = remember(l1) { TARGET_LANGS.filter { it != l1 } }
    var sel by remember { mutableStateOf("") }
    OnboardingStepScaffold(
        step = 2, total = 3,
        title = I18nStore.t("onboarding.pickTitle", "¿Qué quieres aprender?"),
        subtitle = I18nStore.t("onboarding.pickSubtitle", "Empieza ahora mismo, sin crear cuenta."),
        onBack = onBack,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            targets.forEach { code ->
                LangSelectRow(code = code, selected = code == sel, onClick = { sel = code })
            }
        }
        FluentaButton(
            text = I18nStore.t("common.continue", "Continuar"),
            onClick = { onContinue(sel) },
            enabled = sel.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
        )
    }
}

@Composable
private fun MotivationStep(l2: String, onPick: (String) -> Unit, onBack: () -> Unit) {
    var sel by remember { mutableStateOf("") }
    OnboardingStepScaffold(
        step = 3, total = 3,
        title = I18nStore.t("onboarding.motivationTitle", "¿Por qué aprendes {lang}?")
            .replace("{lang}", langName(l2)),
        subtitle = I18nStore.t("onboarding.motivationSubtitle", "Personalizamos tu experiencia según tu meta."),
        onBack = onBack,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            MotivationStore.OPTIONS.forEachIndexed { i, opt ->
                MotivationTile(
                    emoji = opt.emoji,
                    label = I18nStore.t("motivation.${opt.id}", opt.label),
                    tile = MOTIVATION_TILES[i % MOTIVATION_TILES.size],
                    selected = sel == opt.id,
                    onClick = { sel = opt.id },
                )
            }
        }
        FluentaButton(
            text = I18nStore.t("onboarding.motivationCta", "Comenzar mi aventura"),
            onClick = { onPick(sel) },
            enabled = sel.isNotEmpty(),
            style = FluentaButtonStyle.Success,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
        )
    }
}

// Ficha bilingüe: autónimo (中文 / Español) arriba, nombre localizado debajo si difiere.
@Composable
private fun LangSelectRow(code: String, selected: Boolean, onClick: () -> Unit) {
    val auto = autonym(code)
    val localized = langName(code)
    val borderColor = if (selected) FluentaTokens.Primary else FluentaTokens.Border
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(2.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .semantics { this.selected = selected },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(flag(code), fontSize = 28.sp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    auto,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = FluentaTokens.Ink,
                )
                if (!localized.equals(auto, ignoreCase = true)) {
                    Text(
                        localized,
                        style = MaterialTheme.typography.bodySmall,
                        color = FluentaTokens.Muted,
                    )
                }
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(FluentaTokens.Primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MotivationTile(
    emoji: String,
    label: String,
    tile: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (selected) FluentaTokens.Container else Color.White,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(2.dp, FluentaTokens.Border),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { this.selected = selected },
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(if (selected) Color.White else tile),
                contentAlignment = Alignment.Center,
            ) {
                Text(emoji, fontSize = 22.sp)
            }
            Spacer(Modifier.width(14.dp))
            Text(
                label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = FluentaTokens.Ink,
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = FluentaTokens.BrandText,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}
