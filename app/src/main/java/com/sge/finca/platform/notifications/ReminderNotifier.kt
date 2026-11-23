package com.sge.finca.platform.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sge.finca.MainActivity
import com.sge.finca.R
import com.sge.finca.domain.model.Reminder
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Muestra y retira las notificaciones de recordatorios en el canal "Recordatorios SGE". */
@Singleton
class ReminderNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val manager get() = NotificationManagerCompat.from(context)

    /** Se llama al iniciar la app. Crear el canal varias veces no tiene efecto. */
    fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, "Recordatorios SGE", NotificationManager.IMPORTANCE_HIGH)
            .apply { description = "Avisos de vacunas, pagos y tareas de la finca" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** En Android 13+ hace falta el permiso; en todas, que el usuario no las haya apagado. */
    fun canNotify(): Boolean {
        val permissionOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return permissionOk && manager.areNotificationsEnabled()
    }

    @SuppressLint("MissingPermission") // verificado en canNotify()
    fun show(reminder: Reminder) {
        // Sin permiso el recordatorio sigue visible dentro de la app, solo que no suena.
        if (!canNotify()) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setContentTitle(reminder.title)
            .setContentText(reminder.description ?: "Recordatorio de la finca")
            .setStyle(NotificationCompat.BigTextStyle().bigText(reminder.description ?: "Recordatorio de la finca"))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setWhen(reminder.dueAt)
            .setShowWhen(true)
            .setAutoCancel(true)
            .setContentIntent(openRemindersIntent(reminder))
            .build()

        manager.notify(notificationId(reminder.id), notification)
    }

    fun dismiss(reminderId: String) = manager.cancel(notificationId(reminderId))

    /** Abre la app en Recordatorios (pasando por el login si no hay sesión). */
    private fun openRemindersIntent(reminder: Reminder): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(NotificationDestinations.EXTRA_DESTINATION, NotificationDestinations.REMINDERS)
            putExtra(NotificationDestinations.EXTRA_REMINDER_ID, reminder.id)
        }
        return PendingIntent.getActivity(
            context,
            notificationId(reminder.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** Id estable por recordatorio: volver a avisar reemplaza la notificación, no la duplica. */
    private fun notificationId(reminderId: String) = reminderId.hashCode()

    companion object {
        const val CHANNEL_ID = "reminders"
    }
}

/** Extras del Intent con el que una notificación abre la app en una pantalla concreta. */
object NotificationDestinations {
    const val EXTRA_DESTINATION = "com.sge.finca.extra.DESTINATION"
    const val EXTRA_REMINDER_ID = "com.sge.finca.extra.REMINDER_ID"
    const val EXTRA_TRANSACTION_ID = "com.sge.finca.extra.TRANSACTION_ID"
    const val EXTRA_ALERT_ID = "com.sge.finca.extra.ALERT_ID"
    const val REMINDERS = "reminders"
    const val TRANSACTION = "transaction"
    const val ALERT = "alert"
}
