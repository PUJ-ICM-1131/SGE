package com.sge.finca.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sge.finca.data.local.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders WHERE farmId = :farmId ORDER BY dueAt ASC")
    fun observeByFarm(farmId: String): Flow<List<ReminderEntity>>

    /** Recordatorios sin completar, del más próximo al más lejano. */
    @Query(
        "SELECT * FROM reminders WHERE farmId = :farmId AND isCompleted = 0 " +
            "ORDER BY dueAt ASC"
    )
    fun observePending(farmId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :id AND farmId = :farmId LIMIT 1")
    suspend fun getById(farmId: String, id: String): ReminderEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(reminder: ReminderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(reminders: List<ReminderEntity>)

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Query(
        "UPDATE reminders SET syncStatus = 'SYNCED' " +
            "WHERE id = :id AND farmId = :farmId AND updatedAt = :updatedAt"
    )
    suspend fun markSynced(farmId: String, id: String, updatedAt: Long)

    @Query("DELETE FROM reminders WHERE id = :id AND farmId = :farmId")
    suspend fun delete(farmId: String, id: String)

    // ---------- Sincronización (issue #32) ----------

    /** Filas por subir a Firestore (de todas las fincas). */
    @Query("SELECT * FROM reminders WHERE syncStatus = 'PENDING'")
    suspend fun getPendingUpload(): List<ReminderEntity>

    /** Firestore la rechazó de forma definitiva. Solo si la fila no cambió mientras se subía. */
    @Query(
        "UPDATE reminders SET syncStatus = 'FAILED' " +
            "WHERE id = :id AND farmId = :farmId AND updatedAt = :updatedAt"
    )
    suspend fun markFailed(farmId: String, id: String, updatedAt: Long)

    /** "Reintentar": las rechazadas vuelven a la cola. */
    @Query("UPDATE reminders SET syncStatus = 'PENDING' WHERE farmId = :farmId AND syncStatus = 'FAILED'")
    suspend fun resetFailed(farmId: String)

    /** [status] es el nombre del enum SyncStatus. */
    @Query("SELECT COUNT(*) FROM reminders WHERE farmId = :farmId AND syncStatus = :status")
    fun observeCountByStatus(farmId: String, status: String): Flow<Int>

    /** Ids ya sincronizados: sirven para detectar lo que se borró en otro celular. */
    @Query("SELECT id FROM reminders WHERE farmId = :farmId AND syncStatus = 'SYNCED'")
    suspend fun getSyncedIds(farmId: String): List<String>
}
