package com.sge.finca.ui.screens.reminders

import com.sge.finca.domain.model.Reminder

/** Estado visible de un recordatorio: pendiente (ámbar), vencido (rojo) o completado (verde, tachado). */
enum class ReminderStatus { PENDING, OVERDUE, COMPLETED }

fun Reminder.statusAt(now: Long): ReminderStatus = when {
    isCompleted -> ReminderStatus.COMPLETED
    dueAt <= now -> ReminderStatus.OVERDUE
    else -> ReminderStatus.PENDING
}

/**
 * Pendientes que requieren atención hoy: vencidos o que vencen antes de que termine el día.
 * Es el número que muestra la campana del inicio.
 */
fun List<Reminder>.attentionCount(endOfToday: Long): Int =
    count { !it.isCompleted && it.dueAt <= endOfToday }

data class RemindersUiState(
    val isLoading: Boolean = true,
    val noFarm: Boolean = false,
    /** Vencidos sin completar, del más antiguo al más reciente. */
    val overdue: List<Reminder> = emptyList(),
    /** Próximos, del más cercano al más lejano. */
    val upcoming: List<Reminder> = emptyList(),
    /** Completados, quedan como historial. */
    val completed: List<Reminder> = emptyList(),
    val now: Long = System.currentTimeMillis(),
    val currentUserId: String? = null,
    /** Propietario: puede eliminar cualquiera. Un trabajador, solo los que creó. */
    val isOwner: Boolean = false,
    val editor: ReminderEditorState? = null,
    val pendingDelete: Reminder? = null,
    val message: String? = null,
) {
    val isEmpty: Boolean get() = overdue.isEmpty() && upcoming.isEmpty() && completed.isEmpty()

    fun canDelete(reminder: Reminder): Boolean = isOwner || reminder.createdBy == currentUserId
}

/** Formulario de crear/editar. [id] null = recordatorio nuevo. */
data class ReminderEditorState(
    val id: String? = null,
    val title: String = "",
    val description: String = "",
    val dueAt: Long,
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
) {
    val isNew: Boolean get() = id == null
}
