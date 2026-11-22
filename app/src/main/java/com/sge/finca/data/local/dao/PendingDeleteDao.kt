package com.sge.finca.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sge.finca.data.local.entity.PendingDeleteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingDeleteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pendingDelete: PendingDeleteEntity)

    /** Borrados por enviar (de todas las fincas), en el orden en que se hicieron. */
    @Query("SELECT * FROM pending_deletes WHERE failed = 0 ORDER BY createdAt")
    suspend fun getRetryable(): List<PendingDeleteEntity>

    @Query(
        "SELECT EXISTS(SELECT 1 FROM pending_deletes " +
            "WHERE farmId = :farmId AND collection = :collection AND docId = :docId)"
    )
    suspend fun exists(farmId: String, collection: String, docId: String): Boolean

    @Query("DELETE FROM pending_deletes WHERE path = :path")
    suspend fun remove(path: String)

    @Query("UPDATE pending_deletes SET failed = 1 WHERE path = :path")
    suspend fun markFailed(path: String)

    @Query("UPDATE pending_deletes SET failed = 0 WHERE farmId = :farmId AND failed = 1")
    suspend fun resetFailed(farmId: String)

    @Query("SELECT COUNT(*) FROM pending_deletes WHERE farmId = :farmId AND failed = :failed")
    fun observeCount(farmId: String, failed: Boolean): Flow<Int>
}
