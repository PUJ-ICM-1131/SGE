package com.sge.finca.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sge.finca.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories WHERE farmId = :farmId ORDER BY type, name COLLATE NOCASE")
    fun observeByFarm(farmId: String): Flow<List<CategoryEntity>>

    /** [type] es el nombre del enum ("INCOME"/"EXPENSE"). */
    @Query(
        "SELECT * FROM categories WHERE farmId = :farmId AND type = :type " +
                "ORDER BY name COLLATE NOCASE"
    )
    fun observeByFarmAndType(farmId: String, type: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE farmId = :farmId")
    suspend fun getByFarm(farmId: String): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id AND farmId = :farmId LIMIT 1")
    suspend fun getById(farmId: String, id: String): CategoryEntity?

    @Query(
        "SELECT EXISTS(SELECT 1 FROM categories " +
                "WHERE farmId = :farmId AND type = :type " +
                "AND normalizedName = :name AND id != :excludeId)"
    )
    suspend fun existsName(
        farmId: String,
        type: String,
        name: String,
        excludeId: String
    ): Boolean

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(categories: List<CategoryEntity>)

    @Update
    suspend fun update(category: CategoryEntity)

    @Query(
        "UPDATE categories SET syncStatus = 'SYNCED' " +
                "WHERE id = :id AND farmId = :farmId AND name = :name"
    )
    suspend fun markSynced(farmId: String, id: String, name: String)

    @Query("DELETE FROM categories WHERE id = :id AND farmId = :farmId")
    suspend fun delete(farmId: String, id: String)

    // ---------- Sincronización (issue #32) ----------

    /** Filas por subir a Firestore (de todas las fincas). */
    @Query("SELECT * FROM categories WHERE syncStatus = 'PENDING'")
    suspend fun getPendingUpload(): List<CategoryEntity>

    /** Firestore la rechazó de forma definitiva. Solo si la fila no cambió mientras se subía. */
    @Query(
        "UPDATE categories SET syncStatus = 'FAILED' " +
            "WHERE id = :id AND farmId = :farmId AND name = :name"
    )
    suspend fun markFailed(farmId: String, id: String, name: String)

    /** "Reintentar": las rechazadas vuelven a la cola. */
    @Query("UPDATE categories SET syncStatus = 'PENDING' WHERE farmId = :farmId AND syncStatus = 'FAILED'")
    suspend fun resetFailed(farmId: String)

    /** [status] es el nombre del enum SyncStatus. */
    @Query("SELECT COUNT(*) FROM categories WHERE farmId = :farmId AND syncStatus = :status")
    fun observeCountByStatus(farmId: String, status: String): Flow<Int>

    /** Ids ya sincronizados: sirven para detectar lo que se borró en otro celular. */
    @Query("SELECT id FROM categories WHERE farmId = :farmId AND syncStatus = 'SYNCED'")
    suspend fun getSyncedIds(farmId: String): List<String>
}
