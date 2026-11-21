package com.sge.finca.data.repository

import com.sge.finca.data.local.dao.CategoryDao
import com.sge.finca.data.local.entity.CategoryEntity
import com.sge.finca.domain.model.DefaultCategories
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) {
    fun observeByFarm(farmId: String): Flow<List<CategoryEntity>> {
        require(farmId.isNotBlank()) { "La finca es obligatoria" }
        return categoryDao.observeByFarm(farmId)
    }

    suspend fun insertDefaults(farmId: String) {
        require(farmId.isNotBlank()) { "La finca es obligatoria" }

        for (item in DefaultCategories.items) {
            val id = UUID.nameUUIDFromBytes(
                "$farmId:${item.type}:${normalize(item.name)}"
                    .toByteArray(Charsets.UTF_8)
            ).toString()

            // El ID estable evita volver a insertar una categoría
            // predeterminada si esta función se llama otra vez.
            if (categoryDao.getById(farmId, id) != null) continue

            if (categoryDao.existsName(
                    farmId,
                    item.type,
                    normalize(item.name),
                    ""
                )
            ) continue

            categoryDao.insert(
                CategoryEntity(
                    id = id,
                    farmId = farmId,
                    name = item.name,
                    normalizedName = normalize(item.name),
                    type = item.type,
                    isDefault = true
                )
            )
        }
    }

    suspend fun create(farmId: String, name: String, type: String) {
        require(farmId.isNotBlank()) { "La finca es obligatoria" }
        require(type == "INCOME" || type == "EXPENSE") {
            "Selecciona un tipo válido"
        }

        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "Escribe un nombre" }

        if (categoryDao.existsName(farmId, type, normalize(cleanName), "")) {
            throw IllegalArgumentException(
                "Ya existe una categoría con ese nombre y tipo"
            )
        }

        categoryDao.insert(
            CategoryEntity(
                id = UUID.randomUUID().toString(),
                farmId = farmId,
                name = cleanName,
                normalizedName = normalize(cleanName),
                type = type,
                isDefault = false
            )
        )
    }

    suspend fun rename(farmId: String, id: String, newName: String) {
        val category = categoryDao.getById(farmId, id)
            ?: throw IllegalArgumentException("Categoría no encontrada")

        val cleanName = newName.trim()
        require(cleanName.isNotEmpty()) { "Escribe un nombre" }

        if (categoryDao.existsName(
                farmId,
                category.type,
                normalize(cleanName),
                id
            )
        ) {
            throw IllegalArgumentException(
                "Ya existe una categoría con ese nombre y tipo"
            )
        }

        categoryDao.update(
            category.copy(
                name = cleanName,
                normalizedName = normalize(cleanName),
                syncStatus = "PENDING"
            )
        )
    }

    private fun normalize(name: String): String =
        name.trim().lowercase(Locale.ROOT)
}