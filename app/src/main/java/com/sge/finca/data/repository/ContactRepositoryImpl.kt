package com.sge.finca.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.sge.finca.data.local.SyncStatus
import com.sge.finca.data.local.dao.FarmContactDao
import com.sge.finca.data.local.dao.PendingDeleteDao
import com.sge.finca.data.local.entity.PendingDeleteEntity
import com.sge.finca.data.local.entity.FarmContactEntity
import com.sge.finca.data.remote.FirestorePaths
import com.sge.finca.data.sync.SyncScheduler
import com.sge.finca.domain.model.Permission
import com.sge.finca.domain.model.FarmContact
import com.sge.finca.domain.repository.ContactRepository
import com.sge.finca.domain.repository.SessionRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class ContactRepositoryImpl @Inject constructor(
    private val dao: FarmContactDao,
    private val pendingDeleteDao: PendingDeleteDao,
    private val syncScheduler: SyncScheduler,
    private val auth: FirebaseAuth,
    private val sessionRepository: SessionRepository,
) : ContactRepository {

    override fun observeContacts(farmId: String): Flow<List<FarmContact>> {
        require(farmId.isNotBlank()) { "La finca es obligatoria" }
        return dao.observeByFarm(farmId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getContact(farmId: String, id: String): FarmContact? =
        dao.getById(farmId, id)?.toDomain()

    override suspend fun create(
        farmId: String,
        name: String,
        phone: String?,
        email: String?,
        notes: String?,
    ): String {
        sessionRepository.requirePermission(farmId, Permission.MANAGE_CONTACTS)
        require(farmId.isNotBlank()) { "La finca es obligatoria" }
        val uid = checkNotNull(auth.currentUser?.uid) { "Inicia sesión para guardar contactos" }
        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "Escribe un nombre" }
        val now = System.currentTimeMillis()

        val entity = FarmContactEntity(
            id = UUID.randomUUID().toString(),
            farmId = farmId,
            name = cleanName,
            phone = phone.cleanOrNull(),
            email = email.cleanOrNull(),
            notes = notes.cleanOrNull(),
            createdBy = uid,
            createdAt = now,
            updatedAt = now,
        )
        dao.insert(entity)
        syncScheduler.requestSync()
        return entity.id
    }

    override suspend fun update(contact: FarmContact) {
        sessionRepository.requirePermission(contact.farmId, Permission.MANAGE_CONTACTS)
        val current = dao.getById(contact.farmId, contact.id)
            ?: throw IllegalArgumentException("Contacto no encontrado")
        val cleanName = contact.name.trim()
        require(cleanName.isNotEmpty()) { "Escribe un nombre" }

        val updated = current.copy(
            name = cleanName,
            phone = contact.phone.cleanOrNull(),
            email = contact.email.cleanOrNull(),
            notes = contact.notes.cleanOrNull(),
            updatedAt = maxOf(System.currentTimeMillis(), current.updatedAt + 1),
            syncStatus = SyncStatus.PENDING,
        )
        dao.update(updated)
        syncScheduler.requestSync()
    }

    override suspend fun delete(farmId: String, id: String) {
        sessionRepository.requirePermission(farmId, Permission.MANAGE_CONTACTS)
        // Primero se anota el borrado pendiente: si la app se cierra justo aquí, igual se envía.
        pendingDeleteDao.insert(
            PendingDeleteEntity(
                path = FirestorePaths.contact(farmId, id),
                farmId = farmId,
                collection = FirestorePaths.CONTACTS,
                docId = id,
                createdAt = System.currentTimeMillis(),
            )
        )
        dao.delete(farmId, id)
        syncScheduler.requestSync()
    }


    private fun String?.cleanOrNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
}
