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

/**
 * Alerta de vaca fuera de los potreros (#73) que llega por FCM desde sge-notifier: al propietario
 * cuando sale, y a los trabajadores cuando él la reenvía. Al tocarla se abre el detalle.
 */
@Singleton
class AlertPushNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val manager get() = NotificationManagerCompat.from(context)

    fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        // Importancia alta: suena y aparece encima, hay que ir a buscar la vaca.
        val channel = NotificationChannel(CHANNEL_ID, "Alertas del ganado", NotificationManager.IMPORTANCE_HIGH)
            .apply { description = "Vacas que salen de los potreros de la finca" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission") // se verifica abajo
    fun show(alertId: String, title: String, body: String) {
        val permissionOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!permissionOk || !manager.areNotificationsEnabled()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(NotificationDestinations.EXTRA_DESTINATION, NotificationDestinations.ALERT)
            putExtra(NotificationDestinations.EXTRA_ALERT_ID, alertId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId(alertId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(notificationId(alertId), notification)
    }

    /** Distinto del de las transacciones aunque los ids coincidan por azar. */
    private fun notificationId(alertId: String) = "alert:$alertId".hashCode()

    companion object {
        const val CHANNEL_ID = "livestock_alerts"
    }
}
