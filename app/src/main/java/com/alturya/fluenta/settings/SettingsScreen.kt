package com.alturya.fluenta.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alturya.fluenta.audio.Sfx
import com.alturya.fluenta.data.GoalStore
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.data.SettingsStore
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val sfxOn by SettingsStore.sfxEnabled(context).collectAsState(initial = true)
    val hapticsOn by SettingsStore.hapticsEnabled(context).collectAsState(initial = true)
    val remindersOn by SettingsStore.remindersEnabled(context).collectAsState(initial = true)
    val goalXp by GoalStore.flow(context).collectAsState(initial = 50)

    // Permiso de notificaciones (Android 13+): se pide CON CONTEXTO, justo cuando el
    // usuario activa los recordatorios, no de golpe al abrir la app.
    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        scope.launch { SettingsStore.setReminders(context, granted) }
    }
    fun enableReminders() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            scope.launch { SettingsStore.setReminders(context, true) }
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            I18nStore.t("settings.title", "Ajustes"),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))

        // ── Sonido y vibración ────────────────────────────────────────────────
        SectionTitle(I18nStore.t("settings.feedback", "Sonido y vibración"))
        ToggleRow(
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            title = I18nStore.t("settings.sfx", "Efectos de sonido"),
            checked = sfxOn,
            onCheckedChange = {
                scope.launch { SettingsStore.setSfx(context, it) }
                Sfx.enabled = it
            },
        )
        ToggleRow(
            icon = Icons.Default.Vibration,
            title = I18nStore.t("settings.haptics", "Vibración (háptica)"),
            checked = hapticsOn,
            onCheckedChange = { scope.launch { SettingsStore.setHaptics(context, it) } },
        )

        // ── Recordatorios ─────────────────────────────────────────────────────
        SectionTitle(I18nStore.t("settings.reminders", "Recordatorios"))
        ToggleRow(
            icon = Icons.Default.Notifications,
            title = I18nStore.t("settings.dailyReminder", "Recordatorio diario de práctica"),
            checked = remindersOn,
            onCheckedChange = { on ->
                if (on) enableReminders()
                else scope.launch { SettingsStore.setReminders(context, false) }
            },
        )

        // ── Meta diaria ───────────────────────────────────────────────────────
        SectionTitle(I18nStore.t("settings.dailyGoal", "Meta diaria"))
        GoalStore.OPTIONS.forEach { opt ->
            val selected = opt.xp == goalXp
            Card(
                onClick = { scope.launch { GoalStore.set(context, opt.xp) } },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(opt.label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                        Text(opt.description, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (selected) RadioButton(selected = true, onClick = null)
                }
            }
        }

        // ── Legal ─────────────────────────────────────────────────────────────
        SectionTitle(I18nStore.t("settings.legal", "Legal"))
        TextButton(onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://fluenta.alturya.com/privacy")))
        }) { Text(I18nStore.t("settings.privacy", "Política de privacidad")) }
        TextButton(onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://fluenta.alturya.com/terms")))
        }) { Text(I18nStore.t("settings.terms", "Términos de servicio")) }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(I18nStore.t("common.back", "Volver"))
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Spacer(Modifier.height(8.dp))
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun ToggleRow(icon: ImageVector, title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
