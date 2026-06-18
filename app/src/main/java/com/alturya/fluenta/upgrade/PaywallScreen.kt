package com.alturya.fluenta.upgrade

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.network.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PaywallState(
    val loading: Boolean = false,
    val checkoutUrl: String? = null,
    val error: String? = null,
    val selectedPlan: String = "annual",
)

class PaywallViewModel : ViewModel() {
    private val _state = MutableStateFlow(PaywallState())
    val state = _state.asStateFlow()

    fun selectPlan(plan: String) {
        _state.value = _state.value.copy(selectedPlan = plan, checkoutUrl = null, error = null)
    }

    fun startCheckout() {
        val plan = _state.value.selectedPlan
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val res = ApiClient.api.getCheckoutUrl(plan)
                _state.value = _state.value.copy(loading = false, checkoutUrl = res.url)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = "No se pudo iniciar el pago. Intenta de nuevo.")
            }
        }
    }
}

private data class Feature(val icon: ImageVector, val title: String, val subtitle: String, val proOnly: Boolean = false)
private data class Testimonial(val quote: String, val author: String, val stars: Int = 5)

private val TESTIMONIALS = listOf(
    Testimonial(
        quote = "Aprendí más inglés en 2 semanas con Fluenta que en 2 años con otras apps. ¡El coach de IA realmente te habla!",
        author = "María G. · México · Inglés B2",
    ),
    Testimonial(
        quote = "Lo uso 10 minutos en el metro. Mi inglés mejoró notablemente y me ascendieron en el trabajo.",
        author = "Carlos R. · Argentina · Inglés B1",
    ),
)

private val FEATURES = listOf(
    Feature(Icons.Default.MicNone,            "Conversación IA ilimitada",        "Habla inglés sin límites con tu coach"),
    Feature(Icons.Default.Shield,             "Escudo de racha",                  "Protege tu racha hasta 3 días", proOnly = true),
    Feature(Icons.Default.WifiOff,            "Lecciones sin conexión",           "Practica en el metro, sin internet", proOnly = true),
    Feature(Icons.Default.LocalFireDepartment,"Sin anuncios ni interrupciones",   "Flujo de aprendizaje puro", proOnly = true),
    Feature(Icons.Default.Star,               "Repaso inteligente ilimitado",     "SRS sin tope de tarjetas por día"),
    Feature(Icons.Default.CheckCircle,        "Estadísticas avanzadas",           "Velocidad lectora, éxito por habilidad", proOnly = true),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreen(onDismiss: () -> Unit) {
    val vm: PaywallViewModel = viewModel()
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    // Open checkout URL when ready
    LaunchedEffect(state.checkoutUrl) {
        state.checkoutUrl?.let { url ->
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = I18nStore.t("common.close", "Cerrar"))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(pad),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── Hero gradient header ───────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primaryContainer,
                            )
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AnimatedVisibility(visible = visible, enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { -40 }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Fluenta Pro",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            I18nStore.t("paywall.tagline", "Habla con confianza en 90 días"),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(16.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f),
                            shape = MaterialTheme.shapes.large,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    I18nStore.t("paywall.trialBadge", "7 días gratis — cancela cuando quieras"),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Plan selector ─────────────────────────────────────────────────
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text(
                    I18nStore.t("paywall.choosePlan", "Elige tu plan"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(12.dp))
                PlanCard(
                    title = I18nStore.t("paywall.annual", "Anual"),
                    price = "$4.99",
                    period = I18nStore.t("paywall.perMonth", "/mes"),
                    originalPrice = "$9.99",
                    badge = I18nStore.t("paywall.bestValue", "MEJOR VALOR · −50%"),
                    selected = state.selectedPlan == "annual",
                    onClick = { vm.selectPlan("annual") },
                )
                Spacer(Modifier.height(10.dp))
                PlanCard(
                    title = I18nStore.t("paywall.monthly", "Mensual"),
                    price = "$9.99",
                    period = I18nStore.t("paywall.perMonth", "/mes"),
                    originalPrice = null,
                    badge = null,
                    selected = state.selectedPlan == "monthly",
                    onClick = { vm.selectPlan("monthly") },
                )
            }

            // ── Savings calculator ────────────────────────────────────────────
            Spacer(Modifier.height(12.dp))
            Surface(
                color = Color(0xFF22C55E).copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        I18nStore.t("paywall.savings", "Con el plan anual ahorras $60/año · equivale a $0.16/día"),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF16A34A),
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Features ──────────────────────────────────────────────────────
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text(
                    I18nStore.t("paywall.included", "Qué incluye Pro"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(12.dp))
                FEATURES.forEach { feature ->
                    FeatureRow(feature)
                    Spacer(Modifier.height(12.dp))
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Testimonios ───────────────────────────────────────────────────
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text(
                    I18nStore.t("paywall.testimonials", "Lo que dicen los usuarios"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(12.dp))
                TESTIMONIALS.forEach { t ->
                    TestimonialCard(t)
                    Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── CTA ───────────────────────────────────────────────────────────
            Column(Modifier.padding(horizontal = 20.dp)) {
                state.error?.let { err ->
                    Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp))
                }
                Button(
                    onClick = { vm.startCheckout() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !state.loading,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    if (state.loading) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text(
                            I18nStore.t("paywall.startTrial", "Empezar 7 días gratis"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        I18nStore.t("paywall.noThanks", "Continuar con el plan gratuito"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        I18nStore.t("paywall.guarantee", "Satisfacción garantizada — si no te gusta, te devolvemos el dinero"),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF16A34A),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    I18nStore.t("paywall.legal", "Sin cargos durante el período de prueba. Cancela antes de que termine para no ser cobrado. Renovación automática."),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PlanCard(
    title: String,
    price: String,
    period: String,
    originalPrice: String?,
    badge: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val bgColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .border(width = if (selected) 2.dp else 1.dp, color = borderColor, shape = MaterialTheme.shapes.medium),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(if (selected) 4.dp else 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    badge?.let {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.tertiary,
                            shape = MaterialTheme.shapes.extraSmall,
                        ) {
                            Text(it, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
                originalPrice?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textDecoration = TextDecoration.LineThrough)
                }
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(price, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                Text(period, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 3.dp))
            }
        }
    }
}

@Composable
private fun TestimonialCard(t: Testimonial) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row {
                repeat(t.stars) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFEAB308), modifier = Modifier.size(14.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "\"${t.quote}\"",
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "— ${t.author}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FeatureRow(feature: Feature) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = feature.icon,
            contentDescription = null,
            tint = if (feature.proOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(22.dp).padding(top = 1.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(feature.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                if (feature.proOnly) {
                    Spacer(Modifier.width(6.dp))
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.extraSmall) {
                        Text("PRO", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                    }
                }
            }
            Text(feature.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
