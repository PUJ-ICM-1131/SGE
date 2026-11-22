package com.sge.finca.data.sync

import com.sge.finca.data.local.SyncStatus

enum class MergeDecision { APPLY_REMOTE, KEEP_LOCAL }

/**
 * Qué hacer cuando llega de Firestore un documento que también existe en Room (issue #32).
 *
 * Regla: last-write-wins por updatedAt, que es lo que Firestore aplica por defecto.
 * - Si la fila local ya está sincronizada, siempre gana la remota (es la versión más nueva).
 * - Si la fila local tiene cambios sin subir (PENDING o FAILED), gana la más reciente.
 *   En empate gana la local: es la que el usuario acaba de ver en este celular.
 * - Si no hay updatedAt para comparar (categorías), gana la local mientras esté pendiente.
 */
object SyncConflicts {

    fun decide(
        localStatus: SyncStatus?,
        localUpdatedAt: Long?,
        remoteUpdatedAt: Long?,
    ): MergeDecision {
        if (localStatus == null || localStatus == SyncStatus.SYNCED) {
            return MergeDecision.APPLY_REMOTE
        }
        if (localUpdatedAt == null || remoteUpdatedAt == null) return MergeDecision.KEEP_LOCAL
        return if (remoteUpdatedAt > localUpdatedAt) {
            MergeDecision.APPLY_REMOTE
        } else {
            MergeDecision.KEEP_LOCAL
        }
    }

    /** ¿Hubo conflicto? (la fila local tenía cambios sin subir). Solo para el log. */
    fun isConflict(localStatus: SyncStatus?): Boolean =
        localStatus != null && localStatus != SyncStatus.SYNCED
}
