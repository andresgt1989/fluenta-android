package com.alturya.fluenta.upgrade

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alturya.fluenta.R
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.network.ApiClient
import com.alturya.fluenta.ui.FluentaButton
import com.alturya.fluenta.ui.theme.FluentaTokens
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
                _state.value = _state.value.copy(loading = false, error = I18nStore.t("paywall.error.checkout", "No se completó la compra. No se realizó ningún cargo. Inténtalo de nuevo."))
            }
        }
    }
}

// Beneficios incluidos en Pro. titleKey/title: clave i18n + fallback es (se resuelven
// en el render, no aquí). Datos REALES del producto — no inventamos planes.
private data class ProBenefit(val titleKey: String, val title: String)
private data class Testimonial(val quoteKey: String, val quote: String, val authorKey: String, val author: String, val stars: Int = 5)

private val PRO_BENEFITS = listOf(
    ProBenefit("paywall.feat.convo.title", "Conversación IA ilimitada"),
    ProBenefit("paywall.feat.shield.title", "Escudo de racha"),
    ProBenefit("paywall.feat.offline.title", "Lecciones sin conexión"),
    ProBenefit("paywall.feat.noads.title", "Sin anuncios ni interrupciones"),
    ProBenefit("paywall.feat.srs.title", "Repaso inteligente ilimitado"),
)

private val TESTIMONIALS = listOf(
    Testimonial(
        quoteKey = "paywall.testimonial1.quote",
        quote = "Aprendí más inglés en 2 semanas con Fluenta que en 2 años con otras apps. ¡El coach de IA realmente te habla!",
        authorKey = "paywall.testimonial1.author",
        author = "María G. · México · Inglés B2",
    ),
    Testimonial(
        quoteKey = "paywall.testimonial2.quote",
        quote = "Lo uso 10 minutos en el metro. Mi inglés mejoró notablemente y me ascendieron en el trabajo.",
        authorKey = "paywall.testimonial2.author",
        author = "Carlos R. · Argentina · Inglés B1",
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreen(onDismiss: () -> Unit) {
    val vm: PaywallViewModel = viewModel()
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.checkoutUrl) {
        state.checkoutUrl?.let { url ->
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
        com.alturya.fluenta.data.Analytics.track(context, com.alturya.fluenta.data.Analytics.PAYWALL_VIEW)
    }

    Scaffold(containerColor = FluentaTokens.Surface) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(pad),
        ) {
            // ── Hero: ✕ + mascota + título ─────────────────────────────────────
            Column(Modifier.padding(start = 26.dp, end = 26.dp, top = 8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = I18nStore.t("common.close", "Cerrar"), tint = FluentaTokens.Muted)
                    }
                }
                AnimatedVisibility(visible = visible, enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { -40 }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.ic_fluenta_hola),
                            contentDescription = null,
                            modifier = Modifier.size(52.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                I18nStore.t("paywall.heroTitle", "Aprende sin límites"),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = FluentaTokens.Ink,
                                modifier = Modifier.semantics { heading() },
                            )
                            Text(
                                I18nStore.t("paywall.heroSubtitle", "Desbloquea todo Fluenta"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = FluentaTokens.Muted,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // ── Planes (datos reales: Anual recomendado + Mensual) ─────────────
            Column(Modifier.padding(horizontal = 26.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                RecommendedPlanCard(
                    title = I18nStore.t("paywall.annual", "Anual"),
                    tagline = I18nStore.t("paywall.annualTagline", "Para avanzar en serio"),
                    price = "$4.99",
                    period = I18nStore.t("paywall.perMonth", "/mes"),
                    originalPrice = "$9.99",
                    benefits = PRO_BENEFITS.take(3),
                    selected = state.selectedPlan == "annual",
                    onClick = { vm.selectPlan("annual") },
                )
                SimplePlanCard(
                    title = I18nStore.t("paywall.monthly", "Mensual"),
                    tagline = I18nStore.t("paywall.monthlyTagline", "Flexibilidad total"),
                    price = "$9.99",
                    period = I18nStore.t("paywall.perMonth", "/mes"),
                    selected = state.selectedPlan == "monthly",
                    onClick = { vm.selectPlan("monthly") },
                )
            }

            // ── Ahorro (plan anual) ────────────────────────────────────────────
            Spacer(Modifier.height(14.dp))
            Surface(
                color = FluentaTokens.SuccessBg,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp),
            ) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = FluentaTokens.SuccessInk, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        I18nStore.t("paywall.savings", "Con el plan anual ahorras $60/año · equivale a $0.16/día"),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = FluentaTokens.SuccessInk,
                    )
                }
            }

            // ── Beneficios ──────────────────────────────────────────────────────
            Spacer(Modifier.height(24.dp))
            Column(Modifier.padding(horizontal = 26.dp)) {
                Text(
                    I18nStore.t("paywall.included", "Qué incluye Pro"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = FluentaTokens.Ink,
                )
                Spacer(Modifier.height(12.dp))
                PRO_BENEFITS.forEach { b ->
                    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = FluentaTokens.Primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(I18nStore.t(b.titleKey, b.title), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = FluentaTokens.Ink)
                    }
                }
            }

            // ── Testimonios ─────────────────────────────────────────────────────
            Spacer(Modifier.height(24.dp))
            Column(Modifier.padding(horizontal = 26.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    I18nStore.t("paywall.testimonials", "Lo que dicen los usuarios"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = FluentaTokens.Ink,
                )
                TESTIMONIALS.forEach { TestimonialCard(it) }
            }

            // ── CTA ─────────────────────────────────────────────────────────────
            Spacer(Modifier.height(24.dp))
            Column(Modifier.padding(horizontal = 26.dp)) {
                state.error?.let { err ->
                    Text(err, color = FluentaTokens.Coral, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))
                }
                FluentaButton(
                    text = if (state.loading) I18nStore.t("paywall.processing", "Procesando…")
                    else I18nStore.t("paywall.startTrial", "Empezar 7 días gratis"),
                    onClick = { vm.startCheckout() },
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth(),
                    leading = if (state.loading) {
                        { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White) }
                    } else null,
                )
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        I18nStore.t("paywall.noThanks", "Cancela cuando quieras · Restaurar compra"),
                        style = MaterialTheme.typography.bodySmall,
                        color = FluentaTokens.Muted,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    I18nStore.t("paywall.legal", "Sin cargos durante el período de prueba. Cancela antes de que termine para no ser cobrado. Renovación automática."),
                    style = MaterialTheme.typography.labelSmall,
                    color = FluentaTokens.Muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun RecommendedPlanCard(
    title: String,
    tagline: String,
    price: String,
    period: String,
    originalPrice: String?,
    benefits: List<ProBenefit>,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(Modifier.fillMaxWidth()) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            border = BorderStroke(if (selected) 2.5.dp else 2.dp, if (selected) FluentaTokens.Primary else FluentaTokens.Border),
            shadowElevation = if (selected) 6.dp else 0.dp,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column {
                        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = FluentaTokens.Ink)
                        Text(tagline, style = MaterialTheme.typography.bodySmall, color = FluentaTokens.Muted)
                    }
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(price, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = FluentaTokens.Ink)
                        Text(period, style = MaterialTheme.typography.labelMedium, color = FluentaTokens.Muted, modifier = Modifier.padding(bottom = 3.dp))
                    }
                }
                originalPrice?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = FluentaTokens.Muted, textDecoration = TextDecoration.LineThrough)
                }
                Spacer(Modifier.height(10.dp))
                benefits.forEach { b ->
                    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = FluentaTokens.Primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(I18nStore.t(b.titleKey, b.title), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = FluentaTokens.Ink)
                    }
                }
            }
        }
        // Badge "RECOMENDADO" flotante (ámbar) — el destacado del kit.
        Surface(
            shape = RoundedCornerShape(99.dp),
            color = FluentaTokens.Amber,
            modifier = Modifier.align(Alignment.TopStart).padding(start = 18.dp),
        ) {
            Text(
                I18nStore.t("paywall.recommended", "RECOMENDADO"),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun SimplePlanCard(
    title: String,
    tagline: String,
    price: String,
    period: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(2.dp, if (selected) FluentaTokens.Primary else FluentaTokens.Border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = FluentaTokens.Primary))
            Spacer(Modifier.width(4.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = FluentaTokens.Ink)
                Text(tagline, style = MaterialTheme.typography.bodySmall, color = FluentaTokens.Muted)
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(price, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = FluentaTokens.Ink)
                Text(period, style = MaterialTheme.typography.labelMedium, color = FluentaTokens.Muted, modifier = Modifier.padding(bottom = 2.dp))
            }
        }
    }
}

@Composable
private fun TestimonialCard(t: Testimonial) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, FluentaTokens.Border), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row {
                repeat(t.stars) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = FluentaTokens.Amber, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("\"${I18nStore.t(t.quoteKey, t.quote)}\"", style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic, color = FluentaTokens.Ink)
            Spacer(Modifier.height(4.dp))
            Text("— ${I18nStore.t(t.authorKey, t.author)}", style = MaterialTheme.typography.labelSmall, color = FluentaTokens.Muted)
        }
    }
}
