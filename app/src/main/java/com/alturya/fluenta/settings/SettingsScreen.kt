package com.alturya.fluenta.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alturya.fluenta.audio.Sfx
import com.alturya.fluenta.data.GoalStore
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.data.InterfaceLang
import com.alturya.fluenta.data.InterfaceLanguages
import com.alturya.fluenta.data.SettingsStore
import com.alturya.fluenta.reminder.ReminderScheduler
import com.alturya.fluenta.network.ApiClient
import kotlinx.coroutines.launch

private object Sz {
    val BgTop = Color(0xFFF2FBF8)
    val BgBot = Color(0xFFE4F6F1)
    val Teal = Color(0xFF0E9D8E)
    val TealDark = Color(0xFF0A6F64)
    val Mint = Color(0xFFCDEEE6)
    val Muted = Color(0xFF8A968F)
    val RowInk = Color(0xFF143F3A)
    val AmberBox = Color(0xFFFBE7C6)
}

@Composable
fun SettingsScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val sfxOn by SettingsStore.sfxEnabled(context).collectAsState(initial = true)
    val hapticsOn by SettingsStore.hapticsEnabled(context).collectAsState(initial = true)
    val remindersOn by SettingsStore.remindersEnabled(context).collectAsState(initial = true)
    val goalXp by GoalStore.flow(context).collectAsState(initial = 50)
    val uiLang by I18nStore.currentLangFlow(context).collectAsState(initial = "es")

    var languages by remember { mutableStateOf(InterfaceLanguages.SUPPORTED) }
    LaunchedEffect(Unit) {
        runCatching { ApiClient.api.getInterfaceLanguages() }
            .getOrNull()?.languages
            ?.mapNotNull { dto ->
                dto.code?.lowercase()?.take(2)?.takeIf { it.length == 2 }?.let { code ->
                    val known = InterfaceLanguages.byCode(code)
                    InterfaceLang(code = code,
                        nativeName = dto.nativeName?.takeIf { it.isNotBlank() } ?: known?.nativeName ?: code,
                        flag = dto.flag?.takeIf { it.isNotBlank() } ?: known?.flag ?: "🌐")
                }
            }?.takeIf { it.isNotEmpty() }?.let { languages = it }
    }

    val notifPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        scope.launch { SettingsStore.setReminders(context, granted) }
        if (granted) ReminderScheduler.schedule(context)
    }
    fun enableReminders() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            scope.launch { SettingsStore.setReminders(context, true) }
            ReminderScheduler.schedule(context)
        }
    }

    var langExpanded by remember { mutableStateOf(false) }
    var goalExpanded by remember { mutableStateOf(false) }
    val curLang = languages.firstOrNull { it.code == uiLang.lowercase().take(2) }
    val curGoal = GoalStore.OPTIONS.firstOrNull { it.xp == goalXp }

    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Sz.BgTop, 1f to Sz.BgBot))
            .verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(I18nStore.t("settings.title", "Ajustes"), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Sz.TealDark,
            modifier = Modifier.semantics { heading() }.padding(start = 4.dp, bottom = 6.dp))

        // ── Preferencias ──
        SectionLabel(I18nStore.t("settings.prefs", "Preferencias"))
        SectionCard {
            ValueRow(Icons.Default.Language, Sz.Mint, I18nStore.t("settings.appLanguage", "Idioma de la app"),
                value = curLang?.let { "${it.flag} ${it.nativeName}" } ?: uiLang.uppercase(),
                onClick = { langExpanded = !langExpanded })
            if (langExpanded) {
                languages.forEach { lang ->
                    val selected = lang.code == uiLang.lowercase().take(2)
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .clickable {
                            if (!selected) scope.launch { I18nStore.setLang(context, lang.code); I18nStore.ensureLoaded(context, lang.code) }
                            langExpanded = false
                        }.padding(horizontal = 56.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(lang.flag, fontSize = 16.sp)
                        Text(lang.nativeName, fontSize = 14.sp, fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (selected) Sz.TealDark else Sz.RowInk, modifier = Modifier.weight(1f))
                        if (selected) Text("✓", color = Sz.Teal, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
            Divider(color = Sz.Mint.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 12.dp))
            ValueRow(Icons.Default.TrackChanges, Sz.AmberBox, I18nStore.t("settings.dailyGoal", "Meta diaria"),
                value = curGoal?.label ?: "$goalXp XP", onClick = { goalExpanded = !goalExpanded })
            if (goalExpanded) {
                GoalStore.OPTIONS.forEach { opt ->
                    val selected = opt.xp == goalXp
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .clickable { scope.launch { GoalStore.set(context, opt.xp) }; goalExpanded = false }
                        .padding(horizontal = 56.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(opt.label, fontSize = 14.sp, fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium, color = if (selected) Sz.TealDark else Sz.RowInk)
                            Text(opt.description, fontSize = 11.sp, color = Sz.Muted)
                        }
                        if (selected) Text("✓", color = Sz.Teal, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
            Divider(color = Sz.Mint.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 12.dp))
            ToggleRow(Icons.Default.Notifications, Sz.Mint, I18nStore.t("settings.dailyReminder", "Recordatorio diario"), remindersOn) { on ->
                if (on) enableReminders() else { scope.launch { SettingsStore.setReminders(context, false) }; ReminderScheduler.cancel(context) }
            }
            Divider(color = Sz.Mint.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 12.dp))
            ToggleRow(Icons.AutoMirrored.Filled.VolumeUp, Sz.Mint, I18nStore.t("settings.sfx", "Efectos de sonido"), sfxOn) {
                scope.launch { SettingsStore.setSfx(context, it) }; Sfx.enabled = it
            }
            Divider(color = Sz.Mint.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 12.dp))
            ToggleRow(Icons.Default.Vibration, Sz.Mint, I18nStore.t("settings.haptics", "Vibración"), hapticsOn) {
                scope.launch { SettingsStore.setHaptics(context, it) }
            }
        }

        // ── Legal ──
        SectionLabel(I18nStore.t("settings.legal", "Legal"))
        SectionCard {
            ValueRow(Icons.Default.Shield, Sz.Mint, I18nStore.t("settings.privacy", "Política de privacidad"), value = "",
                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://fluenta.alturya.com/privacy"))) })
            Divider(color = Sz.Mint.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 12.dp))
            ValueRow(Icons.Default.Shield, Sz.Mint, I18nStore.t("settings.terms", "Términos de servicio"), value = "",
                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://fluenta.alturya.com/terms"))) })
        }

        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(14.dp)).clickable(onClick = onBack).padding(12.dp), horizontalArrangement = Arrangement.Center) {
            Text(I18nStore.t("common.back", "Volver"), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Sz.TealDark)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, color = Sz.Teal,
        modifier = Modifier.semantics { heading() }.padding(start = 6.dp, top = 10.dp, bottom = 6.dp))
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = Color.White, shadowElevation = 3.dp) {
        Column(Modifier.padding(vertical = 4.dp), content = content)
    }
}

@Composable
private fun ValueRow(icon: ImageVector, tint: Color, label: String, value: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        IconBox(icon, tint)
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Sz.RowInk, modifier = Modifier.weight(1f))
        if (value.isNotBlank()) Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Sz.TealDark)
        Text("›", fontSize = 22.sp, color = Sz.Muted)
    }
}

@Composable
private fun ToggleRow(icon: ImageVector, tint: Color, label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        IconBox(icon, tint)
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Sz.RowInk, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Sz.Teal, uncheckedTrackColor = Color(0xFFCBD8D2), uncheckedBorderColor = Color.Transparent))
    }
}

@Composable
private fun IconBox(icon: ImageVector, tint: Color) {
    Box(Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(tint.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = Sz.TealDark, modifier = Modifier.size(22.dp))
    }
}
