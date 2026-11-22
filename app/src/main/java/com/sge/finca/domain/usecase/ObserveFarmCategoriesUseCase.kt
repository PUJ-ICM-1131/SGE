package com.sge.finca.domain.usecase

import android.util.Log
import com.sge.finca.domain.model.Category
import com.sge.finca.domain.repository.CategoryRepository
import com.sge.finca.domain.repository.UserRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/** Finca activa del usuario con sus categorías económicas. */
data class FarmCategories(
    val farmId: String,
    val categories: List<Category>,
)

/**
 * Finca del usuario y sus categorías, actualizándose solas.
 * Emite null si no hay sesión o el usuario aún no tiene finca.
 *
 * Si la finca no tiene categorías en este celular (por ejemplo, un trabajador que entra
 * por primera vez antes de sincronizar), inserta las por defecto. Es seguro: usan ids estables,
 * así que coinciden con las del propietario y no se duplican.
 */
class ObserveFarmCategoriesUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val categoryRepository: CategoryRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<FarmCategories?> =
        userRepository.observeCurrentFarmId().flatMapLatest { farmId ->
            if (farmId == null) {
                flowOf(null)
            } else {
                categoryRepository.observeCategories(farmId)
                    .onEach { if (it.isEmpty()) insertDefaults(farmId) }
                    .map { FarmCategories(farmId, it) }
            }
        }

    private suspend fun insertDefaults(farmId: String) {
        try {
            categoryRepository.insertDefaults(farmId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("SgeCategories", "No se pudieron crear las categorías por defecto", e)
        }
    }
}
