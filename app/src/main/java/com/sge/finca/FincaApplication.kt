package com.sge.finca

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.sge.finca.platform.notifications.PushTokenRegistrar
import com.sge.finca.platform.notifications.ReminderNotifier
import com.sge.finca.platform.notifications.TransactionPushNotifier
import com.sge.finca.platform.storage.PhotoCleanupWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class FincaApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var reminderNotifier: ReminderNotifier

    @Inject
    lateinit var transactionPushNotifier: TransactionPushNotifier

    @Inject
    lateinit var pushTokenRegistrar: PushTokenRegistrar

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // El canal debe existir antes de mostrar la primera notificación (Android 8+).
        reminderNotifier.createChannel()
        transactionPushNotifier.createChannel()
        // Guarda el token FCM de este celular cada vez que hay sesión (para los avisos push).
        pushTokenRegistrar.start()
        schedulePhotoCleanup()
    }

    private fun schedulePhotoCleanup() {
        val cleanupRequest =
            PeriodicWorkRequestBuilder<PhotoCleanupWorker>(
                24,
                TimeUnit.HOURS,
            ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "photo_cleanup",
            ExistingPeriodicWorkPolicy.KEEP,
            cleanupRequest,
        )
    }
}