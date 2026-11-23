package com.sge.finca.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.sge.finca.data.local.SyncStatus
import com.sge.finca.data.local.dao.PaddockDao
import com.sge.finca.data.local.dao.PendingDeleteDao
import com.sge.finca.data.local.entity.PaddockEntity
import com.sge.finca.data.local.entity.PendingDeleteEntity
import com.sge.finca.data.remote.FirestorePaths
import com.sge.finca.data.sync.SyncScheduler
import com.sge.finca.domain.model.Coordinate
import com.sge.finca.domain.model.Paddock
import com.sge.finca.domain.model.PaddockGeometry
import com.sge.finca.domain.model.Permission
import com.sge.finca.domain.repository.PaddockRepository
import com.sge.finca.domain.repository.SessionRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class PaddockRepositoryImpl @Inject constructor(
    private val dao: PaddockDao,
    private val pendingDeleteDao: PendingDeleteDao,
    private val syncScheduler: SyncScheduler,
    private val auth: FirebaseAuth,
    private val sessionRepository: SessionRepository,
) : PaddockRepository {

    override fun observePaddocks(farmId: String): Flow<List<Paddock>> =
        dao.observeByFarm(farmId).map { list -> list.map { it.toDomain() } }

    override suspend fun create(farmId: String, name: String, points: List<Coordinate>): String {
        sessionRepository.requirePermission(farmId, Permission.START_INSPECTION)
        val uid = checkNotNull(auth.currentUser?.uid) { "Inicia sesión para guardar potreros" }
        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "Escribe un nombre para el potrero" }
        require(points.size >= 3) { "El potrero necesita al menos 3 puntos" }
        val now = System.currentTimeMillis()

        val entity = PaddockEntity(
            id = UUID.randomUUID().toString(),
            farmId = farmId,
            name = cleanName,
            points = PaddockEntity.encodePoints(points),
            areaM2 = PaddockGeometry.areaM2(points),
            createdBy = uid,
            createdAt = now,
            updatedAt = now,
        )
        dao.insert(entity)
        syncScheduler.requestSync()
        return entity.id
    }

    override suspend fun rename(farmId: String, id: String, name: String) {
        sessionRepository.requirePermission(farmId, Permission.CONFIGURE_FARM)
        val current = dao.getById(farmId, id) ?: throw IllegalArgumentException("Potrero no encontrado")
        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "Escribe un nombre para el potrero" }

        dao.update(
            current.copy(
                name = cleanName,
                updatedAt = maxOf(System.currentTimeMillis(), current.updatedAt + 1),
                syncStatus = SyncStatus.PENDING,
            )
        )
        syncScheduler.requestSync()
    }

    override suspend fun delete(farmId: String, id: String) {
        sessionRepository.requirePermission(farmId, Permission.CONFIGURE_FARM)
        // Primero se anota el borrado pendiente: si la app se cierra justo aquí, igual se envía.
        pendingDeleteDao.insert(
            PendingDeleteEntity(
                path = FirestorePaths.paddock(farmId, id),
                farmId = farmId,
                collection = FirestorePaths.PADDOCKS,
                docId = id,
                createdAt = System.currentTimeMillis(),
            )
        )
        dao.delete(farmId, id)
        syncScheduler.requestSync()
    }
}
