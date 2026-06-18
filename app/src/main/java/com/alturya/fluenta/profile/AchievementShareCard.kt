package com.alturya.fluenta.profile

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.util.flag
import com.alturya.fluenta.util.langName
import com.alturya.fluenta.util.levelLabel
import com.alturya.fluenta.util.levelSystemName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun AchievementShareCard(
    l2: String?,
    level: String?,
    levelSystem: String?,
    streakDays: Int,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var sharing by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    I18nStore.t("profile.shareTitle", "Comparte tu logro"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                buildString {
                    append("${flag(l2)} ${langName(l2)} · ${levelLabel(level, levelSystem)} ${levelSystemName(levelSystem)}")
                    if (streakDays > 1) append(" · $streakDays días")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    sharing = true
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            val bmp = buildAchievementBitmap(
                                context = context,
                                l2 = l2 ?: "en",
                                level = level ?: "a1",
                                levelSystem = levelSystem ?: "cefr",
                                streakDays = streakDays,
                            )
                            shareAchievementBitmap(context, bmp, l2 ?: "en")
                        }
                        sharing = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !sharing,
            ) {
                if (sharing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(I18nStore.t("profile.shareBtn", "Compartir imagen de logro"))
                }
            }
        }
    }
}

// ─── Bitmap generation ────────────────────────────────────────────────────────

private fun buildAchievementBitmap(
    context: Context,
    l2: String,
    level: String,
    levelSystem: String,
    streakDays: Int,
): Bitmap {
    val w = 1080; val h = 566
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val cv = Canvas(bmp)

    // Background gradient (dark navy → brand blue)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, 0f, w.toFloat(), h.toFloat(),
            intArrayOf(0xFF0F172A.toInt(), 0xFF1E3A8A.toInt(), 0xFF3B82F6.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
    }
    cv.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

    // Decorative circle
    val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x183B82F6.toInt() }
    cv.drawCircle(w * 0.82f, h * 0.22f, 210f, circlePaint)

    val white = 0xFFFFFFFF.toInt()
    val accent = 0xFF93C5FD.toInt()

    // "FLUENTA" brand
    val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accent; textSize = 36f; typeface = Typeface.DEFAULT_BOLD; letterSpacing = 0.15f
    }
    cv.drawText("FLUENTA", 64f, 80f, brandPaint)

    // Flag + language
    val langPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = white; textSize = 52f; typeface = Typeface.DEFAULT_BOLD
    }
    val flagStr = flag(l2)
    val langStr = langName(l2)
    cv.drawText("$flagStr  $langStr", 64f, 180f, langPaint)

    // Level badge pill
    val levelText = "${levelLabel(level, levelSystem)}  ${levelSystemName(levelSystem)}"
    val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF3B82F6.toInt() }
    val pillRect = RectF(64f, 208f, 64f + levelText.length * 30f + 40f, 272f)
    cv.drawRoundRect(pillRect, 28f, 28f, pillPaint)
    val levelPillText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = white; textSize = 40f; typeface = Typeface.DEFAULT_BOLD
    }
    cv.drawText(levelText, 84f, 258f, levelPillText)

    // Streak
    if (streakDays > 1) {
        val streakPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFBBF24.toInt(); textSize = 36f; typeface = Typeface.DEFAULT_BOLD
        }
        cv.drawText(
            I18nStore.t("share.streakDays", "🔥 {n} días de racha").replace("{n}", "$streakDays"),
            64f, 340f, streakPaint,
        )
    }

    // Tagline
    val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accent; textSize = 30f; typeface = Typeface.DEFAULT
    }
    cv.drawText(I18nStore.t("share.tagline", "Aprendiendo idiomas con Fluenta · fluenta.alturya.com"), 64f, h - 52f, tagPaint)

    // Bottom divider line
    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x443B82F6.toInt(); strokeWidth = 2f }
    cv.drawLine(64f, h - 70f, w - 64f, h - 70f, linePaint)

    return bmp
}

private fun shareAchievementBitmap(context: Context, bmp: Bitmap, l2: String) {
    val dir = File(context.cacheDir, "achievement").also { it.mkdirs() }
    val file = File(dir, "fluenta_achievement.png")
    file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 95, it) }
    val uri = FileProvider.getUriForFile(context, "com.alturya.fluenta.fileprovider", file)
    val shareText = I18nStore.t("share.text", "¡Estoy aprendiendo {lang} con Fluenta! 🚀 fluenta.alturya.com")
        .replace("{lang}", langName(l2))
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, shareText)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
