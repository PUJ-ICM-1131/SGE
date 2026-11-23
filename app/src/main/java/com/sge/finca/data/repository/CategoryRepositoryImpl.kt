package com.sge.finca.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.sge.finca.data.local.SyncStatus
import com.sge.finca.data.local.dao.CategoryDao
import com.sge.finca.data.local.dao.PendingDeleteDao
import com.sge.finca.data.local.dao.TransactionDao
import com.sge.finca.data.local.entity.CategoryEntity
import com.sge.finca.data.local.entity.PendingDeleteEntity
import com.sge.finca.data.remote.FirestorePaths
import com.sge.finca.data.remote.toCategoryEntity
import com.sge.finca.data.sync.SyncScheduler
import com.sge.finca.domain.model.Category
import com.sge.finca.domain.model.DefaultCategories
import com.sge.finca.domain.model.Permission
import com.sge.finca.domain.model.TransactionType
import com.sge.finca.domain.repository.CategoryRepository
import com.sge.finca.domain.repository.SessionRepository
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val pendingDeleteDao: PendingDeleteDao,
    private val syncScheduler: SyncScheduler,
    private val sessionRepository: SessionRepository,
    private val firestore: FirebaseFirestore,
) : CategoryRepository {

    override fun observeCategories(farmId: String): Flow<List<Category>> {
        require(farmId.isNotBlank()) { "La finca es obligatoria" }
        return categoryDao.observeByFarm(farmId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun observeCategories(
        farmId: String,
        type: TransactionType,
    ): Flow<List<Category>> {
        require(farmId.isNotBlank()) { "La finca es obligatoria" }
        return categoryDao.observeByFarmAndType(farmId, type.name)
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getCategory(farmId: String, id: String): Category? =
        categoryDao.getById(farmId, id)?.toDomain()

    override suspend fun pullFromRemote(farmId: String) {
        require(farmId.isNotBlank()) { "La finca es obligatoria" }
        val snapshot = firestore
            .collection("${FirestorePaths.farm(farmId)}/${FirestorePaths.CATEGORIES}")
            .get()
            .await()
        val categories = snapshot.documents.mapNotNull { it.toCategoryEntity(farmId) }

        if (categories.isNotEmpty()) {
            categoryDao.upsertAll(categories)
        }
    }

    override suspend fun insertDefaults(farmId: String) {
        require(farmId.isNotBlank()) { "La finca es obligatoria" }

        for (item in DefaultCategories.items) {
            val id = UUID.nameUUIDFromBytes(
                "$farmId:${item.type.name}:${normalize(item.name)}"
                    .toByteArray(Charsets.UTF_8)
            ).toString()

            if (categoryDao.getById(farmId, id) != null) continue

            if (
                categoryDao.existsName(
                    farmId,
                    item.type.name,
                    normalize(item.name),
                    ""
                )
            ) continue

            val entity = CategoryEntity(
                id = id,
                farmId = farmId,
                name = item.name,
                normalizedName = normalize(item.name),
                type = item.type,
                isDefault = true,
            )
            categoryDao.insert(entity)
            syncScheduler.requestSync()
        }
    }

    override suspend fun create(
        farmId: String,
        name: String,
        type: TransactionType,
    ): String {
        require(farmId.isNotBlank()) { "La finca es obligatoria" }
        sessionRepository.requirePermission(farmId, Permission.MANAGE_CATEGORIES)

        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "Escribe un nombre" }

        if (categoryDao.existsName(farmId, type.name, normalize(cleanName), "")) {
            throw IllegalArgumentException(
                "Ya existe una categoría con ese nombre y tipo"
            )
        }

        val entity = CategoryEntity(
            id = UUID.randomUUID().toString(),
            farmId = farmId,
            name = cleanName,
            normalizedName = normalize(cleanName),
            type = type,
            isDefault = false,
        )
        categoryDao.insert(entity)
        syncScheduler.requestSync()
        return entity.id
    }

    override suspend fun rename(
        farmId: String,
        id: String,
        newName: String,
    ) {
        require(farmId.isNotBlank()) { "La finca es obligatoria" }
        sessionRepository.requirePermission(farmId, Permission.MANAGE_CATEGORIES)

        val category = categoryDao.getById(farmId, id)
            ?: throw IllegalArgumentException("Categoría no encontrada")

        val cleanName = newName.trim()
        require(cleanName.isNotEmpty()) { "Escribe un nombre" }

        if (
            categoryDao.existsName(
                farmId,
                category.type.name,
                normalize(cleanName),
                id
            )
        ) {
            throw IllegalArgumentException(
                "Ya existe una categoría con ese nombre y tipo"
            )
        }

        val updated = category.copy(
            name = cleanName,
            normalizedName = normalize(cleanName),
            syncStatus = SyncStatus.PENDING,
        )
        categoryDao.update(updated)
        syncScheduler.requestSync()
    }

    override suspend fun delete(farmId: String, id: String) {
        require(farmId.isNotBlank()) { "La finca es obligatoria" }
        sessionRepository.requirePermission(farmId, Permission.MANAGE_CATEGORIES)

        val category = categoryDao.getById(farmId, id)
            ?: throw IllegalArgumentException("Categoría no encontrada")

        require(!category.isDefault) {
            "No se puede eliminar una categoría predeterminada"
        }

        if (transactionDao.hasTransactionsForCategory(farmId, id)) {
            throw IllegalArgumentException(
                "No se puede eliminar una categoría usada por movimientos"
            )
        }

        pendingDeleteDao.insert(
            PendingDeleteEntity(
                path = FirestorePaths.category(farmId, id),
                farmId = farmId,
                collection = FirestorePaths.CATEGORIES,
                docId = id,
                createdAt = System.currentTimeMillis(),
            )
        )
        categoryDao.delete(farmId, id)
        syncScheduler.requestSync()
    }

    private fun normalize(name: String): String =
        name.trim().lowercase(Locale.ROOT)
}