package com.sge.finca.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Sube a Firestore lo pendiente (issue #32). WorkManager lo ejecuta solo cuando hay red, lo
 * guarda en disco (sobrevive al cierre de la app y al reinicio del celular) y, si falla por un
 * error transitorio, lo reintenta con espera exponencial.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val engine: SyncEngine,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        // Varias pasadas: lo que se guardó mientras subíamos también entra en esta ejecución.
        repeat(MAX_ROUNDS) {
            val result = engine.pushPending()
            if (result.transientErrors > 0) return Result.retry()
            if (!result.madeProgress) return Result.success()
        }
        return Result.success()
    }

    private companion object {
        const val MAX_ROUNDS = 5
    }
}
