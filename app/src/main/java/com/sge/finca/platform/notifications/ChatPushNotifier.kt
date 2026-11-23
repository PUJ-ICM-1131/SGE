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
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Mensaje nuevo del chat propietario ↔ trabajador (#74) que llega por FCM desde sge-notifier. */
@Singleton
class ChatPushNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val manager get() = NotificationManagerCompat.from(context)

    fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, "Mensajes", NotificationManager.IMPORTANCE_HIGH)
            .apply { description = "Chat entre el propietario y los trabajadores" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission") // se verifica abajo
    fun show(workerUid: String, title: String, body: String) {
        val permissionOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!permissionOk || !manager.areNotificationsEnabled()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(NotificationDestinations.EXTRA_DESTINATION, NotificationDestinations.CHAT)
            putExtra(NotificationDestinations.EXTRA_CHAT_WORKER_UID, workerUid)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId(workerUid),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_chat)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // Una notificación por conversación: el mensaje nuevo reemplaza al anterior.
        manager.notify(notificationId(workerUid), notification)
    }

    private fun notificationId(workerUid: String) = "chat:$workerUid".hashCode()

    companion object {
        const val CHANNEL_ID = "messages"
    }
}
