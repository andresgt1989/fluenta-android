package com.alturya.fluenta.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Hoot — la mascota búho de Fluenta (kit Claude Design), dibujada con Canvas.
 * Reemplaza el emoji 🦉 marrón genérico en estados vacíos/error para que la marca
 * sea consistente. `sad=true` da las cejas/ojos tristes para errores.
 */
@Composable
fun HootMascot(modifier: Modifier = Modifier, sad: Boolean = false) {
    val teal = Color(0xFF13B0A0); val tealDark = Color(0xFF0A6F64)
    val belly = Color(0xFFF2FBF8); val bellyBot = Color(0xFFCDEEE6)
    Canvas(modifier) {
        val sx = size.width / 200f; val sy = size.height / 200f
        fun p(x: Float, y: Float) = Offset(x * sx, y * sy)
        fun path(b: Path.() -> Unit) = Path().apply(b)
        // tufts
        drawPath(path { moveTo(48f*sx,50f*sy); cubicTo(40f*sx,22f*sy,58f*sx,14f*sy,70f*sx,36f*sy); cubicTo(66f*sx,44f*sy,56f*sx,50f*sy,48f*sx,50f*sy); close() }, tealDark)
        drawPath(path { moveTo(152f*sx,50f*sy); cubicTo(160f*sx,22f*sy,142f*sx,14f*sy,130f*sx,36f*sy); cubicTo(134f*sx,44f*sy,144f*sx,50f*sy,152f*sx,50f*sy); close() }, tealDark)
        // body
        drawPath(path { moveTo(100f*sx,30f*sy); cubicTo(146f*sx,30f*sy,168f*sx,64f*sy,168f*sx,112f*sy); cubicTo(168f*sx,166f*sy,140f*sx,196f*sy,100f*sx,196f*sy); cubicTo(60f*sx,196f*sy,32f*sx,166f*sy,32f*sx,112f*sy); cubicTo(32f*sx,64f*sy,54f*sx,30f*sy,100f*sx,30f*sy); close() },
            Brush.verticalGradient(0f to teal, 1f to tealDark, startY = 30f*sy, endY = 196f*sy))
        // belly
        drawPath(path { moveTo(100f*sx,96f*sy); cubicTo(128f*sx,96f*sy,140f*sx,122f*sy,140f*sx,150f*sy); cubicTo(140f*sx,178f*sy,122f*sx,192f*sy,100f*sx,192f*sy); cubicTo(78f*sx,192f*sy,60f*sx,178f*sy,60f*sx,150f*sy); cubicTo(60f*sx,122f*sy,72f*sx,96f*sy,100f*sx,96f*sy); close() },
            Brush.verticalGradient(0f to belly, 1f to bellyBot, startY = 96f*sy, endY = 192f*sy))
        // face disc
        drawOval(belly, topLeft = p(42f,46f), size = Size(116f*sx,100f*sy))
        // brows (sad = inner-up)
        if (sad) {
            drawPath(path { moveTo(58f*sx,78f*sy); cubicTo(66f*sx,70f*sy,84f*sx,66f*sy,92f*sx,64f*sy) }, tealDark, style = Stroke(width=5f*sx, cap=StrokeCap.Round))
            drawPath(path { moveTo(108f*sx,64f*sy); cubicTo(116f*sx,66f*sy,134f*sx,70f*sy,142f*sx,78f*sy) }, tealDark, style = Stroke(width=5f*sx, cap=StrokeCap.Round))
        } else {
            drawPath(path { moveTo(58f*sx,70f*sy); cubicTo(66f*sx,60f*sy,84f*sx,60f*sy,92f*sx,70f*sy) }, tealDark, style = Stroke(width=5f*sx, cap=StrokeCap.Round))
            drawPath(path { moveTo(108f*sx,70f*sy); cubicTo(116f*sx,60f*sy,134f*sx,60f*sy,142f*sx,70f*sy) }, tealDark, style = Stroke(width=5f*sx, cap=StrokeCap.Round))
        }
        // eyes
        for (cx in listOf(76f,124f)) drawCircle(Color.White, 24f*sx, p(cx,98f))
        drawCircle(tealDark, 12f*sx, p(80f,100f)); drawCircle(Color.White, 4f*sx, p(84f,96f))
        drawCircle(tealDark, 12f*sx, p(120f,100f)); drawCircle(Color.White, 4f*sx, p(124f,96f))
        // beak
        drawPath(path { moveTo(100f*sx,112f*sy); lineTo(91f*sx,124f*sy); cubicTo(95f*sx,129f*sy,105f*sx,129f*sy,109f*sx,124f*sy); close() }, Color(0xFFF6A623))
    }
}
