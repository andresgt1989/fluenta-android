package com.alturya.fluenta.gamification

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.alturya.fluenta.data.I18nStore

/**
 * ¿El día de racha [day] es un hito celebrable que aún no se mostró?
 * Reusa [Streak.isMilestone] (mismo set que "próximo hito"): la celebración a
 * pantalla completa (handoff "Fluenta Gamificación" · pantalla 3) solo aparece en
 * HITOS y una sola vez por hito ([lastCelebrated] = mayor hito ya celebrado).
 */
fun isStreakMilestone(day: Int, lastCelebrated: Int): Boolean =
    day > lastCelebrated && Streak.isMilestone(day)

// Tokens del kit (gradiente teal del handoff).
private val GradTop = Color(0xFF0A6F64)
private val GradBottom = Color(0xFF0E9D8E)
private val FlameAmber = Color(0xFFE08A00)
private val BonusAmber = Color(0xFFFFE9C2)

/**
 * Celebración de racha extendida (pantalla 3 del handoff): overlay a pantalla
 * completa con gradiente teal, llama, el número de días y un CTA "¡Seguir!".
 * `xpEarned` = XP REAL de la lección que disparó el hito (no inventamos bonus).
 */
@Composable
fun StreakCelebrationSheet(
    days: Int,
    xpEarned: Int,
    onClose: () -> Unit,
    onShare: () -> Unit = {},
) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(GradTop, GradBottom))),
        ) {
            Text(
                "✕",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 24.sp,
                modifier = Modifier.align(Alignment.TopEnd).padding(20.dp).clickable { onClose() },
            )

            Column(
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier.size(118.dp).clip(CircleShape).background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = FlameAmber, modifier = Modifier.size(64.dp))
                }
                Spacer(Modifier.height(22.dp))
                Text("$days", color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(6.dp))
                Text(
                    I18nStore.t("streak.celebrate.title", "¡Racha de {n} días!").replace("{n}", "$days"),
                    color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    I18nStore.t("streak.celebrate.sub", "Sigue así. Vuelve mañana para no romperla."),
                    color = Color.White.copy(alpha = 0.82f), fontSize = 14.5.sp, textAlign = TextAlign.Center,
                )
                if (xpEarned > 0) {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color.White.copy(alpha = 0.18f)).padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = BonusAmber, modifier = Modifier.size(18.dp))
                        Text("+$xpEarned XP", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            Column(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 22.dp).padding(bottom = 30.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).clickable { onClose() }.padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(I18nStore.t("streak.celebrate.continue", "¡Seguir!"), color = GradTop, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
                Box(
                    modifier = Modifier.fillMaxWidth().clickable { onShare() }.padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(I18nStore.t("streak.celebrate.share", "Compartir"), color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
