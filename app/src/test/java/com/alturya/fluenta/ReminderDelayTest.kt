package com.alturya.fluenta

import com.alturya.fluenta.reminder.ReminderScheduler
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * Red de seguridad del cálculo de cuándo disparar el recordatorio diario (retención #65).
 * Un off-by-one aquí hace que el recordatorio llegue a la hora equivocada o no llegue.
 * Función pura: se inyecta un Calendar en UTC para que el test no dependa del reloj real.
 */
class ReminderDelayTest {

    private val utc = TimeZone.getTimeZone("UTC")
    private fun cal() = Calendar.getInstance(utc)

    /** Instante UTC del día base (2026-01-15) a hh:mm. */
    private fun at(hour: Int, minute: Int): Long = cal().apply {
        clear()
        set(2026, Calendar.JANUARY, 15, hour, minute, 0)
    }.timeInMillis

    private val oneHour = 3_600_000L

    @Test fun before_target_same_day() {
        // 10:00, objetivo 19:00 → faltan 9 horas hoy mismo.
        val delay = ReminderScheduler.nextReminderDelayMillis(at(10, 0), 19, cal())
        assertEquals(9 * oneHour, delay)
    }

    @Test fun after_target_rolls_to_tomorrow() {
        // 20:00, objetivo 19:00 → ya pasó; siguiente es mañana 19:00 = 23 horas.
        val delay = ReminderScheduler.nextReminderDelayMillis(at(20, 0), 19, cal())
        assertEquals(23 * oneHour, delay)
    }

    @Test fun exactly_at_target_rolls_to_tomorrow() {
        // 19:00 en punto: cuenta como "ya ocurrió", apunta a mañana (24h), nunca delay 0.
        val delay = ReminderScheduler.nextReminderDelayMillis(at(19, 0), 19, cal())
        assertEquals(24 * oneHour, delay)
    }

    @Test fun just_before_target_is_small_positive() {
        // 18:30, objetivo 19:00 → 30 min. Siempre positivo.
        val delay = ReminderScheduler.nextReminderDelayMillis(at(18, 30), 19, cal())
        assertEquals(30 * 60_000L, delay)
    }
}
