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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.data.MotivationStore
import com.alturya.fluenta.util.flag
import com.alturya.fluenta.util.langName
import kotlinx.coroutines.launch

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
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var l1 by remember { mutableStateOf(detected) }
    var l2 by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(0) }

    when (step) {
        0 -> WelcomeStep(onStart = { step = 1 })
        1 -> SourceLanguageStep(selected = l1, onPick = { l1 = it; step = 2 }, onBack = { step = 0 })
        2 -> LanguagePickStep(l1 = l1, onPick = { l2 = it; step = 3 }, onBack = { step = 1 })
        // Paso de META / "por qué": personaliza la experiencia y crea compromiso
        // antes del primer minuto (lo que pedía el scorecard de onboarding).
        else -> MotivationStep(
            onPick = { motivationId ->
                scope.launch { com.alturya.fluenta.data.MotivationStore.set(context, motivationId) }
                onPicked(l1, l2)
            },
            onBack = { step = 2 },
        )
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
                // a11y: expone el estado "seleccionado" semanticamente (WCAG 4.1.2); antes solo
                // se veia por color + checkmark, invisible para un lector de pantalla. `isSelected`
                // se calcula fuera del lambda semantics para no chocar con la propiedad `selected`.
                val isSelected = code == selected
                Card(
                    onClick = { onPick(code) },
                    // `this.selected` (no `selected`) para apuntar a la propiedad del receiver
                    // de semantics y no a la variable externa `selected` (el codigo elegido).
                    modifier = Modifier.fillMaxWidth().semantics { this.selected = isSelected },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
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
                        if (code == selected) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
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
                Icon(Icons.Default.MicNone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
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
            I18nStore.t("onboarding.welcomeTagline", "Aprende un idioma de verdad, hablando desde el primer día."),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Bullet(I18nStore.t("onboarding.bullet1", "Habla en voz alta desde el primer minuto"))
        Bullet(I18nStore.t("onboarding.bullet2", "Tu tutor de IA te corrige al instante"))
        Bullet(I18nStore.t("onboarding.bullet3", "Lecciones cortas: 10 minutos al día"))
        Spacer(Modifier.height(36.dp))
        com.alturya.fluenta.ui.FluentaButton(
            text = I18nStore.t("onboarding.start", "Empezar"),
            onClick = onStart,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun Bullet(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Icono consistente (no emoji) — más profesional + accesible.
        androidx.compose.material3.Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun MotivationStep(onPick: (String) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        BackBar(onBack)
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 8.dp)) {
            Text(
                I18nStore.t("onboarding.motivationTitle", "¿Para qué quieres aprender?"),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                I18nStore.t("onboarding.motivationSubtitle", "Personalizamos tu experiencia según tu meta."),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(MotivationStore.OPTIONS) { opt ->
                Card(
                    onClick = { onPick(opt.id) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(opt.emoji, style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.width(16.dp))
                        Text(
                            I18nStore.t("motivation.${opt.id}", opt.label),
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
