package com.sge.finca.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sge.finca.data.local.entity.PaddockEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaddockDao {

    @Query("SELECT * FROM paddocks WHERE farmId = :farmId ORDER BY name COLLATE NOCASE")
    fun observeByFarm(farmId: String): Flow<List<PaddockEntity>>

    @Query("SELECT * FROM paddocks WHERE id = :id AND farmId = :farmId LIMIT 1")
    suspend fun getById(farmId: String, id: String): PaddockEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(paddock: PaddockEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(paddocks: List<PaddockEntity>)

    @Update
    suspend fun update(paddock: PaddockEntity)

    @Query(
        "UPDATE paddocks SET syncStatus = 'SYNCED' " +
            "WHERE id = :id AND farmId = :farmId AND updatedAt = :updatedAt"
    )
    suspend fun markSynced(farmId: String, id: String, updatedAt: Long)

    @Query("DELETE FROM paddocks WHERE id = :id AND farmId = :farmId")
    suspend fun delete(farmId: String, id: String)

    // ---------- Sincronización (issue #32) ----------

    @Query("SELECT * FROM paddocks WHERE syncStatus = 'PENDING'")
    suspend fun getPendingUpload(): List<PaddockEntity>

    @Query(
        "UPDATE paddocks SET syncStatus = 'FAILED' " +
            "WHERE id = :id AND farmId = :farmId AND updatedAt = :updatedAt"
    )
    suspend fun markFailed(farmId: String, id: String, updatedAt: Long)

    @Query("UPDATE paddocks SET syncStatus = 'PENDING' WHERE farmId = :farmId AND syncStatus = 'FAILED'")
    suspend fun resetFailed(farmId: String)

    /** [status] es el nombre del enum SyncStatus. */
    @Query("SELECT COUNT(*) FROM paddocks WHERE farmId = :farmId AND syncStatus = :status")
    fun observeCountByStatus(farmId: String, status: String): Flow<Int>

    @Query("SELECT id FROM paddocks WHERE farmId = :farmId AND syncStatus = 'SYNCED'")
    suspend fun getSyncedIds(farmId: String): List<String>
}
