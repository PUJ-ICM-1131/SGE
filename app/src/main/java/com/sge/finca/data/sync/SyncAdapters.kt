package com.sge.finca.data.sync

import com.google.firebase.firestore.DocumentSnapshot
import com.sge.finca.data.local.SyncStatus
import com.sge.finca.data.local.dao.AnimalDao
import com.sge.finca.data.local.dao.CategoryDao
import com.sge.finca.data.local.dao.FarmContactDao
import com.sge.finca.data.local.dao.PaddockDao
import com.sge.finca.data.local.dao.ReminderDao
import com.sge.finca.data.local.dao.TransactionDao
import com.sge.finca.data.local.entity.AnimalEntity
import com.sge.finca.data.local.entity.CategoryEntity
import com.sge.finca.data.local.entity.FarmContactEntity
import com.sge.finca.data.local.entity.PaddockEntity
import com.sge.finca.data.local.entity.ReminderEntity
import com.sge.finca.data.local.entity.TransactionEntity
import com.sge.finca.data.remote.FirestorePaths
import com.sge.finca.data.remote.toAnimalEntity
import com.sge.finca.data.remote.toCategoryEntity
import com.sge.finca.data.remote.toFarmContactEntity
import com.sge.finca.data.remote.toFirestore
import com.sge.finca.data.remote.toPaddockEntity
import com.sge.finca.data.remote.toReminderEntity
import com.sge.finca.data.remote.toTransactionEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * Lo que el motor de sincronización necesita saber de una tabla para subirla y para aplicar lo
 * que llega de Firestore. Hay una implementación por colección sincronizada.
 */
interface SyncAdapter<E : Any> {
    /** Subcolección en Firestore: farms/{farmId}/{collection}. */
    val collection: String

    fun farmId(item: E): String
    fun id(item: E): String
    fun status(item: E): SyncStatus

    /** null si la tabla no tiene updatedAt (categorías). */
    fun updatedAt(item: E): Long?

    // --- Subida ---
    suspend fun pendingUploads(): List<E>
    fun toFirestore(item: E): Map<String, Any?>
    suspend fun markSynced(item: E)
    suspend fun markFailed(item: E)

    // --- Bajada (listener) ---
    fun fromFirestore(doc: DocumentSnapshot, farmId: String): E?

    /** Conserva lo que solo existe en este celular (p. ej. la foto local). */
    fun keepLocalOnly(remote: E, local: E): E = remote
    suspend fun getLocal(farmId: String, id: String): E?
    suspend fun upsert(items: List<E>)
    suspend fun deleteLocal(farmId: String, id: String)
    suspend fun syncedIds(farmId: String): List<String>

    // --- Estado para la UI ---
    suspend fun resetFailed(farmId: String)
    fun observeCount(farmId: String, status: SyncStatus): Flow<Int>

    fun path(item: E): String = FirestorePaths.farmDocument(farmId(item), collection, id(item))
}

class TransactionSyncAdapter @Inject constructor(
    private val dao: TransactionDao,
) : SyncAdapter<TransactionEntity> {
    override val collection = FirestorePaths.TRANSACTIONS
    override fun farmId(item: TransactionEntity) = item.farmId
    override fun id(item: TransactionEntity) = item.id
    override fun status(item: TransactionEntity) = item.syncStatus
    override fun updatedAt(item: TransactionEntity): Long? = item.updatedAt
    override suspend fun pendingUploads() = dao.getPendingUpload()
    override fun toFirestore(item: TransactionEntity) = item.toFirestore()
    override suspend fun markSynced(item: TransactionEntity) =
        dao.markSynced(item.farmId, item.id, item.updatedAt)
    override suspend fun markFailed(item: TransactionEntity) =
        dao.markFailed(item.farmId, item.id, item.updatedAt)
    override fun fromFirestore(doc: DocumentSnapshot, farmId: String) = doc.toTransactionEntity(farmId)
    override fun keepLocalOnly(remote: TransactionEntity, local: TransactionEntity) =
        remote.copy(photoUri = local.photoUri)
    override suspend fun getLocal(farmId: String, id: String) = dao.getById(farmId, id)
    override suspend fun upsert(items: List<TransactionEntity>) = dao.upsertAll(items)
    override suspend fun deleteLocal(farmId: String, id: String) = dao.delete(farmId, id)
    override suspend fun syncedIds(farmId: String) = dao.getSyncedIds(farmId)
    override suspend fun resetFailed(farmId: String) = dao.resetFailed(farmId)
    override fun observeCount(farmId: String, status: SyncStatus) =
        dao.observeCountByStatus(farmId, status.name)
}

class CategorySyncAdapter @Inject constructor(
    private val dao: CategoryDao,
) : SyncAdapter<CategoryEntity> {
    override val collection = FirestorePaths.CATEGORIES
    override fun farmId(item: CategoryEntity) = item.farmId
    override fun id(item: CategoryEntity) = item.id
    override fun status(item: CategoryEntity) = item.syncStatus
    override fun updatedAt(item: CategoryEntity): Long? = null
    override suspend fun pendingUploads() = dao.getPendingUpload()
    override fun toFirestore(item: CategoryEntity) = item.toFirestore()
    override suspend fun markSynced(item: CategoryEntity) =
        dao.markSynced(item.farmId, item.id, item.name)
    override suspend fun markFailed(item: CategoryEntity) =
        dao.markFailed(item.farmId, item.id, item.name)
    override fun fromFirestore(doc: DocumentSnapshot, farmId: String) = doc.toCategoryEntity(farmId)
    override suspend fun getLocal(farmId: String, id: String) = dao.getById(farmId, id)
    override suspend fun upsert(items: List<CategoryEntity>) = dao.upsertAll(items)
    override suspend fun deleteLocal(farmId: String, id: String) = dao.delete(farmId, id)
    override suspend fun syncedIds(farmId: String) = dao.getSyncedIds(farmId)
    override suspend fun resetFailed(farmId: String) = dao.resetFailed(farmId)
    override fun observeCount(farmId: String, status: SyncStatus) =
        dao.observeCountByStatus(farmId, status.name)
}

class ContactSyncAdapter @Inject constructor(
    private val dao: FarmContactDao,
) : SyncAdapter<FarmContactEntity> {
    override val collection = FirestorePaths.CONTACTS
    override fun farmId(item: FarmContactEntity) = item.farmId
    override fun id(item: FarmContactEntity) = item.id
    override fun status(item: FarmContactEntity) = item.syncStatus
    override fun updatedAt(item: FarmContactEntity): Long? = item.updatedAt
    override suspend fun pendingUploads() = dao.getPendingUpload()
    override fun toFirestore(item: FarmContactEntity) = item.toFirestore()
    override suspend fun markSynced(item: FarmContactEntity) =
        dao.markSynced(item.farmId, item.id, item.updatedAt)
    override suspend fun markFailed(item: FarmContactEntity) =
        dao.markFailed(item.farmId, item.id, item.updatedAt)
    override fun fromFirestore(doc: DocumentSnapshot, farmId: String) = doc.toFarmContactEntity(farmId)
    override suspend fun getLocal(farmId: String, id: String) = dao.getById(farmId, id)
    override suspend fun upsert(items: List<FarmContactEntity>) = dao.upsertAll(items)
    override suspend fun deleteLocal(farmId: String, id: String) = dao.delete(farmId, id)
    override suspend fun syncedIds(farmId: String) = dao.getSyncedIds(farmId)
    override suspend fun resetFailed(farmId: String) = dao.resetFailed(farmId)
    override fun observeCount(farmId: String, status: SyncStatus) =
        dao.observeCountByStatus(farmId, status.name)
}

class ReminderSyncAdapter @Inject constructor(
    private val dao: ReminderDao,
) : SyncAdapter<ReminderEntity> {
    override val collection = FirestorePaths.REMINDERS
    override fun farmId(item: ReminderEntity) = item.farmId
    override fun id(item: ReminderEntity) = item.id
    override fun status(item: ReminderEntity) = item.syncStatus
    override fun updatedAt(item: ReminderEntity): Long? = item.updatedAt
    override suspend fun pendingUploads() = dao.getPendingUpload()
    override fun toFirestore(item: ReminderEntity) = item.toFirestore()
    override suspend fun markSynced(item: ReminderEntity) =
        dao.markSynced(item.farmId, item.id, item.updatedAt)
    override suspend fun markFailed(item: ReminderEntity) =
        dao.markFailed(item.farmId, item.id, item.updatedAt)
    override fun fromFirestore(doc: DocumentSnapshot, farmId: String) = doc.toReminderEntity(farmId)
    override suspend fun getLocal(farmId: String, id: String) = dao.getById(farmId, id)
    override suspend fun upsert(items: List<ReminderEntity>) = dao.upsertAll(items)
    override suspend fun deleteLocal(farmId: String, id: String) = dao.delete(farmId, id)
    override suspend fun syncedIds(farmId: String) = dao.getSyncedIds(farmId)
    override suspend fun resetFailed(farmId: String) = dao.resetFailed(farmId)
    override fun observeCount(farmId: String, status: SyncStatus) =
        dao.observeCountByStatus(farmId, status.name)
}

/** Animales de la finca (issue #62): antes solo vivían en el celular de quien los registró. */
class AnimalSyncAdapter @Inject constructor(
    private val dao: AnimalDao,
) : SyncAdapter<AnimalEntity> {
    override val collection = FirestorePaths.ANIMAL_RECORDS
    // pendingUploads() solo devuelve filas con finca.
    override fun farmId(item: AnimalEntity) = checkNotNull(item.farmId) { "Animal sin finca" }
    override fun id(item: AnimalEntity) = item.id
    override fun status(item: AnimalEntity) = item.syncStatus
    override fun updatedAt(item: AnimalEntity): Long? = item.updatedAt
    override suspend fun pendingUploads() = dao.getPendingUpload()
    override fun toFirestore(item: AnimalEntity) = item.toFirestore()
    override suspend fun markSynced(item: AnimalEntity) =
        dao.markSynced(farmId(item), item.id, item.updatedAt)
    override suspend fun markFailed(item: AnimalEntity) =
        dao.markFailed(farmId(item), item.id, item.updatedAt)
    override fun fromFirestore(doc: DocumentSnapshot, farmId: String) = doc.toAnimalEntity(farmId)
    /** La foto local solo existe en el celular que la tomó. */
    override fun keepLocalOnly(remote: AnimalEntity, local: AnimalEntity) =
        remote.copy(photoUri = local.photoUri)
    override suspend fun getLocal(farmId: String, id: String) = dao.getById(farmId, id)
    override suspend fun upsert(items: List<AnimalEntity>) = dao.upsertAll(items)
    override suspend fun deleteLocal(farmId: String, id: String) = dao.deleteById(farmId, id)
    override suspend fun syncedIds(farmId: String) = dao.getSyncedIds(farmId)
    override suspend fun resetFailed(farmId: String) = dao.resetFailed(farmId)
    override fun observeCount(farmId: String, status: SyncStatus) =
        dao.observeCountByStatus(farmId, status.name)
}

/** Potreros de la finca (#71). */
class PaddockSyncAdapter @Inject constructor(
    private val dao: PaddockDao,
) : SyncAdapter<PaddockEntity> {
    override val collection = FirestorePaths.PADDOCKS
    override fun farmId(item: PaddockEntity) = item.farmId
    override fun id(item: PaddockEntity) = item.id
    override fun status(item: PaddockEntity) = item.syncStatus
    override fun updatedAt(item: PaddockEntity): Long? = item.updatedAt
    override suspend fun pendingUploads() = dao.getPendingUpload()
    override fun toFirestore(item: PaddockEntity) = item.toFirestore()
    override suspend fun markSynced(item: PaddockEntity) =
        dao.markSynced(item.farmId, item.id, item.updatedAt)
    override suspend fun markFailed(item: PaddockEntity) =
        dao.markFailed(item.farmId, item.id, item.updatedAt)
    override fun fromFirestore(doc: DocumentSnapshot, farmId: String) = doc.toPaddockEntity(farmId)
    override suspend fun getLocal(farmId: String, id: String) = dao.getById(farmId, id)
    override suspend fun upsert(items: List<PaddockEntity>) = dao.upsertAll(items)
    override suspend fun deleteLocal(farmId: String, id: String) = dao.delete(farmId, id)
    override suspend fun syncedIds(farmId: String) = dao.getSyncedIds(farmId)
    override suspend fun resetFailed(farmId: String) = dao.resetFailed(farmId)
    override fun observeCount(farmId: String, status: SyncStatus) =
        dao.observeCountByStatus(farmId, status.name)
}

/**
 * Todas las colecciones que se sincronizan. Las categorías van primero para que, en una finca
 * nueva, lleguen antes que las transacciones que las usan.
 */
@Singleton
class SyncAdapters @Inject constructor(
    categories: CategorySyncAdapter,
    transactions: TransactionSyncAdapter,
    contacts: ContactSyncAdapter,
    reminders: ReminderSyncAdapter,
    animals: AnimalSyncAdapter,
    paddocks: PaddockSyncAdapter,
) {
    val all: List<SyncAdapter<*>> =
        listOf(categories, transactions, contacts, reminders, animals, paddocks)
}
