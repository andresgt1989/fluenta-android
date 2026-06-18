package com.alturya.fluenta.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.network.Badge
import com.alturya.fluenta.util.flag
import com.alturya.fluenta.util.langName
import com.alturya.fluenta.util.levelLabel
import com.alturya.fluenta.util.levelSystemName

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    onChangeLanguage: () -> Unit = {},
    onLogout: () -> Unit = {},
    onDiagnostic: () -> Unit = {},
    onSettings: () -> Unit = {},
    previewState: com.alturya.fluenta.profile.ProfileState? = null,
) {
    val context = LocalContext.current
    val vm: ProfileViewModel = viewModel()
    val vmState by vm.state.collectAsState()
    val state = previewState ?: vmState

    fun open(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    if (state.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    if (state.error) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Default.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(12.dp))
            Text(
                I18nStore.t("profile.loadError", "No pudimos cargar tu perfil. Revisa tu conexión."),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            com.alturya.fluenta.ui.FluentaButton(text = I18nStore.t("common.retry", "Reintentar"), onClick = { vm.load() })
        }
        return
    }

    val p = state.profile
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(I18nStore.t("profile.title", "Mi perfil"), style = MaterialTheme.typography.headlineMedium)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(p?.phone ?: "—", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(8.dp))
                Text("${I18nStore.t("home.learning", "Aprendiendo")}: ${flag(p?.l2)} ${langName(p?.l2)}")
                Text("${I18nStore.t("lang.from", "Desde")}: ${langName(p?.l1)}")
                Text("${I18nStore.t("profile.level", "Nivel")}: ${levelLabel(p?.level, p?.levelSystem)} (${levelSystemName(p?.levelSystem)})")
                Spacer(Modifier.height(8.dp))
                AssistChip(
                    onClick = {},
                    label = { Text("${I18nStore.t("profile.plan", "Plan")}: ${(p?.plan ?: "free").uppercase()}") }
                )
            }
        }

        // Share achievement card (Virality feature)
        if (p != null) {
            AchievementShareCard(
                l2 = p.l2,
                level = p.level,
                levelSystem = p.levelSystem,
                streakDays = p.streakDays ?: 0,
            )
        }

        if ((p?.plan ?: "free") == "free") {
            Text(I18nStore.t("profile.upgradeTitle", "Mejora tu plan"), style = MaterialTheme.typography.titleMedium)
            com.alturya.fluenta.ui.FluentaButton(
                text = I18nStore.t("profile.goPro", "Hazte Pro — \$9/mes"),
                onClick = { vm.openCheckout("basic") { open(it) } },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(
                onClick = { vm.openCheckout("pro") { open(it) } },
                modifier = Modifier.fillMaxWidth()
            ) { Text(I18nStore.t("profile.goPremium", "Premium — \$19/mes · voz ilimitada")) }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    I18nStore.t("profile.payNote", "Pago seguro con Stripe · aceptado en todo el mundo · cancela cuando quieras"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            com.alturya.fluenta.ui.FluentaButton(
                text = I18nStore.t("profile.manage", "Administrar suscripción"),
                onClick = { vm.openPortal { open(it) } },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        OutlinedButton(
            onClick = onDiagnostic,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.GpsFixed, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(I18nStore.t("profile.levelTest", "Realiza tu test de nivel"))
        }

        OutlinedButton(
            onClick = onChangeLanguage,
            modifier = Modifier.fillMaxWidth()
        ) { Text(I18nStore.t("profile.changeLanguage", "Cambiar idioma")) }

        OutlinedButton(
            onClick = onSettings,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(I18nStore.t("profile.settings", "Ajustes"))
        }

        TextButton(
            onClick = { vm.logout(context, onLogout) },
            modifier = Modifier.fillMaxWidth()
        ) { Text(I18nStore.t("profile.logout", "Cerrar sesión")) }

        // ── Badges / logros ───────────────────────────────────────────────────
        if (state.badges.isNotEmpty()) {
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            Text(
                "${I18nStore.t("profile.badges", "Logros")} · ${state.badgesEarned}/${state.badgesTotal}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                state.badges.forEach { badge -> BadgeChip(badge) }
            }
        }
        // ── Referidos ─────────────────────────────────────────────────────────
        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        ReferralCard()

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun BadgeChip(badge: Badge) {
    val earned = badge.earned
    Surface(
        color = if (earned) MaterialTheme.colorScheme.tertiaryContainer
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).widthIn(min = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                badge.icon,
                style = MaterialTheme.typography.titleLarge,
                color = if (earned) Color.Unspecified else Color.Gray,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                badge.title,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = if (earned) MaterialTheme.colorScheme.onTertiaryContainer
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                maxLines = 2,
            )
        }
    }
}
