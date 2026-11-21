package com.sge.finca.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sge.finca.data.injection.ApplicationScope
import com.sge.finca.data.local.SyncStatus
import com.sge.finca.data.local.dao.FarmContactDao
import com.sge.finca.data.local.entity.FarmContactEntity
import com.sge.finca.data.remote.FirestorePaths
import com.sge.finca.data.remote.deleteDocument
import com.sge.finca.data.remote.pushDocument
import com.sge.finca.data.remote.toFirestore
import com.sge.finca.domain.model.FarmContact
import com.sge.finca.domain.repository.ContactRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class ContactRepositoryImpl @Inject constructor(
    private val dao: FarmContactDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @ApplicationScope private val scope: CoroutineScope,
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
        push(entity)
        return entity.id
    }

    override suspend fun update(contact: FarmContact) {
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
        push(updated)
    }

    override suspend fun delete(farmId: String, id: String) {
        dao.delete(farmId, id)
        firestore.deleteDocument(FirestorePaths.contact(farmId, id))
    }

    private fun push(entity: FarmContactEntity) {
        firestore.pushDocument(
            path = FirestorePaths.contact(entity.farmId, entity.id),
            data = entity.toFirestore(),
            scope = scope,
        ) {
            dao.markSynced(entity.farmId, entity.id, entity.updatedAt)
        }
    }

    private fun String?.cleanOrNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
}
