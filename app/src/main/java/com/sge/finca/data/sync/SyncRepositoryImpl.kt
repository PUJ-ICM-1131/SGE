package com.sge.finca.data.sync

import com.sge.finca.data.local.SyncStatus
import com.sge.finca.data.local.dao.PendingDeleteDao
import com.sge.finca.domain.model.SyncSummary
import com.sge.finca.domain.repository.SyncRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val adapters: SyncAdapters,
    private val pendingDeleteDao: PendingDeleteDao,
    private val syncScheduler: SyncScheduler,
) : SyncRepository {

    override fun observeSummary(farmId: String): Flow<SyncSummary> {
        val pending = adapters.all.map { it.observeCount(farmId, SyncStatus.PENDING) } +
            pendingDeleteDao.observeCount(farmId, failed = false)
        val failed = adapters.all.map { it.observeCount(farmId, SyncStatus.FAILED) } +
            pendingDeleteDao.observeCount(farmId, failed = true)
        return combine(combine(pending) { it.sum() }, combine(failed) { it.sum() }) { p, f ->
            SyncSummary(pending = p, failed = f)
        }.distinctUntilChanged()
    }

    override suspend fun retryFailed(farmId: String) {
        adapters.all.forEach { it.resetFailed(farmId) }
        pendingDeleteDao.resetFailed(farmId)
        syncScheduler.requestSync()
    }
}
