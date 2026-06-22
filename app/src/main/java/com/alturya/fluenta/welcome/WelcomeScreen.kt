package com.alturya.fluenta.welcome

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import com.alturya.fluenta.data.I18nStore

private object Wz {
    val GradTop = Color(0xFFE4F6F1)
    val GradBottom = Color(0xFFCDEEE6)
    val Teal = Color(0xFF0E9D8E)
    val TealDark = Color(0xFF0A6F64)
    val BodyTop = Color(0xFF13B0A0)
    val BellyTop = Color(0xFFF2FBF8)
    val FaceDisc = Color(0xFFF2FBF8)
    val Amber = Color(0xFFF6A623)
    val ValueInk = Color(0xFF13524B)
}

@Composable
fun WelcomeScreen(onStart: () -> Unit, onLogin: () -> Unit) {
    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(0f to Wz.GradTop, 0.38f to Wz.GradTop, 1f to Wz.GradBottom)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ── HERO: mascota + wordmark + value prop ──
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val transition = rememberInfiniteTransition(label = "hoot")
            val bob by transition.animateFloat(
                initialValue = 0f, targetValue = -12f,
                animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "bob",
            )
            Box(Modifier.size(230.dp), contentAlignment = Alignment.Center) {
                HootOwl(Modifier.size(210.dp).offset(y = bob.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                I18nStore.t("welcome.brand", "Fluenta"),
                fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = Wz.TealDark,
                letterSpacing = (-1).sp,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                I18nStore.t("welcome.valueProp", "Habla un idioma nuevo desde el primer minuto con tu tutor de IA"),
                fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Wz.ValueInk,
                textAlign = TextAlign.Center, lineHeight = 26.sp,
                modifier = Modifier.widthIn(max = 340.dp),
            )
        }

        // ── ACCIONES ──
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 28.dp).padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // CTA 3D
            val depth = 5.dp
            Box(Modifier.fillMaxWidth().widthIn(max = 340.dp).height(56.dp + depth)) {
                Box(Modifier.fillMaxWidth().height(56.dp).align(Alignment.BottomCenter).clip(RoundedCornerShape(18.dp)).background(Wz.TealDark))
                Box(
                    Modifier.fillMaxWidth().height(56.dp).align(Alignment.TopCenter)
                        .clip(RoundedCornerShape(18.dp)).background(Wz.Teal).clickable(onClick = onStart),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(I18nStore.t("common.start", "Empezar"), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.2.sp)
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(
                Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onLogin).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(I18nStore.t("welcome.haveAccount", "Ya tengo cuenta ·"), color = Wz.TealDark.copy(alpha = 0.7f), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(I18nStore.t("welcome.login", "Entrar"), color = Wz.TealDark, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** Hoot — el búho mascota, portado del SVG de "Fluenta Bienvenida.dc.html" (viewBox 200×210). */
@Composable
private fun HootOwl(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val sx = size.width / 200f
        val sy = size.height / 210f
        fun p(x: Float, y: Float) = Offset(x * sx, y * sy)
        fun path(block: Path.() -> Unit) = Path().apply(block)

        // ground shadow
        drawOval(
            color = Wz.TealDark.copy(alpha = 0.18f),
            topLeft = p(30f, 188f), size = Size(140f * sx, 22f * sy),
        )

        // ear tufts
        drawPath(path {
            moveTo(48f * sx, 50f * sy)
            cubicTo(40f * sx, 22f * sy, 58f * sx, 14f * sy, 70f * sx, 36f * sy)
            cubicTo(66f * sx, 44f * sy, 56f * sx, 50f * sy, 48f * sx, 50f * sy)
            close()
        }, Wz.TealDark)
        drawPath(path {
            moveTo(152f * sx, 50f * sy)
            cubicTo(160f * sx, 22f * sy, 142f * sx, 14f * sy, 130f * sx, 36f * sy)
            cubicTo(134f * sx, 44f * sy, 144f * sx, 50f * sy, 152f * sx, 50f * sy)
            close()
        }, Wz.TealDark)

        // body (gradient)
        val body = path {
            moveTo(100f * sx, 30f * sy)
            cubicTo(146f * sx, 30f * sy, 168f * sx, 64f * sy, 168f * sx, 112f * sy)
            cubicTo(168f * sx, 166f * sy, 140f * sx, 196f * sy, 100f * sx, 196f * sy)
            cubicTo(60f * sx, 196f * sy, 32f * sx, 166f * sy, 32f * sx, 112f * sy)
            cubicTo(32f * sx, 64f * sy, 54f * sx, 30f * sy, 100f * sx, 30f * sy)
            close()
        }
        drawPath(body, Brush.verticalGradient(0f to Wz.BodyTop, 1f to Wz.TealDark, startY = 30f * sy, endY = 196f * sy))

        // wings
        drawPath(path {
            moveTo(36f * sx, 104f * sy)
            cubicTo(26f * sx, 120f * sy, 28f * sx, 156f * sy, 44f * sx, 176f * sy)
            cubicTo(48f * sx, 150f * sy, 46f * sx, 122f * sy, 44f * sx, 104f * sy)
            close()
        }, Wz.TealDark.copy(alpha = 0.55f))
        drawPath(path {
            moveTo(164f * sx, 104f * sy)
            cubicTo(174f * sx, 120f * sy, 172f * sx, 156f * sy, 156f * sx, 176f * sy)
            cubicTo(152f * sx, 150f * sy, 154f * sx, 122f * sy, 156f * sx, 104f * sy)
            close()
        }, Wz.TealDark.copy(alpha = 0.55f))

        // belly
        drawPath(path {
            moveTo(100f * sx, 96f * sy)
            cubicTo(128f * sx, 96f * sy, 140f * sx, 122f * sy, 140f * sx, 150f * sy)
            cubicTo(140f * sx, 178f * sy, 122f * sx, 192f * sy, 100f * sx, 192f * sy)
            cubicTo(78f * sx, 192f * sy, 60f * sx, 178f * sy, 60f * sx, 150f * sy)
            cubicTo(60f * sx, 122f * sy, 72f * sx, 96f * sy, 100f * sx, 96f * sy)
            close()
        }, Brush.verticalGradient(0f to Wz.BellyTop, 1f to Wz.GradBottom, startY = 96f * sy, endY = 192f * sy))

        // face disc
        drawOval(Wz.FaceDisc, topLeft = p(42f, 46f), size = Size(116f * sx, 100f * sy))

        // brows
        drawPath(path {
            moveTo(58f * sx, 70f * sy)
            cubicTo(66f * sx, 60f * sy, 84f * sx, 60f * sy, 92f * sx, 70f * sy)
        }, Wz.TealDark, style = Stroke(width = 5f * sx, cap = StrokeCap.Round))
        drawPath(path {
            moveTo(108f * sx, 70f * sy)
            cubicTo(116f * sx, 60f * sy, 134f * sx, 60f * sy, 142f * sx, 70f * sy)
        }, Wz.TealDark, style = Stroke(width = 5f * sx, cap = StrokeCap.Round))

        // eyes (left + right)
        for (cx in listOf(76f, 124f)) {
            drawCircle(Color.White, radius = 24f * sx, center = p(cx, 98f))
            drawCircle(Wz.TealDark.copy(alpha = 0.25f), radius = 24f * sx, center = p(cx, 98f), style = Stroke(width = 3f * sx))
        }
        // pupils + highlight
        drawCircle(Wz.TealDark, radius = 12f * sx, center = p(80f, 100f))
        drawCircle(Color.White, radius = 4f * sx, center = p(84f, 96f))
        drawCircle(Wz.TealDark, radius = 12f * sx, center = p(120f, 100f))
        drawCircle(Color.White, radius = 4f * sx, center = p(124f, 96f))

        // beak
        drawPath(path {
            moveTo(100f * sx, 112f * sy)
            lineTo(91f * sx, 124f * sy)
            cubicTo(95f * sx, 129f * sy, 105f * sx, 129f * sy, 109f * sx, 124f * sy)
            close()
        }, Wz.Amber, style = Fill)

        // feet
        for (fx in listOf(84f, 116f)) {
            drawLine(Wz.Amber, p(fx, 194f), p(fx, 202f), strokeWidth = 5f * sx, cap = StrokeCap.Round)
            drawLine(Wz.Amber, p(fx - 6f, 202f), p(fx + 6f, 202f), strokeWidth = 5f * sx, cap = StrokeCap.Round)
        }
    }
}
