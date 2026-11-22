package com.sge.finca.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sge.finca.data.local.entity.AnimalEntity
import kotlinx.coroutines.flow.Flow

/**
 * Los animales son de la finca (issue #62): todas las consultas filtran por farmId, nunca por
 * quien los registró (ownerUid solo se guarda como dato del creador).
 */
@Dao
interface AnimalDao {

    @Query("SELECT * FROM animals WHERE farmId = :farmId ORDER BY createdAt DESC")
    fun observeByFarm(farmId: String): Flow<List<AnimalEntity>>

    @Query("SELECT * FROM animals WHERE id = :id AND farmId = :farmId LIMIT 1")
    suspend fun getById(farmId: String, id: String): AnimalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnimal(animal: AnimalEntity)

    /** Para escribir lo que llega de Firestore (#32). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(animals: List<AnimalEntity>)

    @Query("""UPDATE animals SET name = :name, notes = :notes, photoUri = :photoUri,
        latitude = :latitude, longitude = :longitude, locationAccuracy = :locationAccuracy,
        createdAt = :createdAt, variant = :variant, updatedAt = :updatedAt, syncStatus = 'PENDING'
        WHERE id = :id AND farmId = :farmId""")
    suspend fun updateAnimal(farmId: String, id: String, name: String, notes: String,
        photoUri: String?, latitude: Double, longitude: Double, locationAccuracy: Float?,
        createdAt: Long, variant: Int, updatedAt: Long): Int

    @Query("DELETE FROM animals WHERE id = :id AND farmId = :farmId")
    suspend fun deleteById(farmId: String, id: String)

    /**
     * Animales registrados antes de #62 (sin finca): pasan a la finca activa de quien los
     * registró y quedan pendientes de subir, para que los vea el resto de la finca.
     * @return cuántos se asignaron.
     */
    @Query("""UPDATE animals SET farmId = :farmId, syncStatus = 'PENDING', updatedAt = :now
        WHERE farmId IS NULL AND ownerUid = :ownerUid""")
    suspend fun claimUnassigned(farmId: String, ownerUid: String, now: Long): Int

    // ---------- Sincronización (#32) ----------

    /** Por subir, de todas las fincas. Los que no tienen finca no tienen a dónde subir. */
    @Query("SELECT * FROM animals WHERE syncStatus = 'PENDING' AND farmId IS NOT NULL")
    suspend fun getPendingUpload(): List<AnimalEntity>

    /** Solo si la fila no cambió desde que se envió (mismo updatedAt). */
    @Query("UPDATE animals SET syncStatus = 'SYNCED' WHERE id = :id AND farmId = :farmId AND updatedAt = :updatedAt")
    suspend fun markSynced(farmId: String, id: String, updatedAt: Long)

    @Query("UPDATE animals SET syncStatus = 'FAILED' WHERE id = :id AND farmId = :farmId AND updatedAt = :updatedAt")
    suspend fun markFailed(farmId: String, id: String, updatedAt: Long)

    @Query("UPDATE animals SET syncStatus = 'PENDING' WHERE farmId = :farmId AND syncStatus = 'FAILED'")
    suspend fun resetFailed(farmId: String)

    /** [status] es el nombre del enum SyncStatus. */
    @Query("SELECT COUNT(*) FROM animals WHERE farmId = :farmId AND syncStatus = :status")
    fun observeCountByStatus(farmId: String, status: String): Flow<Int>

    @Query("SELECT id FROM animals WHERE farmId = :farmId AND syncStatus = 'SYNCED'")
    suspend fun getSyncedIds(farmId: String): List<String>
}
