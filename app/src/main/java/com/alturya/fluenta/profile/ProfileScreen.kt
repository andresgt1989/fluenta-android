package com.alturya.fluenta.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.network.Badge
import com.alturya.fluenta.util.flag
import com.alturya.fluenta.util.langName
import com.alturya.fluenta.util.levelLabel
import com.alturya.fluenta.util.levelSystemName

/* Paleta del kit Claude Design (Fluenta Perfil y Ajustes.dc.html) */
private object Pf {
    val BgTop = Color(0xFFF2FBF8)
    val BgBot = Color(0xFFE4F6F1)
    val Teal = Color(0xFF0E9D8E)
    val TealDark = Color(0xFF0A6F64)
    val Teal2 = Color(0xFF13B0A0)
    val Mint = Color(0xFFCDEEE6)
    val Muted = Color(0xFF7C857F)
    val Amber = Color(0xFFC77A12)
    val AmberBox = Color(0xFFFBE7C6)
    val RowInk = Color(0xFF143F3A)
    val Danger = Color(0xFFC0492F)
    val DangerBox = Color(0xFFF7E2DC)
}

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

    fun open(url: String) { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }

    val bg = Brush.verticalGradient(0f to Pf.BgTop, 1f to Pf.BgBot)

    if (state.loading) {
        Box(Modifier.fillMaxSize().background(bg), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Pf.Teal, trackColor = Pf.Mint)
        }
        return
    }
    if (state.error) {
        Column(Modifier.fillMaxSize().background(bg).padding(36.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🦉", fontSize = 80.sp)
            Spacer(Modifier.height(16.dp))
            Text(I18nStore.t("profile.loadError", "No pudimos cargar tu perfil. Revisa tu conexión."), fontSize = 16.sp, color = Pf.TealDark, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            Hard3dPf(Modifier.fillMaxWidth(), onClick = { vm.load() }) { Text(I18nStore.t("common.retry", "Reintentar"), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }
        return
    }

    val p = state.profile
    Column(
        modifier = Modifier.fillMaxSize().background(bg).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(I18nStore.t("profile.title", "Perfil"), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Pf.TealDark,
            modifier = Modifier.semantics { heading() }.padding(start = 4.dp))

        // ── Hero: avatar Hoot + nombre + nivel ──
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(start = 4.dp)) {
            Box(Modifier.size(88.dp).clip(RoundedCornerShape(44.dp)).background(Brush.radialGradient(listOf(Pf.BgBot, Pf.Mint))).border(3.dp, Color.White, RoundedCornerShape(44.dp)), contentAlignment = Alignment.Center) {
                HootAvatar(Modifier.size(64.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(p?.phone?.takeIf { it.isNotBlank() } ?: I18nStore.t("profile.learner", "Aprendiz Fluenta"),
                    fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Pf.TealDark, maxLines = 1)
                Spacer(Modifier.height(6.dp))
                Surface(shape = RoundedCornerShape(999.dp), color = Pf.Mint) {
                    Text("${I18nStore.t("profile.level", "Nivel")} ${levelLabel(p?.level, p?.levelSystem)} · ${levelSystemName(p?.levelSystem)}",
                        fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Pf.TealDark, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                }
            }
        }

        // ── 3 stats ──
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatPf("🔥", "${p?.streakDays ?: 0}", I18nStore.t("home.streak", "Racha"), Pf.Amber, Modifier.weight(1f))
            StatPf("⚡", "${p?.totalXp ?: 0}", I18nStore.t("home.xp", "XP total"), Pf.TealDark, Modifier.weight(1f))
            StatPf("🏆", levelLabel(p?.level, p?.levelSystem), I18nStore.t("profile.levelShort", "Nivel"), Pf.TealDark, Modifier.weight(1f))
        }

        // ── Tarjeta idioma de aprendizaje ──
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(listOf(Pf.Teal, Pf.TealDark))).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.size(52.dp).clip(RoundedCornerShape(15.dp)).background(Color.White.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                Text(flag(p?.l2), fontSize = 26.sp)
            }
            Column(Modifier.weight(1f)) {
                Text(I18nStore.t("profile.learningLabel", "Estás aprendiendo"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Pf.Mint)
                Text(langName(p?.l2), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text("${langName(p?.l1)} → ${langName(p?.l2)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Pf.Mint)
            }
            Surface(shape = RoundedCornerShape(12.dp), color = Color.White, modifier = Modifier.clickable(onClick = onChangeLanguage)) {
                Text(I18nStore.t("profile.changeShort", "Cambiar"), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Pf.TealDark, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
            }
        }

        // ── Compartir logro (viralidad) ──
        if (p != null) {
            AchievementShareCard(l2 = p.l2, level = p.level, levelSystem = p.levelSystem, streakDays = p.streakDays ?: 0)
        }

        // ── PRO / suscripción ──
        if ((p?.plan ?: "free") == "free") {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(listOf(Pf.Teal2, Pf.TealDark))).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) { Text("👑", fontSize = 24.sp) }
                Column(Modifier.weight(1f)) {
                    Text(I18nStore.t("profile.proTitle", "Fluenta Pro"), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text(I18nStore.t("profile.proSub", "Lecciones sin límite y sin anuncios"), fontSize = 13.sp, color = Pf.Mint)
                }
                Surface(shape = RoundedCornerShape(13.dp), color = Color.White, modifier = Modifier.clickable { vm.openCheckout("basic") { open(it) } }) {
                    Text(I18nStore.t("profile.proCta", "Hazte Pro"), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Pf.TealDark, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Pf.Muted, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text(I18nStore.t("profile.payNote", "Pago seguro con Stripe · cancela cuando quieras"), fontSize = 11.sp, color = Pf.Muted)
            }
        } else {
            CardRowPf("⚙️", I18nStore.t("profile.manage", "Administrar suscripción"), Pf.Mint, onClick = { vm.openPortal { open(it) } })
        }

        // ── Acciones (rows kit) ──
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = Color.White, shadowElevation = 2.dp) {
            Column(Modifier.padding(4.dp)) {
                RowPf(Icons.Default.GpsFixed, I18nStore.t("profile.levelTest", "Test de nivel"), onClick = onDiagnostic)
                RowPf(Icons.Default.Settings, I18nStore.t("profile.settings", "Ajustes"), onClick = onSettings)
            }
        }

        // ── Logros / badges ──
        if (state.badges.isNotEmpty()) {
            Text("${I18nStore.t("profile.badges", "Logros")} · ${state.badgesEarned}/${state.badgesTotal}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Pf.TealDark, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                state.badges.forEach { badge -> BadgeChip(badge) }
            }
        }

        ReferralCard()

        // ── Cerrar sesión ──
        Surface(Modifier.fillMaxWidth().clickable { vm.logout(context, onLogout) }, shape = RoundedCornerShape(18.dp), color = Color.White, shadowElevation = 2.dp) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(Pf.DangerBox), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Logout, contentDescription = null, tint = Pf.Danger, modifier = Modifier.size(22.dp))
                }
                Text(I18nStore.t("profile.logout", "Cerrar sesión"), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Pf.Danger)
            }
        }
    }
}

@Composable
private fun StatPf(emoji: String, value: String, label: String, valueColor: Color, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(18.dp), color = Color.White, shadowElevation = 3.dp) {
        Column(Modifier.padding(vertical = 14.dp, horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(Pf.Mint.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) { Text(emoji, fontSize = 20.sp) }
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = valueColor, maxLines = 1, modifier = Modifier.padding(top = 8.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Pf.Muted)
        }
    }
}

@Composable
private fun RowPf(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(Pf.Mint.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Pf.TealDark, modifier = Modifier.size(22.dp))
        }
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Pf.RowInk, modifier = Modifier.weight(1f))
        Text("›", fontSize = 22.sp, color = Pf.Muted)
    }
}

@Composable
private fun CardRowPf(emoji: String, label: String, tint: Color, onClick: () -> Unit) {
    Surface(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(18.dp), color = Color.White, shadowElevation = 2.dp) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(tint.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) { Text(emoji, fontSize = 20.sp) }
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Pf.RowInk, modifier = Modifier.weight(1f))
            Text("›", fontSize = 22.sp, color = Pf.Muted)
        }
    }
}

@Composable
private fun Hard3dPf(modifier: Modifier = Modifier, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(modifier.height(56.dp)) {
        Box(Modifier.fillMaxWidth().height(52.dp).align(Alignment.BottomCenter).clip(RoundedCornerShape(18.dp)).background(Pf.TealDark))
        Box(Modifier.fillMaxWidth().height(52.dp).align(Alignment.TopCenter).clip(RoundedCornerShape(18.dp)).background(Pf.Teal).clickable(onClick = onClick), contentAlignment = Alignment.Center) { content() }
    }
}

/** Búho Hoot compacto para el avatar (port del SVG del kit). */
@Composable
private fun HootAvatar(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val sx = size.width / 200f; val sy = size.height / 200f
        fun p(x: Float, y: Float) = Offset(x * sx, y * sy)
        fun path(b: Path.() -> Unit) = Path().apply(b)
        // tufts
        drawPath(path { moveTo(48f*sx,50f*sy); cubicTo(40f*sx,22f*sy,58f*sx,14f*sy,70f*sx,36f*sy); cubicTo(66f*sx,44f*sy,56f*sx,50f*sy,48f*sx,50f*sy); close() }, Pf.TealDark)
        drawPath(path { moveTo(152f*sx,50f*sy); cubicTo(160f*sx,22f*sy,142f*sx,14f*sy,130f*sx,36f*sy); cubicTo(134f*sx,44f*sy,144f*sx,50f*sy,152f*sx,50f*sy); close() }, Pf.TealDark)
        // body
        drawPath(path { moveTo(100f*sx,30f*sy); cubicTo(146f*sx,30f*sy,168f*sx,64f*sy,168f*sx,112f*sy); cubicTo(168f*sx,166f*sy,140f*sx,196f*sy,100f*sx,196f*sy); cubicTo(60f*sx,196f*sy,32f*sx,166f*sy,32f*sx,112f*sy); cubicTo(32f*sx,64f*sy,54f*sx,30f*sy,100f*sx,30f*sy); close() },
            Brush.verticalGradient(0f to Pf.Teal2, 1f to Pf.TealDark, startY = 30f*sy, endY = 196f*sy))
        // belly
        drawPath(path { moveTo(100f*sx,96f*sy); cubicTo(128f*sx,96f*sy,140f*sx,122f*sy,140f*sx,150f*sy); cubicTo(140f*sx,178f*sy,122f*sx,192f*sy,100f*sx,192f*sy); cubicTo(78f*sx,192f*sy,60f*sx,178f*sy,60f*sx,150f*sy); cubicTo(60f*sx,122f*sy,72f*sx,96f*sy,100f*sx,96f*sy); close() },
            Brush.verticalGradient(0f to Color(0xFFF2FBF8), 1f to Pf.Mint, startY = 96f*sy, endY = 192f*sy))
        // face disc
        drawOval(Color(0xFFF2FBF8), topLeft = p(42f,46f), size = Size(116f*sx,100f*sy))
        // brows
        drawPath(path { moveTo(58f*sx,70f*sy); cubicTo(66f*sx,60f*sy,84f*sx,60f*sy,92f*sx,70f*sy) }, Pf.TealDark, style = Stroke(width=5f*sx, cap=StrokeCap.Round))
        drawPath(path { moveTo(108f*sx,70f*sy); cubicTo(116f*sx,60f*sy,134f*sx,60f*sy,142f*sx,70f*sy) }, Pf.TealDark, style = Stroke(width=5f*sx, cap=StrokeCap.Round))
        // eyes
        for (cx in listOf(76f,124f)) drawCircle(Color.White, 24f*sx, p(cx,98f))
        drawCircle(Pf.TealDark, 12f*sx, p(80f,100f)); drawCircle(Color.White, 4f*sx, p(84f,96f))
        drawCircle(Pf.TealDark, 12f*sx, p(120f,100f)); drawCircle(Color.White, 4f*sx, p(124f,96f))
        // beak
        drawPath(path { moveTo(100f*sx,112f*sy); lineTo(91f*sx,124f*sy); cubicTo(95f*sx,129f*sy,105f*sx,129f*sy,109f*sx,124f*sy); close() }, Color(0xFFF6A623))
    }
}

@Composable
private fun BadgeChip(badge: Badge) {
    val earned = badge.earned
    Surface(
        color = if (earned) Pf.Mint else Color.White,
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).widthIn(min = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(badge.icon, fontSize = 22.sp, color = if (earned) Color.Unspecified else Color.Gray)
            Spacer(Modifier.height(2.dp))
            Text(badge.title, fontSize = 11.sp, textAlign = TextAlign.Center,
                color = if (earned) Pf.TealDark else Pf.Muted, maxLines = 2, fontWeight = FontWeight.SemiBold)
        }
    }
}
