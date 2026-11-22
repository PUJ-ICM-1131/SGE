package com.sge.finca.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Pide subir a Firestore lo pendiente. Los repositorios lo llaman después de cada escritura. */
interface SyncScheduler {
    fun requestSync()

    /** Trabajo periódico de respaldo: recoge lo que haya quedado pendiente por cualquier motivo. */
    fun schedulePeriodicSync()
}

@Singleton
class WorkManagerSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : SyncScheduler {

    private val onlyWithNetwork = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /**
     * KEEP: si ya hay una subida esperando red, no se encola otra; esa subida procesa todo lo
     * pendiente al ejecutarse. Sin red, espera a que vuelva la conexión.
     */
    override fun requestSync() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(onlyWithNetwork)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NOW, ExistingWorkPolicy.KEEP, request)
    }

    override fun schedulePeriodicSync() {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(PERIODIC_MINUTES, TimeUnit.MINUTES)
            .setConstraints(onlyWithNetwork)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    private companion object {
        const val WORK_NOW = "sge_sync_now"
        const val WORK_PERIODIC = "sge_sync_periodic"
        const val BACKOFF_SECONDS = 30L
        const val PERIODIC_MINUTES = 30L
    }
}
