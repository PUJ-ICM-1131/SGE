package com.sge.finca.domain.repository

import com.sge.finca.domain.model.Reminder

/**
 * Programa y cancela la notificación local de un recordatorio.
 * Usa el id del recordatorio como identificador estable: programar dos veces el mismo
 * recordatorio reemplaza el aviso anterior, nunca lo duplica.
 */
interface ReminderScheduler {

    /** Programa el aviso para [Reminder.dueAt]. Si ya pasó o está completado, cancela el que hubiera. */
    fun schedule(reminder: Reminder)

    /** Cancela el aviso pendiente y retira la notificación si ya se mostró. */
    fun cancel(reminderId: String)
}
