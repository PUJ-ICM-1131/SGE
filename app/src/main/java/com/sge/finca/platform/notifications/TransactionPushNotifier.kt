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

/** Notificación de "transacción nueva" que llega por FCM desde sge-notifier. */
@Singleton
class TransactionPushNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val manager get() = NotificationManagerCompat.from(context)

    fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, "Movimientos de la finca", NotificationManager.IMPORTANCE_DEFAULT)
            .apply { description = "Ingresos y egresos que registran otros miembros de la finca" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission") // se verifica abajo
    fun show(transactionId: String, title: String, body: String) {
        val permissionOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!permissionOk || !manager.areNotificationsEnabled()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(NotificationDestinations.EXTRA_DESTINATION, NotificationDestinations.TRANSACTION)
            putExtra(NotificationDestinations.EXTRA_TRANSACTION_ID, transactionId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            transactionId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_transaction)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // Id por transacción: si el mismo aviso llega dos veces, se reemplaza en vez de duplicarse.
        manager.notify(transactionId.hashCode(), notification)
    }

    companion object {
        const val CHANNEL_ID = "movements"
    }
}
