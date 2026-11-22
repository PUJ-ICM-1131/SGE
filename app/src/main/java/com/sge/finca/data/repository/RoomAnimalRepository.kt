package com.sge.finca.data.repository

import android.util.Log
import com.sge.finca.data.local.dao.AnimalDao
import com.sge.finca.data.local.dao.PendingDeleteDao
import com.sge.finca.data.local.entity.AnimalEntity
import com.sge.finca.data.local.entity.PendingDeleteEntity
import com.sge.finca.data.remote.FirestorePaths
import com.sge.finca.data.sync.SyncScheduler
import com.sge.finca.domain.model.Animal
import com.sge.finca.domain.model.Permission
import com.sge.finca.domain.repository.AnimalRepository
import com.sge.finca.domain.repository.SessionRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * Animales de la finca activa (issue #62). Son de la finca, no de quien los registró: todos los
 * miembros ven el mismo catálogo en el mapa y en el listado.
 *
 * Igual que las demás tablas (#32): se escribe en Room como PENDING, SyncWorker lo sube a
 * farms/{farmId}/animalRecords y FarmSyncListener baja lo que registran o borran los demás.
 * La foto es local: los otros celulares muestran el avatar de la vaca hasta que exista la
 * subida a Storage (remotePhotoUrl).
 */
@Singleton
class RoomAnimalRepository @Inject constructor(
    private val dao: AnimalDao,
    private val pendingDeleteDao: PendingDeleteDao,
    private val sessionRepository: SessionRepository,
    private val syncScheduler: SyncScheduler,
) : AnimalRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getAnimals(): Flow<List<Animal>> =
        sessionRepository.session
            .map { session ->
                val uid = session.uid
                val farmId = session.farmId
                if (uid != null && farmId != null && session.role != null) ActiveFarm(uid, farmId) else null
            }
            .distinctUntilChanged()
            .flatMapLatest { active ->
                if (active == null) {
                    flowOf(emptyList())
                } else {
                    dao.observeByFarm(active.farmId)
                        .onStart { claimUnassigned(active) }
                        .map { list -> list.map { it.toDomain() } }
                }
            }

    override suspend fun saveAnimal(animal: Animal) {
        val active = requireActiveFarm()
        sessionRepository.requirePermission(active.farmId, Permission.REGISTER_ANIMAL)
        dao.insertAnimal(
            AnimalEntity.fromDomain(
                animal = animal,
                ownerUid = active.uid,
                farmId = active.farmId,
                updatedAt = System.currentTimeMillis(),
            )
        )
        syncScheduler.requestSync()
    }

    override suspend fun getAnimal(id: String): Animal? {
        // Puede llamarse apenas abre la pantalla, antes de que cargue la sesión.
        val farmId = sessionRepository.session.first { !it.loading }.farmId ?: return null
        return dao.getById(farmId, id)?.toDomain()
    }

    override suspend fun updateAnimal(animal: Animal) {
        val active = requireActiveFarm()
        sessionRepository.requirePermission(active.farmId, Permission.REGISTER_ANIMAL)
        val current = dao.getById(active.farmId, animal.id)
            ?: throw IllegalArgumentException("El animal ya no existe en esta finca.")
        dao.updateAnimal(
            farmId = active.farmId, id = animal.id, name = animal.name, notes = animal.notes,
            photoUri = animal.photoUri, latitude = animal.latitude, longitude = animal.longitude,
            locationAccuracy = animal.locationAccuracy, createdAt = animal.createdAt,
            variant = animal.variant,
            // max() evita que dos ediciones en el mismo milisegundo compartan updatedAt.
            updatedAt = maxOf(System.currentTimeMillis(), current.updatedAt + 1),
        )
        syncScheduler.requestSync()
    }

    override suspend fun deleteAnimal(id: String) {
        val active = requireActiveFarm()
        sessionRepository.requirePermission(active.farmId, Permission.REGISTER_ANIMAL)
        // Primero se anota el borrado pendiente: si la app se cierra justo aquí, igual se envía.
        pendingDeleteDao.insert(
            PendingDeleteEntity(
                path = FirestorePaths.animalRecord(active.farmId, id),
                farmId = active.farmId,
                collection = FirestorePaths.ANIMAL_RECORDS,
                docId = id,
                createdAt = System.currentTimeMillis(),
            )
        )
        dao.deleteById(active.farmId, id)
        syncScheduler.requestSync()
    }

    /** Los animales creados antes de #62 no tenían finca: se pasan a la activa de su creador. */
    private suspend fun claimUnassigned(active: ActiveFarm) {
        try {
            val claimed = dao.claimUnassigned(active.farmId, active.uid, System.currentTimeMillis())
            if (claimed > 0) {
                Log.i(TAG, "$claimed animales sin finca asignados a ${active.farmId}")
                syncScheduler.requestSync()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "No se pudieron asignar los animales sin finca", e)
        }
    }

    private suspend fun requireActiveFarm(): ActiveFarm {
        val session = sessionRepository.session.first { !it.loading }
        val uid = checkNotNull(session.uid) { "Inicia sesión para gestionar animales." }
        val farmId = checkNotNull(session.farmId) { "Únete a una finca para gestionar animales." }
        return ActiveFarm(uid, farmId)
    }

    private data class ActiveFarm(val uid: String, val farmId: String)

    private companion object {
        const val TAG = "SgeAnimals"
    }
}
