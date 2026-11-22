package com.sge.finca.data.sync

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sge.finca.data.local.dao.PendingDeleteDao
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/** Resultado de una pasada de subida. */
data class PushResult(
    val uploaded: Int = 0,
    val deleted: Int = 0,
    /** Rechazados por Firestore de forma definitiva: quedaron FAILED. */
    val rejected: Int = 0,
    /** Errores de red o tiempo de espera: siguen pendientes y se reintentan. */
    val transientErrors: Int = 0,
) {
    val madeProgress: Boolean get() = uploaded > 0 || deleted > 0 || rejected > 0
}

/**
 * Sube a Firestore lo que está pendiente en Room (issue #32). Lo usa SyncWorker.
 *
 * - Usa el mismo id de Room como id del documento y `set`, así que repetir una subida no crea
 *   duplicados: solo vuelve a escribir el mismo documento.
 * - markSynced solo aplica si la fila no cambió mientras se subía (mismo updatedAt); si el
 *   usuario la editó en ese momento, sigue PENDING y sube en la siguiente pasada.
 */
@Singleton
class SyncEngine @Inject constructor(
    private val adapters: SyncAdapters,
    private val pendingDeleteDao: PendingDeleteDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) {
    /** Evita dos pasadas a la vez (p. ej. el trabajo periódico y uno inmediato). */
    private val mutex = Mutex()

    suspend fun pushPending(): PushResult = mutex.withLock {
        // Sin sesión, las Security Rules rechazarían todo y quedaría FAILED por error.
        if (auth.currentUser == null) return PushResult(transientErrors = 1)

        var result = PushResult()
        for (adapter in adapters.all) {
            result = result.plus(pushAdapter(adapter))
        }
        result.plus(pushDeletes())
    }

    private suspend fun <E : Any> pushAdapter(adapter: SyncAdapter<E>): PushResult {
        var uploaded = 0
        var rejected = 0
        var transientErrors = 0
        for (item in adapter.pendingUploads()) {
            val outcome = write {
                firestore.document(adapter.path(item)).set(adapter.toFirestore(item)).await()
            }
            when (outcome) {
                Outcome.OK -> {
                    adapter.markSynced(item)
                    uploaded++
                }
                Outcome.REJECTED -> {
                    adapter.markFailed(item)
                    rejected++
                    Log.w(TAG, "Firestore rechazó ${adapter.collection}/${adapter.id(item)}: queda FAILED")
                }
                Outcome.TRANSIENT -> transientErrors++
            }
        }
        return PushResult(uploaded = uploaded, rejected = rejected, transientErrors = transientErrors)
    }

    private suspend fun pushDeletes(): PushResult {
        var deleted = 0
        var rejected = 0
        var transientErrors = 0
        for (pending in pendingDeleteDao.getRetryable()) {
            when (write { firestore.document(pending.path).delete().await() }) {
                Outcome.OK -> {
                    pendingDeleteDao.remove(pending.path)
                    deleted++
                }
                Outcome.REJECTED -> {
                    pendingDeleteDao.markFailed(pending.path)
                    rejected++
                    Log.w(TAG, "Firestore rechazó el borrado de ${pending.path}")
                }
                Outcome.TRANSIENT -> transientErrors++
            }
        }
        return PushResult(deleted = deleted, rejected = rejected, transientErrors = transientErrors)
    }

    private enum class Outcome { OK, REJECTED, TRANSIENT }

    private suspend fun write(block: suspend () -> Unit): Outcome = try {
        // Si la red se cae a mitad de camino, el Task de Firestore no termina nunca.
        withTimeout(WRITE_TIMEOUT_MS) { block() }
        Outcome.OK
    } catch (e: TimeoutCancellationException) {
        Outcome.TRANSIENT
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        if (SyncErrors.isPermanent(e)) Outcome.REJECTED else {
            Log.w(TAG, "Error transitorio al sincronizar", e)
            Outcome.TRANSIENT
        }
    }

    private fun PushResult.plus(other: PushResult) = PushResult(
        uploaded = uploaded + other.uploaded,
        deleted = deleted + other.deleted,
        rejected = rejected + other.rejected,
        transientErrors = transientErrors + other.transientErrors,
    )

    private companion object {
        const val TAG = "SgeSync"
        const val WRITE_TIMEOUT_MS = 20_000L
    }
}
