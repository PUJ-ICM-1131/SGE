package com.sge.finca.data.sync

import android.util.Log
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.sge.finca.data.injection.ApplicationScope
import com.sge.finca.data.local.SyncStatus
import com.sge.finca.data.local.dao.PendingDeleteDao
import com.sge.finca.data.remote.FirestorePaths
import com.sge.finca.domain.repository.SessionRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Firestore -> Room (issue #32): escucha las colecciones de la finca activa y guarda en Room lo
 * que crean, cambian o borran otros celulares.
 *
 * - Lo que llega se guarda como SYNCED y NO pide subida: no genera operaciones pendientes.
 * - Conflictos: last-write-wins por updatedAt (ver [SyncConflicts]); se registran en Logcat con
 *   la etiqueta "SgeSync".
 * - Lo borrado en este celular que aún no se ha enviado no "revive" (se consulta pending_deletes).
 * - Al cambiar de finca o cerrar sesión, se quitan los listeners anteriores.
 */
@Singleton
class FarmSyncListener @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val adapters: SyncAdapters,
    private val pendingDeleteDao: PendingDeleteDao,
    private val firestore: FirebaseFirestore,
    private val syncScheduler: SyncScheduler,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private var job: Job? = null

    /** Se llama una vez al abrir la app (FincaApplication). */
    @Synchronized
    fun start() {
        if (job != null) return
        job = scope.launch {
            sessionRepository.session
                // Solo con una membresía activa: sin ella las Security Rules niegan la lectura.
                .map { session -> session.farmId.takeIf { session.role != null } }
                .distinctUntilChanged()
                .collectLatest { farmId ->
                    if (farmId == null) return@collectLatest
                    // Recién iniciada la sesión o cambiada la finca: subir lo que haya pendiente.
                    syncScheduler.requestSync()
                    coroutineScope {
                        adapters.all.forEach { adapter -> launch { listen(farmId, adapter) } }
                    }
                }
        }
    }

    private suspend fun <E : Any> listen(farmId: String, adapter: SyncAdapter<E>) {
        var reconciled = false
        snapshots(FirestorePaths.farmCollection(farmId, adapter.collection)).collect { snapshot ->
            apply(farmId, adapter, snapshot)
            // La primera respuesta completa del servidor trae TODOS los documentos: lo que en Room
            // figura como sincronizado y ya no está allá, se borró en otro celular con la app cerrada.
            if (!reconciled && !snapshot.metadata.isFromCache) {
                reconcileDeletions(farmId, adapter, snapshot)
                reconciled = true
            }
        }
    }

    private suspend fun <E : Any> apply(farmId: String, adapter: SyncAdapter<E>, snapshot: QuerySnapshot) {
        val toSave = mutableListOf<E>()
        for (change in snapshot.documentChanges) {
            val doc = change.document
            // Escrituras propias que aún no confirma el servidor: Room ya las tiene.
            if (doc.metadata.hasPendingWrites()) continue
            val local = adapter.getLocal(farmId, doc.id)

            if (change.type == DocumentChange.Type.REMOVED) {
                when {
                    local == null -> Unit
                    adapter.status(local) == SyncStatus.SYNCED -> adapter.deleteLocal(farmId, doc.id)
                    else -> logConflict(adapter, doc.id, "borrado en otro celular, pero aquí tiene cambios sin subir: se conserva")
                }
                continue
            }

            if (pendingDeleteDao.exists(farmId, adapter.collection, doc.id)) continue
            val remote = adapter.fromFirestore(doc, farmId) ?: continue
            val localStatus = local?.let { adapter.status(it) }
            val decision = SyncConflicts.decide(
                localStatus = localStatus,
                localUpdatedAt = local?.let { adapter.updatedAt(it) },
                remoteUpdatedAt = adapter.updatedAt(remote),
            )
            if (SyncConflicts.isConflict(localStatus)) {
                val winner = if (decision == MergeDecision.APPLY_REMOTE) "gana la remota" else "gana la local"
                logConflict(adapter, doc.id, "cambios en ambos lados: $winner (last-write-wins)")
            }
            if (decision == MergeDecision.APPLY_REMOTE) {
                toSave += if (local != null) adapter.keepLocalOnly(remote, local) else remote
            }
        }
        if (toSave.isNotEmpty()) adapter.upsert(toSave)
    }

    private suspend fun <E : Any> reconcileDeletions(
        farmId: String,
        adapter: SyncAdapter<E>,
        snapshot: QuerySnapshot,
    ) {
        val remoteIds = snapshot.documents.mapTo(HashSet()) { it.id }
        adapter.syncedIds(farmId)
            .filterNot { it in remoteIds }
            .forEach { adapter.deleteLocal(farmId, it) }
    }

    private fun snapshots(collectionPath: String): Flow<QuerySnapshot> = callbackFlow {
        val registration = firestore.collection(collectionPath)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // P. ej. reglas que niegan la lectura: se registra y se sigue con lo local.
                    Log.w(TAG, "No se pudo escuchar $collectionPath", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) trySend(snapshot)
            }
        awaitClose { registration.remove() }
    }.buffer(Channel.UNLIMITED)

    private fun logConflict(adapter: SyncAdapter<*>, id: String, detail: String) {
        Log.i(TAG, "Conflicto en ${adapter.collection}/$id: $detail")
    }

    private companion object {
        const val TAG = "SgeSync"
    }
}
