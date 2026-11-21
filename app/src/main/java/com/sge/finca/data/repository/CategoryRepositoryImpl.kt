package com.sge.finca.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.sge.finca.data.injection.ApplicationScope
import com.sge.finca.data.local.SyncStatus
import com.sge.finca.data.local.dao.CategoryDao
import com.sge.finca.data.local.entity.CategoryEntity
import com.sge.finca.data.remote.FirestorePaths
import com.sge.finca.data.remote.deleteDocument
import com.sge.finca.data.remote.pushDocument
import com.sge.finca.data.remote.toFirestore
import com.sge.finca.domain.model.Category
import com.sge.finca.domain.model.DefaultCategories
import com.sge.finca.domain.model.TransactionType
import com.sge.finca.domain.repository.CategoryRepository
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao,
    private val firestore: FirebaseFirestore,
    @ApplicationScope private val scope: CoroutineScope,
) : CategoryRepository {

    override fun observeCategories(farmId: String): Flow<List<Category>> {
        require(farmId.isNotBlank()) { "La finca es obligatoria" }
        return categoryDao.observeByFarm(farmId).map { list -> list.map { it.toDomain() } }
    }

    override fun observeCategories(farmId: String, type: TransactionType): Flow<List<Category>> {
        require(farmId.isNotBlank()) { "La finca es obligatoria" }
        return categoryDao.observeByFarmAndType(farmId, type.name)
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getCategory(farmId: String, id: String): Category? =
        categoryDao.getById(farmId, id)?.toDomain()

    override suspend fun insertDefaults(farmId: String) {
        require(farmId.isNotBlank()) { "La finca es obligatoria" }

        for (item in DefaultCategories.items) {
            val id = UUID.nameUUIDFromBytes(
                "$farmId:${item.type.name}:${normalize(item.name)}"
                    .toByteArray(Charsets.UTF_8)
            ).toString()

            // El ID estable evita volver a insertar una categoría
            // predeterminada si esta función se llama otra vez.
            if (categoryDao.getById(farmId, id) != null) continue

            if (categoryDao.existsName(
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
                isDefault = true
            )
            categoryDao.insert(entity)
            push(entity)
        }
    }

    override suspend fun create(farmId: String, name: String, type: TransactionType): String {
        require(farmId.isNotBlank()) { "La finca es obligatoria" }

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
            isDefault = false
        )
        categoryDao.insert(entity)
        push(entity)
        return entity.id
    }

    override suspend fun rename(farmId: String, id: String, newName: String) {
        val category = categoryDao.getById(farmId, id)
            ?: throw IllegalArgumentException("Categoría no encontrada")

        val cleanName = newName.trim()
        require(cleanName.isNotEmpty()) { "Escribe un nombre" }

        if (categoryDao.existsName(
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
            syncStatus = SyncStatus.PENDING
        )
        categoryDao.update(updated)
        push(updated)
    }

    /**
     * Las transacciones guardan una copia del nombre de la categoría, así que el historial
     * sigue leyéndose bien después de borrarla.
     */
    override suspend fun delete(farmId: String, id: String) {
        categoryDao.delete(farmId, id)
        firestore.deleteDocument(FirestorePaths.category(farmId, id))
    }

    private fun push(entity: CategoryEntity) {
        firestore.pushDocument(
            path = FirestorePaths.category(entity.farmId, entity.id),
            data = entity.toFirestore(),
            scope = scope,
        ) {
            categoryDao.markSynced(entity.farmId, entity.id, entity.name)
        }
    }

    private fun normalize(name: String): String =
        name.trim().lowercase(Locale.ROOT)
}
