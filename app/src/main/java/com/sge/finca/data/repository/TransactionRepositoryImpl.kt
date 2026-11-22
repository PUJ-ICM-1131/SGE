package com.sge.finca.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sge.finca.data.injection.ApplicationScope
import com.sge.finca.data.local.SyncStatus
import com.sge.finca.data.local.dao.CategoryDao
import com.sge.finca.data.local.dao.TransactionDao
import com.sge.finca.data.local.entity.CategoryEntity
import com.sge.finca.data.local.entity.TransactionEntity
import com.sge.finca.data.remote.FirestorePaths
import com.sge.finca.data.remote.deleteDocument
import com.sge.finca.data.remote.pushDocument
import com.sge.finca.data.remote.toFirestore
import com.sge.finca.domain.model.Transaction
import com.sge.finca.domain.model.TransactionFilter
import com.sge.finca.domain.model.TransactionInput
import com.sge.finca.domain.repository.TransactionRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Lecturas desde Room; escrituras en Room y luego a Firestore (write-through, ver ADR-003).
 * Toda la validación se hace aquí, antes de escribir, para que a Firestore llegue data limpia.
 */
@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @ApplicationScope private val scope: CoroutineScope,
) : TransactionRepository {

    override fun observeTransactions(
        farmId: String,
        filter: TransactionFilter,
    ): Flow<List<Transaction>> {
        require(farmId.isNotBlank()) { "La finca es obligatoria" }
        return transactionDao.observeFiltered(
            farmId = farmId,
            fromDay = filter.fromEpochDay,
            toDay = filter.toEpochDay,
            type = filter.type?.name,
            categoryId = filter.categoryId,
        ).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getTransaction(farmId: String, id: String): Transaction? =
        transactionDao.getById(farmId, id)?.toDomain()

    override suspend fun create(farmId: String, input: TransactionInput): String {
        require(farmId.isNotBlank()) { "La finca es obligatoria" }
        val user = checkNotNull(auth.currentUser) { "Inicia sesión para registrar movimientos" }
        val category = validate(farmId, input)
        val now = System.currentTimeMillis()

        val entity = TransactionEntity(
            id = UUID.randomUUID().toString(),
            farmId = farmId,
            type = input.type,
            amountCents = input.amountCents,
            categoryId = category.id,
            categoryName = category.name,
            dateEpochDay = input.dateEpochDay,
            note = input.note.cleanOrNull(),
            paymentMethod = input.paymentMethod.cleanOrNull(),
            photoUri = input.photoUri,
            remotePhotoUrl = null,
            createdBy = user.uid,
            createdByName = user.displayName ?: user.email,
            createdAt = now,
            updatedAt = now,
        )
        transactionDao.insert(entity)
        push(entity)
        return entity.id
    }

    override suspend fun update(farmId: String, id: String, input: TransactionInput) {
        val current = transactionDao.getById(farmId, id)
            ?: throw IllegalArgumentException("Movimiento no encontrado")
        val category = validate(farmId, input)

        val updated = current.copy(
            type = input.type,
            amountCents = input.amountCents,
            categoryId = category.id,
            categoryName = category.name,
            dateEpochDay = input.dateEpochDay,
            note = input.note.cleanOrNull(),
            paymentMethod = input.paymentMethod.cleanOrNull(),
            photoUri = input.photoUri,
            // Si cambió la foto local, la URL remota anterior ya no corresponde.
            remotePhotoUrl = if (input.photoUri == current.photoUri) current.remotePhotoUrl else null,
            // max() evita que dos ediciones en el mismo milisegundo compartan updatedAt.
            updatedAt = maxOf(System.currentTimeMillis(), current.updatedAt + 1),
            syncStatus = SyncStatus.PENDING,
        )
        transactionDao.update(updated)
        push(updated)
    }

    override suspend fun delete(farmId: String, id: String) {
        transactionDao.delete(farmId, id)
        firestore.deleteDocument(FirestorePaths.transaction(farmId, id))
    }

    /** Valida las reglas de negocio y devuelve la categoría elegida. */
    private suspend fun validate(farmId: String, input: TransactionInput): CategoryEntity {
        require(input.amountCents > 0) { "El monto debe ser mayor que cero" }
        require(input.amountCents <= MAX_AMOUNT_CENTS) { "El monto es demasiado grande" }
        val category = categoryDao.getById(farmId, input.categoryId)
            ?: throw IllegalArgumentException("Selecciona una categoría")
        require(category.type == input.type) {
            "La categoría no corresponde al tipo de movimiento"
        }
        return category
    }

    private fun push(entity: TransactionEntity) {
        firestore.pushDocument(
            path = FirestorePaths.transaction(entity.farmId, entity.id),
            data = entity.toFirestore(),
            scope = scope,
        ) {
            transactionDao.markSynced(entity.farmId, entity.id, entity.updatedAt)
        }
    }

    private fun String?.cleanOrNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

    private companion object {
        /** 10.000 millones de pesos: tope de cordura contra errores de digitación. */
        const val MAX_AMOUNT_CENTS = 1_000_000_000_000L
    }
}
