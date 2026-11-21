package com.sge.finca.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sge.finca.data.local.entity.FarmContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FarmContactDao {

    @Query("SELECT * FROM farm_contacts WHERE farmId = :farmId ORDER BY name COLLATE NOCASE")
    fun observeByFarm(farmId: String): Flow<List<FarmContactEntity>>

    @Query("SELECT * FROM farm_contacts WHERE id = :id AND farmId = :farmId LIMIT 1")
    suspend fun getById(farmId: String, id: String): FarmContactEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(contact: FarmContactEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(contacts: List<FarmContactEntity>)

    @Update
    suspend fun update(contact: FarmContactEntity)

    @Query(
        "UPDATE farm_contacts SET syncStatus = 'SYNCED' " +
            "WHERE id = :id AND farmId = :farmId AND updatedAt = :updatedAt"
    )
    suspend fun markSynced(farmId: String, id: String, updatedAt: Long)

    @Query("DELETE FROM farm_contacts WHERE id = :id AND farmId = :farmId")
    suspend fun delete(farmId: String, id: String)
}
