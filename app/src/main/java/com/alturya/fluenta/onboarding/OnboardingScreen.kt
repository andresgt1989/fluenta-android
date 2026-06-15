package com.alturya.fluenta.onboarding

// Play-first onboarding (first run, before any account):
//   Step 0 — Welcome / value prop
//   Step 1 — "¿Qué idioma quieres aprender?" (20×20: any target, minus the user's L1)
// On pick we hand (l1, l2) back to the nav graph, which opens a real guest lesson
// in that exact pair. L1 is inferred from the device locale.

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.util.flag
import com.alturya.fluenta.util.langName

// Target languages offered up front. The long tail still exists in the in-app
// selector; this is the curated set for the first-run pick.
private val TARGET_LANGS = listOf(
    "en", "es", "pt", "fr", "de", "it", "ja", "zh", "ko", "ar", "ru", "hi", "tr", "nl", "sv", "pl",
)

// User (native) languages supported as L1 — courses exist from all three.
private val SOURCE_LANGS = listOf("es", "en", "pt")

@Composable
fun OnboardingScreen(onPicked: (l1: String, l2: String) -> Unit) {
    // Device locale is just the DEFAULT — the user confirms/changes their
    // language explicitly (es/en/pt) instead of being locked to the detection.
    val detected = remember {
        java.util.Locale.getDefault().language.takeIf { it in SOURCE_LANGS } ?: "es"
    }
    var l1 by remember { mutableStateOf(detected) }
    var step by remember { mutableStateOf(0) }

    when (step) {
        0 -> WelcomeStep(onStart = { step = 1 })
        1 -> SourceLanguageStep(selected = l1, onPick = { l1 = it; step = 2 }, onBack = { step = 0 })
        else -> LanguagePickStep(l1 = l1, onPick = { l2 -> onPicked(l1, l2) }, onBack = { step = 1 })
    }
}

@Composable
private fun BackBar(onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 8.dp, top = 8.dp)) {
        TextButton(onClick = onBack) { Text("‹ ${I18nStore.t("common.back", "Atrás")}") }
    }
}

@Composable
private fun SourceLanguageStep(selected: String, onPick: (String) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        BackBar(onBack)
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 8.dp)) {
            Text(
                I18nStore.t("onboarding.sourceTitle", "¿Qué idioma hablas?"),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                I18nStore.t("onboarding.sourceSubtitle", "Te enseñaremos desde tu idioma."),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(SOURCE_LANGS) { code ->
                Card(
                    onClick = { onPick(code) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (code == selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(flag(code), style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.width(16.dp))
                        Text(
                            langName(code),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        if (code == selected) Text("✓", style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep(onStart: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(104.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("🗣️", style = MaterialTheme.typography.displayMedium)
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "Fluenta",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            I18nStore.t("onboarding.welcomeTagline", "Aprende un idioma de verdad. 15 minutos al día."),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Bullet(I18nStore.t("onboarding.bullet1", "Habla en voz alta desde el primer minuto"))
        Bullet(I18nStore.t("onboarding.bullet2", "Tu tutor de IA te corrige al instante"))
        Bullet(I18nStore.t("onboarding.bullet3", "Lecciones cortas: 10 minutos al día"))
        Spacer(Modifier.height(36.dp))
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().height(54.dp),
        ) {
            Text(
                I18nStore.t("onboarding.start", "Empezar"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun Bullet(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("✅ ", style = MaterialTheme.typography.titleMedium)
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun LanguagePickStep(l1: String, onPick: (String) -> Unit, onBack: () -> Unit) {
    val targets = remember(l1) { TARGET_LANGS.filter { it != l1 } }
    Column(Modifier.fillMaxSize()) {
        BackBar(onBack)
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 8.dp)) {
            Text(
                I18nStore.t("onboarding.pickTitle", "¿Qué idioma quieres aprender?"),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                I18nStore.t("onboarding.pickSubtitle", "Empieza ahora mismo, sin crear cuenta."),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(targets) { code ->
                Card(
                    onClick = { onPick(code) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(flag(code), style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.width(16.dp))
                        Text(
                            langName(code),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        Text("›", style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
