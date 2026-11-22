package com.sge.finca.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sge.finca.data.local.SyncStatus
import com.sge.finca.domain.model.FarmContact

@Entity(tableName = "farm_contacts", indices = [Index("farmId")])
data class FarmContactEntity(
    @PrimaryKey val id: String,
    val farmId: String,
    val name: String,
    val phone: String?,
    val email: String?,
    val notes: String?,
    val createdBy: String,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
) {
    fun toDomain() = FarmContact(
        id = id,
        farmId = farmId,
        name = name,
        phone = phone,
        email = email,
        notes = notes,
        createdBy = createdBy,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    companion object {
        fun fromDomain(c: FarmContact, syncStatus: SyncStatus = SyncStatus.PENDING) =
            FarmContactEntity(
                id = c.id,
                farmId = c.farmId,
                name = c.name,
                phone = c.phone,
                email = c.email,
                notes = c.notes,
                createdBy = c.createdBy,
                createdAt = c.createdAt,
                updatedAt = c.updatedAt,
                syncStatus = syncStatus,
            )
    }
}
