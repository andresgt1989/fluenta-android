package com.alturya.fluenta.gamification

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
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

// Tokens del kit (mismos teal/ámbar que DailyGoalScreen y StreakCelebration).
private val GoalGradTop = Color(0xFF0A6F64)
private val GoalGradBottom = Color(0xFF0E9D8E)
private val GoalCheck = Color(0xFF0E9D8E)
private val GoalAmber = Color(0xFFFFE9C2)

/**
 * Celebración "¡Meta diaria cumplida!" (handoff "Fluenta Gamificación" · estado 4):
 * overlay a pantalla completa que aparece UNA vez, en la lección que cruza la meta
 * diaria de XP. El backend (T5) marca el cruce vía `dailyGoalMet`/`todayXp` en la
 * respuesta del submit; el flanco "se cumplió justo ahora" se detecta en ResultView.
 * [todayXp]/[goalXp] son los valores REALES del servidor (no se inventan).
 */
@Composable
fun DailyGoalCelebrationSheet(
    todayXp: Int,
    goalXp: Int,
    onClose: () -> Unit,
) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(GoalGradTop, GoalGradBottom))),
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
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GoalCheck, modifier = Modifier.size(64.dp))
                }
                Spacer(Modifier.height(22.dp))
                Text(
                    I18nStore.t("goal.celebrate.title", "¡Meta diaria cumplida!"),
                    color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    I18nStore.t("goal.celebrate.sub", "Tu racha sigue viva. Vuelve mañana para mantenerla."),
                    color = Color.White.copy(alpha = 0.82f), fontSize = 14.5.sp, textAlign = TextAlign.Center,
                )
                if (goalXp > 0) {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color.White.copy(alpha = 0.18f)).padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = GoalAmber, modifier = Modifier.size(18.dp))
                        Text("$todayXp/$goalXp XP", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            Box(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 22.dp).padding(bottom = 30.dp),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).clickable { onClose() }.padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(I18nStore.t("goal.celebrate.continue", "¡Seguir!"), color = GoalGradTop, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}
