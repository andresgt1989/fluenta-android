package com.alturya.fluenta.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Recordatorio diario LOCAL de práctica (lever de retención #65). Independiente del
 * backend: la app se recuerda a sí misma a una hora fija, sin push remoto ni conexión.
 *
 * El toggle de Ajustes (`SettingsStore.remindersEnabled`) ya existía pero no programaba
 * nada — esto es la pieza que faltaba para que el recordatorio realmente llegue.
 */
object ReminderScheduler {

    const val CHANNEL_ID = "daily_reminder"
    const val WORK_NAME = "daily_practice_reminder"
    const val TARGET_HOUR = 19 // 7pm hora local: ventana de mayor re-enganche por la tarde-noche.

    /**
     * Milisegundos desde [nowMillis] hasta la próxima ocurrencia de [targetHour]:00 en
     * la zona local. Función PURA (inyecta el Calendar) para poder testearla en JVM sin
     * reloj real: si ya pasó la hora de hoy, apunta a mañana; si aún no, a hoy.
     */
    internal fun nextReminderDelayMillis(nowMillis: Long, targetHour: Int, cal: Calendar): Long {
        cal.timeInMillis = nowMillis
        cal.set(Calendar.HOUR_OF_DAY, targetHour)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (cal.timeInMillis <= nowMillis) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis - nowMillis
    }

    /** Programa (o reprograma) el recordatorio para la próxima [TARGET_HOUR]. Idempotente. */
    fun schedule(context: Context) {
        ensureChannel(context)
        val delay = nextReminderDelayMillis(System.currentTimeMillis(), TARGET_HOUR, Calendar.getInstance())
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        // REPLACE: al reprogramar al abrir la app no se acumulan trabajos duplicados.
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    /** Cancela el recordatorio (cuando el usuario apaga el toggle). */
    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /** Crea el canal de notificación (Android 8+). Sin canal no se muestra NADA. Idempotente. */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = context.getSystemService(NotificationManager::class.java)
        if (mgr.getNotificationChannel(CHANNEL_ID) != null) return
        mgr.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Recordatorios de práctica", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Recordatorio diario para mantener tu racha de aprendizaje"
            },
        )
    }
}
