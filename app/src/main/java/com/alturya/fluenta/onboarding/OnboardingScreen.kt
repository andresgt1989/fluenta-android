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
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.TrendingUp
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
import com.alturya.fluenta.data.Analytics
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
fun OnboardingScreen(onPicked: (l1: String, l2: String) -> Unit, onLogin: () -> Unit = {}, onBack: () -> Unit = {}) {
    // Device locale is just the DEFAULT — the user confirms/changes their
    // language explicitly (es/en/pt) instead of being locked to the detection.
    val detected = remember {
        java.util.Locale.getDefault().language.takeIf { it in SOURCE_LANGS } ?: "es"
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    // Onboarding SIN FRICCIÓN (mock): sin paso de idioma-origen (L1 se infiere del
    // locale, como el fast-path) y SIN paso de motivación. Solo: idioma destino
    // (chino preseleccionado) → nivel SALTABLE → micro-lección. Registro al final.
    val l1 = detected
    var l2 by remember { mutableStateOf("zh") }
    var step by remember { mutableStateOf(1) }

    when (step) {
        1 -> LanguagePickStep(
            l1 = l1,
            onContinue = { picked ->
                l2 = picked
                Analytics.track(context, Analytics.ONBOARDING_STEP, mapOf("step" to "language", "lang" to picked))
                step = 2
            },
            onBack = onBack,
        )
        else -> LevelStep(
            onContinue = { level ->
                Analytics.track(context, Analytics.ONBOARDING_STEP, mapOf("step" to "level", "level" to level))
                onPicked(l1, l2)
            },
            onSkip = {
                Analytics.track(context, Analytics.ONBOARDING_STEP, mapOf("step" to "level", "level" to "skipped"))
                onPicked(l1, l2)
            },
            onBack = { step = 1 },
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
    // Onboarding sin fricción (mock 02): CHINO DESTACADO y PRESELECCIONADO → el
    // usuario avanza en 1 toque. El chino va primero; el resto debajo. (Sin esto,
    // elegir idioma era un paso de fricción antes de la 1ª lección de chino.)
    val targets = remember(l1) {
        listOf("zh") + TARGET_LANGS.filter { it != l1 && it != "zh" }
    }
    var sel by remember { mutableStateOf("zh") }
    OnboardingStepScaffold(
        step = 1, total = 2,
        title = I18nStore.t("onboarding.pickTitle", "¿Qué quieres aprender?"),
        subtitle = I18nStore.t("onboarding.pickSubtitle", "Toca un idioma para empezar."),
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
                LangSelectRow(
                    code = code,
                    selected = code == sel,
                    featured = code == "zh",
                    onClick = { sel = code },
                )
            }
        }
        FluentaButton(
            text = if (sel.isNotEmpty())
                I18nStore.t("onboarding.continueWith", "Continuar con {lang}").replace("{lang}", langName(sel))
            else I18nStore.t("common.continue", "Continuar"),
            onClick = { onContinue(sel) },
            enabled = sel.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
        )
    }
}

// Onboarding sin fricción · pantalla 03 (SALTABLE): nivel de partida. Ajusta la 1ª
// lección; el usuario puede saltarlo (link "Saltar") o elegir un nivel. Quitó la
// fricción del antiguo paso de Motivación.
@Composable
private fun LevelStep(onContinue: (String) -> Unit, onSkip: () -> Unit, onBack: () -> Unit) {
    var sel by remember { mutableStateOf("") }
    OnboardingStepScaffold(
        step = 2, total = 2,
        title = I18nStore.t("onboarding.levelTitle", "¿Cuánto chino sabes?"),
        subtitle = I18nStore.t("onboarding.levelSubtitle", "Ajustamos tu primera lección. Puedes saltarlo."),
        onBack = onBack,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            LevelRow(
                Icons.Default.Spa,
                I18nStore.t("onboarding.levelZero", "Desde cero"),
                I18nStore.t("onboarding.levelZeroSub", "Nunca he estudiado chino"),
                sel == "zero",
            ) { sel = "zero" }
            LevelRow(
                Icons.Default.School,
                I18nStore.t("onboarding.levelSome", "Sé algo"),
                I18nStore.t("onboarding.levelSomeSub", "Conozco saludos y tonos"),
                sel == "some",
            ) { sel = "some" }
            LevelRow(
                Icons.Default.TrendingUp,
                I18nStore.t("onboarding.levelPractice", "Quiero practicar"),
                I18nStore.t("onboarding.levelPracticeSub", "Ya tengo base, busco fluidez"),
                sel == "practice",
            ) { sel = "practice" }
        }
        FluentaButton(
            text = I18nStore.t("common.continue", "Continuar"),
            onClick = { onContinue(sel) },
            enabled = sel.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 4.dp),
        )
        TextButton(
            onClick = onSkip,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 20.dp),
        ) {
            Text(
                I18nStore.t("onboarding.skip", "Saltar"),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = FluentaTokens.BrandText,
            )
        }
    }
}

@Composable
private fun LevelRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
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
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(FluentaTokens.Container),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = FluentaTokens.BrandText, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = FluentaTokens.Ink)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = FluentaTokens.Muted)
            }
            if (selected) {
                Box(
                    Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(FluentaTokens.Primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// Cobertura real de currículo por idioma (probe backend 2026-06-23): solo en+zh son
// profundos; sv casi vacío; el resto stub. Etiquetar honestamente evita el "callejón mudo".
private val FULL_COVERAGE = setOf("en", "zh")
private val COMING_COVERAGE = setOf("sv")

@Composable
private fun CoverageChip(code: String) {
    val (label, bg, ink) = when {
        code in FULL_COVERAGE -> Triple(I18nStore.t("coverage.full", "Completo"), Color(0xFFCDEEE6), Color(0xFF0A6F64))
        code in COMING_COVERAGE -> Triple(I18nStore.t("coverage.coming", "Pronto"), Color(0xFFE7EEEC), Color(0xFF7C857F))
        else -> Triple(I18nStore.t("coverage.beta", "Beta"), Color(0xFFFBE7C6), Color(0xFFC77A12))
    }
    Surface(shape = RoundedCornerShape(999.dp), color = bg) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = ink,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

@Composable
private fun LangSelectRow(code: String, selected: Boolean, featured: Boolean = false, onClick: () -> Unit) {
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
                if (featured) {
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .background(FluentaTokens.Primary)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            I18nStore.t("onboarding.featured", "DESTACADO"),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                        )
                    }
                }
            }
            CoverageChip(code)
            if (selected) {
                Spacer(Modifier.width(8.dp))
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
