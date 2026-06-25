package com.alturya.fluenta.gamification

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alturya.fluenta.data.GoalStore
import com.alturya.fluenta.data.I18nStore
import kotlinx.coroutines.launch

// Pantalla "Meta de hoy" (handoff "Fluenta Gamificación" · estado 2): anillo de
// progreso de la meta diaria de XP + editor de meta. Tokens 1:1 del kit.
// Datos REALES: todayXp (nav-arg desde HOME) + meta diaria de GoalStore (editable).
// El desglose por actividad y la barra de nivel del mock NO se incluyen porque el
// backend aún no expone esos datos (no se inventan): se añadirán cuando existan.
private val GgTeal = Color(0xFF0E9D8E)
private val GgTealDark = Color(0xFF0A6F64)
private val GgInk = Color(0xFF15201D)
private val GgMuted = Color(0xFF5C6562)
private val GgFaint = Color(0xFF9AA39E)
private val GgAmber = Color(0xFFE08A00)
private val GgTrack = Color(0xFFE7EEEB)
private val GgChipBorder = Color(0xFFDCE5E2)

@Composable
fun DailyGoalScreen(
    todayXp: Int,
    onBack: () -> Unit = {},
    onContinue: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val goalXp by GoalStore.flow(context).collectAsState(initial = 50)
    val pct = if (goalXp > 0) (todayXp.toFloat() / goalXp).coerceIn(0f, 1f) else 0f
    val remain = (goalXp - todayXp).coerceAtLeast(0)
    val met = todayXp >= goalXp

    Column(Modifier.fillMaxSize().background(Color(0xFFFBFCFB))) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = I18nStore.t("common.back", "Atrás"),
                tint = GgFaint,
                modifier = Modifier.size(24.dp).clickable { onBack() },
            )
            Spacer(Modifier.width(8.dp))
            Text(I18nStore.t("goal.today.title", "Meta de hoy"), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = GgInk)
        }

        Column(
            Modifier.fillMaxWidth().weight(1f).padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(10.dp))
            // Anillo de progreso (track gris + arco teal = pct).
            Box(Modifier.size(186.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 17.dp.toPx()
                    val inset = stroke / 2
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    val topLeft = Offset(inset, inset)
                    drawArc(color = GgTrack, startAngle = 0f, sweepAngle = 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(width = stroke))
                    drawArc(color = GgTeal, startAngle = -90f, sweepAngle = 360f * pct, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(width = stroke, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = GgAmber, modifier = Modifier.size(26.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$todayXp", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = GgInk)
                        Text("/$goalXp", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GgFaint)
                    }
                    Text(I18nStore.t("goal.today.xp", "XP de hoy"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GgMuted)
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                if (met) I18nStore.t("goal.today.met", "¡Meta diaria cumplida! Tu racha sigue viva.")
                else I18nStore.t("goal.today.remain", "Te faltan {n} XP · ≈ 1 lección").replace("{n}", "$remain"),
                fontSize = 13.5.sp, color = GgMuted,
            )
            Spacer(Modifier.height(24.dp))

            Text(I18nStore.t("goal.today.editTitle", "TU META DIARIA"), Modifier.fillMaxWidth(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B746F))
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GoalStore.OPTIONS.forEach { opt ->
                    val active = opt.xp == goalXp
                    Box(
                        Modifier.weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (active) Color(0xFFEAF7F4) else Color.White)
                            .border(if (active) 1.5.dp else 1.dp, if (active) GgTeal else GgChipBorder, RoundedCornerShape(14.dp))
                            .clickable { scope.launch { GoalStore.set(context, opt.xp) } }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${opt.xp}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = if (active) GgTealDark else GgInk)
                            Text("XP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GgMuted)
                        }
                    }
                }
            }
        }

        // CTA 3D del kit.
        Box(Modifier.padding(18.dp).fillMaxWidth().height(54.dp)) {
            Box(Modifier.matchParentSize().padding(top = 4.dp).clip(RoundedCornerShape(16.dp)).background(GgTealDark))
            Box(
                Modifier.fillMaxWidth().fillMaxHeight().padding(bottom = 4.dp).clip(RoundedCornerShape(16.dp)).background(GgTeal).clickable { onContinue() },
                contentAlignment = Alignment.Center,
            ) {
                Text(I18nStore.t("goal.today.cta", "Seguir aprendiendo"), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}
