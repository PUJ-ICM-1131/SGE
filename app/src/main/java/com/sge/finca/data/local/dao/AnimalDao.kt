package com.sge.finca.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sge.finca.data.local.entity.AnimalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimalDao {

    @Query("SELECT * FROM animals ORDER BY createdAt DESC")
    fun observeAnimals(): Flow<List<AnimalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnimal(animal: AnimalEntity)

    @Query("SELECT * FROM animals WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): AnimalEntity?

    @Update
    suspend fun updateAnimal(animal: AnimalEntity)

    @Query("DELETE FROM animals WHERE id = :id")
    suspend fun deleteById(id: String)
}
