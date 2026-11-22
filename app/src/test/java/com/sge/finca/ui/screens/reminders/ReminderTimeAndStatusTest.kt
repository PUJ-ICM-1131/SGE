package com.sge.finca.ui.screens.reminders

import com.sge.finca.domain.model.Reminder
import com.sge.finca.ui.format.ReminderTimeFormat
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.TimeZone

class ReminderTimeAndStatusTest {

    private val bogota: TimeZone = TimeZone.getTimeZone("America/Bogota")

    /** 23 nov 2026 00:00 UTC, como lo entrega el DatePicker. */
    private val nov23Utc = 1_795_392_000_000L

    @Test
    fun `une el dia del calendario con la hora local sin correr el dia`() {
        val dueAt = ReminderTimeFormat.combine(nov23Utc, hour = 7, minute = 30, timeZone = bogota)
        // 7:30 a. m. en Bogotá = 12:30 UTC del mismo día.
        assertEquals(nov23Utc + (12 * 60 + 30) * 60_000L, dueAt)
        assertEquals(7, ReminderTimeFormat.hourOf(dueAt, bogota))
        assertEquals(30, ReminderTimeFormat.minuteOf(dueAt, bogota))
        // De vuelta al calendario: el mismo día, aunque en UTC ya sea otra hora.
        assertEquals(nov23Utc, ReminderTimeFormat.toPickerDate(dueAt, bogota))
    }

    @Test
    fun `una hora de la noche no salta al dia siguiente`() {
        val dueAt = ReminderTimeFormat.combine(nov23Utc, hour = 22, minute = 0, timeZone = bogota)
        // 10 p. m. en Bogotá ya es 24 nov en UTC, pero sigue siendo el 23 para el usuario.
        assertEquals(nov23Utc, ReminderTimeFormat.toPickerDate(dueAt, bogota))
    }

    @Test
    fun `la siguiente hora en punto deja al menos media hora`() {
        val at0745 = ReminderTimeFormat.combine(nov23Utc, 7, 45, bogota)
        assertEquals(ReminderTimeFormat.combine(nov23Utc, 9, 0, bogota), ReminderTimeFormat.nextRoundHour(at0745, bogota))
        val at0710 = ReminderTimeFormat.combine(nov23Utc, 7, 10, bogota)
        assertEquals(ReminderTimeFormat.combine(nov23Utc, 8, 0, bogota), ReminderTimeFormat.nextRoundHour(at0710, bogota))
    }

    @Test
    fun `estado pendiente, vencido y completado`() {
        val now = 1_000_000L
        assertEquals(ReminderStatus.PENDING, reminder(dueAt = now + 1).statusAt(now))
        assertEquals(ReminderStatus.OVERDUE, reminder(dueAt = now).statusAt(now))
        assertEquals(ReminderStatus.COMPLETED, reminder(dueAt = now - 1, completed = true).statusAt(now))
    }

    @Test
    fun `la campana cuenta vencidos y los de hoy, no completados ni futuros`() {
        val endOfToday = 2_000L
        val list = listOf(
            reminder("vencido", dueAt = 500),
            reminder("hoy", dueAt = 1_500),
            reminder("mañana", dueAt = 3_000),
            reminder("hecho", dueAt = 500, completed = true),
        )
        assertEquals(2, list.attentionCount(endOfToday))
    }

    private fun reminder(id: String = "r", dueAt: Long, completed: Boolean = false) =
        Reminder(id, "f1", "Título", null, dueAt, completed, "u1", 0, 0)
}
