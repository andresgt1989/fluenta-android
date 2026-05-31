package com.alturya.fluenta.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

@Composable
fun ProfileScreen(
    onChangeLanguage: () -> Unit = {},
    onLogout: () -> Unit = {},
    onDiagnostic: () -> Unit = {}
) {
    val context = LocalContext.current
    val vm: ProfileViewModel = viewModel()
    val state by vm.state.collectAsState()

    fun open(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    if (state.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
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
                    Text("📱 ", style = MaterialTheme.typography.titleMedium)
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

        if ((p?.plan ?: "free") == "free") {
            Text(I18nStore.t("profile.upgradeTitle", "Mejora tu plan"), style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = { vm.openCheckout("basic") { open(it) } },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text(I18nStore.t("profile.goPro", "Hazte Pro — \$9/mes")) }
            OutlinedButton(
                onClick = { vm.openCheckout("pro") { open(it) } },
                modifier = Modifier.fillMaxWidth()
            ) { Text(I18nStore.t("profile.goPremium", "Premium — \$19/mes · voz ilimitada")) }
            Text(
                I18nStore.t("profile.payNote", "🔒 Pago seguro con Stripe · aceptado en todo el mundo · cancela cuando quieras"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Button(
                onClick = { vm.openPortal { open(it) } },
                modifier = Modifier.fillMaxWidth()
            ) { Text(I18nStore.t("profile.manage", "Administrar suscripción")) }
        }

        Divider(Modifier.padding(vertical = 8.dp))

        OutlinedButton(
            onClick = onDiagnostic,
            modifier = Modifier.fillMaxWidth()
        ) { Text(I18nStore.t("profile.levelTest", "🎯 Realiza tu test de nivel")) }

        OutlinedButton(
            onClick = onChangeLanguage,
            modifier = Modifier.fillMaxWidth()
        ) { Text(I18nStore.t("profile.changeLanguage", "Cambiar idioma")) }

        TextButton(
            onClick = { vm.logout(context, onLogout) },
            modifier = Modifier.fillMaxWidth()
        ) { Text(I18nStore.t("profile.logout", "Cerrar sesión")) }

        // ── Badges / logros ───────────────────────────────────────────────────
        if (state.badges.isNotEmpty()) {
            Divider(Modifier.padding(vertical = 4.dp))
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
        Divider(Modifier.padding(vertical = 4.dp))
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
