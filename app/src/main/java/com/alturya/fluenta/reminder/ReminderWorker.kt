package com.alturya.fluenta.reminder

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.alturya.fluenta.MainActivity
import com.alturya.fluenta.R
import com.alturya.fluenta.data.I18nStore
import com.alturya.fluenta.data.SettingsStore
import kotlinx.coroutines.flow.first

/**
 * Muestra el recordatorio diario de práctica y reprograma el del día siguiente.
 * Respeta el toggle del usuario: si apagó los recordatorios, no notifica ni se
 * reprograma (cadena rota a propósito). Tras notificar, encadena el siguiente día.
 */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        // El usuario pudo apagar los recordatorios después de programarse: respetarlo.
        if (!SettingsStore.remindersEnabled(ctx).first()) return Result.success()

        if (notificationsAllowed(ctx)) {
            ReminderScheduler.ensureChannel(ctx)
            val openApp = PendingIntent.getActivity(
                ctx, 0,
                Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE,
            )
            val notif = NotificationCompat.Builder(ctx, ReminderScheduler.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(I18nStore.t("reminder.title", "¡Tu inglés te espera! 🔥"))
                .setContentText(I18nStore.t("reminder.body", "5 minutos hoy mantienen tu racha viva. ¿Practicamos?"))
                .setAutoCancel(true)
                .setContentIntent(openApp)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
            NotificationManagerCompat.from(ctx).notify(REMINDER_NOTIF_ID, notif)
        }

        // Encadena el recordatorio de mañana (WorkManager OneTime + reschedule).
        ReminderScheduler.schedule(ctx)
        return Result.success()
    }

    private fun notificationsAllowed(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val REMINDER_NOTIF_ID = 1001
    }
}
