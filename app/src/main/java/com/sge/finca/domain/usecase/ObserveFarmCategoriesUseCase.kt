package com.sge.finca.domain.usecase

import android.util.Log
import com.sge.finca.domain.model.Category
import com.sge.finca.domain.repository.CategoryRepository
import com.sge.finca.domain.repository.SessionRepository
import com.sge.finca.domain.repository.UserRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withTimeoutOrNull
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
 * Si la finca no tiene categorías en este celular:
 * - el **propietario** inserta las por defecto (ids estables: no se duplican entre celulares);
 * - el **trabajador** las descarga de Firestore, porque las reglas (#28) solo dejan crearlas al
 *   dueño. Así ve también las categorías propias que el dueño haya creado.
 */
class ObserveFarmCategoriesUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val categoryRepository: CategoryRepository,
    private val sessionRepository: SessionRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<FarmCategories?> =
        userRepository.observeCurrentFarmId().flatMapLatest { farmId ->
            if (farmId == null) {
                flowOf(null)
            } else {
                categoryRepository.observeCategories(farmId)
                    .onEach { if (it.isEmpty()) loadMissing(farmId) }
                    .map { FarmCategories(farmId, it) }
            }
        }

    private suspend fun loadMissing(farmId: String) {
        // Se espera a conocer el rol de esta finca (sin bloquear para siempre si no hay red).
        val session = withTimeoutOrNull(10_000) {
            sessionRepository.session.first { !it.loading && it.farmId == farmId && it.role != null }
        } ?: return
        // Pocos reintentos: justo después de unirse, la membresía puede tardar en confirmarse
        // en el servidor y la primera lectura ser rechazada.
        repeat(ATTEMPTS) { attempt ->
            try {
                if (session.isOwner) {
                    categoryRepository.insertDefaults(farmId)
                } else {
                    categoryRepository.pullFromRemote(farmId)
                }
                return
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("SgeCategories", "No se pudieron cargar las categorías (intento ${attempt + 1})", e)
                delay(2_000L * (attempt + 1))
            }
        }
    }

    private companion object {
        const val ATTEMPTS = 3
    }
}
