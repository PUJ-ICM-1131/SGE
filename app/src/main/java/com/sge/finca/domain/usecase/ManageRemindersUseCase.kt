package com.sge.finca.domain.usecase

import com.sge.finca.domain.model.Reminder
import com.sge.finca.domain.repository.ReminderRepository
import com.sge.finca.domain.repository.ReminderScheduler
import javax.inject.Inject

/**
 * Ciclo de vida de un recordatorio: cada cambio en los datos va acompañado de su aviso.
 *
 * | Acción                | Aviso                                              |
 * |-----------------------|----------------------------------------------------|
 * | Crear                 | Se programa para la fecha y hora                   |
 * | Editar (fecha u otro) | Se reemplaza el anterior por uno nuevo (mismo id)  |
 * | Completar             | Se cancela si aún no sonó                          |
 * | Reabrir               | Se vuelve a programar si la fecha es futura        |
 * | Eliminar              | Se cancela                                         |
 */
class ManageRemindersUseCase @Inject constructor(
    private val repository: ReminderRepository,
    private val scheduler: ReminderScheduler,
) {
    /** @throws IllegalArgumentException con un mensaje apto para la UI si los datos no son válidos. */
    suspend fun create(
        farmId: String,
        title: String,
        description: String?,
        dueAt: Long,
        now: Long = System.currentTimeMillis(),
    ): String {
        ReminderValidation.check(title, description, dueAt, now, requireFuture = true)
        val id = repository.create(farmId, title, description, dueAt)
        repository.getReminder(farmId, id)?.let(scheduler::schedule)
        return id
    }

    /**
     * Guarda los cambios. Solo exige fecha futura si la fecha cambió: editar el título de un
     * recordatorio vencido no debe obligar a moverlo.
     */
    suspend fun update(reminder: Reminder, now: Long = System.currentTimeMillis()) {
        val current = repository.getReminder(reminder.farmId, reminder.id)
            ?: throw IllegalArgumentException("Recordatorio no encontrado")
        ReminderValidation.check(
            reminder.title,
            reminder.description,
            reminder.dueAt,
            now,
            requireFuture = reminder.dueAt != current.dueAt,
        )
        repository.update(reminder)
        // Reemplaza el aviso anterior (mismo id); si quedó en el pasado o completado, lo cancela.
        repository.getReminder(reminder.farmId, reminder.id)?.let(scheduler::schedule)
    }

    suspend fun setCompleted(farmId: String, id: String, completed: Boolean) {
        repository.setCompleted(farmId, id, completed)
        if (completed) {
            scheduler.cancel(id)
        } else {
            repository.getReminder(farmId, id)?.let(scheduler::schedule)
        }
    }

    suspend fun delete(farmId: String, id: String) {
        // Primero el aviso: si borrar falla a medias, al menos no suena algo que el usuario quiso borrar.
        scheduler.cancel(id)
        repository.delete(farmId, id)
    }
}

/** Reglas de un recordatorio válido, separadas para probarlas sin Android. */
object ReminderValidation {
    const val MAX_TITLE = 80
    const val MAX_DESCRIPTION = 300

    /** Mensaje de error para la UI, o null si es válido. */
    fun errorFor(title: String, description: String?, dueAt: Long, now: Long, requireFuture: Boolean): String? =
        when {
            title.isBlank() -> "Escribe un título"
            title.trim().length > MAX_TITLE -> "El título no puede pasar de $MAX_TITLE caracteres"
            (description?.trim()?.length ?: 0) > MAX_DESCRIPTION ->
                "La descripción no puede pasar de $MAX_DESCRIPTION caracteres"
            requireFuture && dueAt <= now -> "Elige una fecha y hora futura"
            else -> null
        }

    fun check(title: String, description: String?, dueAt: Long, now: Long, requireFuture: Boolean) {
        errorFor(title, description, dueAt, now, requireFuture)?.let { throw IllegalArgumentException(it) }
    }
}
