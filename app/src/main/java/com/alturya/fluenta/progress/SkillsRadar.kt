package com.alturya.fluenta.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alturya.fluenta.network.Skill
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun SkillsRadar(skills: List<Skill>, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurface

    Box(modifier = modifier.fillMaxWidth().height(280.dp).padding(24.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxWidth().height(220.dp)) {
            val n = skills.size
            if (n < 3) return@Canvas

            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = min(cx, cy) * 0.8f
            // Start at top (-90°), clockwise
            val angleFor = { i: Int -> Math.toRadians((-90.0 + i * 360.0 / n)) }

            // Concentric grid rings at 25/50/75/100
            for (ring in 1..4) {
                val rr = radius * ring / 4f
                val path = Path()
                for (i in 0 until n) {
                    val a = angleFor(i)
                    val x = cx + rr * cos(a).toFloat()
                    val y = cy + rr * sin(a).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                drawPath(path, color = grid, style = Stroke(width = 1.5f))
            }

            // Axes
            for (i in 0 until n) {
                val a = angleFor(i)
                drawLine(
                    color = grid,
                    start = Offset(cx, cy),
                    end = Offset(cx + radius * cos(a).toFloat(), cy + radius * sin(a).toFloat()),
                    strokeWidth = 1.5f
                )
            }

            // Data polygon
            val dataPath = Path()
            for (i in 0 until n) {
                val a = angleFor(i)
                val frac = ((skills[i].score ?: 0).coerceIn(0, 100)) / 100f
                val x = cx + radius * frac * cos(a).toFloat()
                val y = cy + radius * frac * sin(a).toFloat()
                if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
            }
            dataPath.close()
            drawPath(dataPath, color = accent.copy(alpha = 0.25f))
            drawPath(dataPath, color = accent, style = Stroke(width = 4f))

            // Vertex dots + labels
            for (i in 0 until n) {
                val a = angleFor(i)
                val frac = ((skills[i].score ?: 0).coerceIn(0, 100)) / 100f
                val x = cx + radius * frac * cos(a).toFloat()
                val y = cy + radius * frac * sin(a).toFloat()
                drawCircle(accent, radius = 6f, center = Offset(x, y))

                val lx = cx + (radius + 28f) * cos(a).toFloat()
                val ly = cy + (radius + 28f) * sin(a).toFloat()
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = labelColor.toArgb()
                        textSize = 30f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    val scoreText = skills[i].score?.toString() ?: "—"
                    drawText(skills[i].label, lx, ly, paint)
                    val paintScore = android.graphics.Paint(paint).apply {
                        isFakeBoldText = true
                        textSize = 34f
                    }
                    drawText(scoreText, lx, ly + 34f, paintScore)
                }
            }
        }
    }
}

private fun Color.toArgb(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt()
)
