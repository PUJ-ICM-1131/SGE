package com.sge.finca.platform.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.sge.finca.domain.model.Reminder
import com.sge.finca.domain.repository.ReminderRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Se ejecuta a la hora del recordatorio y muestra la notificación.
 *
 * Antes de avisar vuelve a leer el recordatorio en Room: si lo completaron, lo borraron
 * o le cambiaron la hora mientras esperaba, no avisa (el aviso correcto ya está programado aparte).
 */
@HiltWorker
class ReminderNotificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: ReminderRepository,
    private val notifier: ReminderNotifier,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val farmId = inputData.getString(KEY_FARM_ID) ?: return Result.failure()
        val reminderId = inputData.getString(KEY_REMINDER_ID) ?: return Result.failure()
        val scheduledFor = inputData.getLong(KEY_DUE_AT, -1)

        val reminder = repository.getReminder(farmId, reminderId) ?: return Result.success()
        if (reminder.isCompleted || reminder.dueAt != scheduledFor) return Result.success()

        notifier.show(reminder)
        return Result.success()
    }

    companion object {
        private const val KEY_FARM_ID = "farmId"
        private const val KEY_REMINDER_ID = "reminderId"
        private const val KEY_DUE_AT = "dueAt"

        fun inputFor(reminder: Reminder): Data = workDataOf(
            KEY_FARM_ID to reminder.farmId,
            KEY_REMINDER_ID to reminder.id,
            KEY_DUE_AT to reminder.dueAt,
        )
    }
}
