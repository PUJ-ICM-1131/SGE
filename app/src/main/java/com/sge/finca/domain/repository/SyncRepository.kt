package com.sge.finca.domain.repository

import com.sge.finca.domain.model.SyncSummary
import kotlinx.coroutines.flow.Flow

/** Estado de la sincronización con Firestore para la UI (issue #32). */
interface SyncRepository {
    /** Registros pendientes y rechazados de la finca (incluye borrados por enviar). */
    fun observeSummary(farmId: String): Flow<SyncSummary>

    /** Vuelve a poner en cola lo que Firestore rechazó y pide sincronizar. */
    suspend fun retryFailed(farmId: String)
}
