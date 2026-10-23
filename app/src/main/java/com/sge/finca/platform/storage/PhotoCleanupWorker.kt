package com.sge.finca.platform.storage

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.IOException

@HiltWorker
class PhotoCleanupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParameters: WorkerParameters,
    private val photoFileManager: PhotoFileManager,
) : CoroutineWorker(appContext, workerParameters) {

    override suspend fun doWork(): Result {
        val cleanupResult = photoFileManager.cleanupTemporaryCameraFiles()

        return cleanupResult.fold(
            onSuccess = { deletedCount ->
                Result.success(
                    workDataOf("deleted_count" to deletedCount)
                )
            },
            onFailure = { error ->
                if (error is IOException && runAttemptCount < 3) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            },
        )
    }
}