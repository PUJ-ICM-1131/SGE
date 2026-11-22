package com.sge.finca.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sge.finca.data.local.entity.AnimalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimalDao {

    @Query("SELECT * FROM animals WHERE ownerUid = :ownerUid ORDER BY createdAt DESC")
    fun observeAnimals(ownerUid: String): Flow<List<AnimalEntity>>

    @Query("SELECT * FROM animals WHERE farmId = :farmId ORDER BY createdAt DESC")
    fun observeByFarm(farmId: String): Flow<List<AnimalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnimal(animal: AnimalEntity)

    @Query("SELECT * FROM animals WHERE id = :id AND ownerUid = :ownerUid LIMIT 1")
    suspend fun getById(id: String, ownerUid: String): AnimalEntity?

    @Query("""UPDATE animals SET name = :name, notes = :notes, photoUri = :photoUri,
        latitude = :latitude, longitude = :longitude, locationAccuracy = :locationAccuracy,
        createdAt = :createdAt, variant = :variant, syncStatus = 'PENDING'
        WHERE id = :id AND ownerUid = :ownerUid""")
    suspend fun updateAnimal(id: String, ownerUid: String, name: String, notes: String,
        photoUri: String?, latitude: Double, longitude: Double, locationAccuracy: Float?,
        createdAt: Long, variant: Int): Int

    @Query("DELETE FROM animals WHERE id = :id AND ownerUid = :ownerUid")
    suspend fun deleteById(id: String, ownerUid: String)
}
