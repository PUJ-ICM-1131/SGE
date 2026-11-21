package com.sge.finca.domain.repository

import com.sge.finca.domain.model.Category
import com.sge.finca.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeCategories(farmId: String): Flow<List<Category>>
    fun observeCategories(farmId: String, type: TransactionType): Flow<List<Category>>
    suspend fun getCategory(farmId: String, id: String): Category?

    /** Inserta las categorías por defecto. Se puede llamar varias veces sin duplicar. */
    suspend fun insertDefaults(farmId: String)

    /** @throws IllegalArgumentException si el nombre está vacío o ya existe para ese tipo. */
    suspend fun create(farmId: String, name: String, type: TransactionType): String

    /** @throws IllegalArgumentException si no existe, el nombre está vacío o ya existe. */
    suspend fun rename(farmId: String, id: String, newName: String)

    suspend fun delete(farmId: String, id: String)
}
