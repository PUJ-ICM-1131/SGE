package com.sge.finca.platform.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.sge.finca.domain.model.Reminder
import com.sge.finca.domain.repository.ReminderScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Programa los avisos con WorkManager y no con AlarmManager:
 * - WorkManager guarda los trabajos en su propia base y los **reprograma solo tras reiniciar
 *   el celular**; con AlarmManager las alarmas se pierden y habría que volver a crearlas.
 * - No necesita el permiso especial de alarmas exactas de Android 12+.
 * Contra: en ahorro de batería (Doze) el aviso puede llegar con algunos minutos de retraso,
 * aceptable para vacunas, pagos o compras.
 *
 * Cada recordatorio tiene un trabajo único "reminder-{id}". Con [ExistingWorkPolicy.REPLACE],
 * programar de nuevo (al editar la fecha) cancela el anterior: nunca hay avisos duplicados.
 */
@Singleton
class WorkManagerReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notifier: ReminderNotifier,
) : ReminderScheduler {

    private val workManager get() = WorkManager.getInstance(context)

    override fun schedule(reminder: Reminder) {
        val delayMillis = reminder.dueAt - System.currentTimeMillis()
        if (reminder.isCompleted || delayMillis <= 0) {
            cancel(reminder.id)
            return
        }
        val request = OneTimeWorkRequestBuilder<ReminderNotificationWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(ReminderNotificationWorker.inputFor(reminder))
            .addTag(TAG)
            .build()
        workManager.enqueueUniqueWork(uniqueWorkName(reminder.id), ExistingWorkPolicy.REPLACE, request)
        // Si había una notificación mostrada de la fecha anterior, ya no aplica.
        notifier.dismiss(reminder.id)
    }

    override fun cancel(reminderId: String) {
        workManager.cancelUniqueWork(uniqueWorkName(reminderId))
        notifier.dismiss(reminderId)
    }

    companion object {
        const val TAG = "reminder"
        fun uniqueWorkName(reminderId: String) = "reminder-$reminderId"
    }
}
