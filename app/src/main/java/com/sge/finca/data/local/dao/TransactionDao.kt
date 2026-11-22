package com.sge.finca.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sge.finca.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query(
        "SELECT * FROM transactions WHERE farmId = :farmId " +
                "ORDER BY dateEpochDay DESC, createdAt DESC"
    )
    fun observeByFarm(farmId: String): Flow<List<TransactionEntity>>

    /**
     * Historial con filtros opcionales: un parámetro en null no filtra.
     * [type] es el nombre del enum ("INCOME"/"EXPENSE"). Rango de fechas inclusivo.
     */
    @Query(
        "SELECT * FROM transactions WHERE farmId = :farmId " +
                "AND (:fromDay IS NULL OR dateEpochDay >= :fromDay) " +
                "AND (:toDay IS NULL OR dateEpochDay <= :toDay) " +
                "AND (:type IS NULL OR type = :type) " +
                "AND (:categoryId IS NULL OR categoryId = :categoryId) " +
                "ORDER BY dateEpochDay DESC, createdAt DESC"
    )
    fun observeFiltered(
        farmId: String,
        fromDay: Long?,
        toDay: Long?,
        type: String?,
        categoryId: String?,
    ): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id AND farmId = :farmId LIMIT 1")
    suspend fun getById(farmId: String, id: String): TransactionEntity?

    /** Detalle: emite null mientras no exista y vuelve a emitir cuando llega o cambia. */
    @Query("SELECT * FROM transactions WHERE id = :id AND farmId = :farmId LIMIT 1")
    fun observeById(farmId: String, id: String): Flow<TransactionEntity?>

    /** Indica si alguna transacción de la finca utiliza esta categoría. */
    @Query(
        "SELECT EXISTS(SELECT 1 FROM transactions " +
                "WHERE farmId = :farmId AND categoryId = :categoryId)"
    )
    suspend fun hasTransactionsForCategory(
        farmId: String,
        categoryId: String,
    ): Boolean

    /** No pisa una fila existente (p. ej. la que acaba de guardar el listener). -1 si ya existía. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(transaction: TransactionEntity)

    /** Para escribir lo que llega de Firestore (issue #32). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(transactions: List<TransactionEntity>)

    @Update
    suspend fun update(transaction: TransactionEntity)

    /** Solo marca SYNCED si la fila no cambió desde que se envió (mismo updatedAt). */
    @Query(
        "UPDATE transactions SET syncStatus = 'SYNCED' " +
                "WHERE id = :id AND farmId = :farmId AND updatedAt = :updatedAt"
    )
    suspend fun markSynced(farmId: String, id: String, updatedAt: Long)

    @Query("DELETE FROM transactions WHERE id = :id AND farmId = :farmId")
    suspend fun delete(farmId: String, id: String)

    // ---------- Sincronización (issue #32) ----------

    /** Filas por subir a Firestore (de todas las fincas). */
    @Query("SELECT * FROM transactions WHERE syncStatus = 'PENDING'")
    suspend fun getPendingUpload(): List<TransactionEntity>

    /** Firestore la rechazó de forma definitiva. Solo si la fila no cambió mientras se subía. */
    @Query(
        "UPDATE transactions SET syncStatus = 'FAILED' " +
                "WHERE id = :id AND farmId = :farmId AND updatedAt = :updatedAt"
    )
    suspend fun markFailed(farmId: String, id: String, updatedAt: Long)

    /** "Reintentar": las rechazadas vuelven a la cola. */
    @Query("UPDATE transactions SET syncStatus = 'PENDING' WHERE farmId = :farmId AND syncStatus = 'FAILED'")
    suspend fun resetFailed(farmId: String)

    /** [status] es el nombre del enum SyncStatus. */
    @Query("SELECT COUNT(*) FROM transactions WHERE farmId = :farmId AND syncStatus = :status")
    fun observeCountByStatus(farmId: String, status: String): Flow<Int>

    /** Ids ya sincronizados: sirven para detectar lo que se borró en otro celular. */
    @Query("SELECT id FROM transactions WHERE farmId = :farmId AND syncStatus = 'SYNCED'")
    suspend fun getSyncedIds(farmId: String): List<String>
}