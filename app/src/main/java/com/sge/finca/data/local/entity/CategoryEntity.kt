package com.sge.finca.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sge.finca.data.local.SyncStatus
import com.sge.finca.domain.model.Category
import com.sge.finca.domain.model.TransactionType

@Entity(
    tableName = "categories",
    indices = [
        Index(value = ["farmId", "type", "normalizedName"], unique = true)
    ]
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    val farmId: String,
    val name: String,
    val normalizedName: String,
    val type: TransactionType,
    val isDefault: Boolean,
    val syncStatus: SyncStatus = SyncStatus.PENDING
) {
    fun toDomain() = Category(
        id = id,
        farmId = farmId,
        name = name,
        type = type,
        isDefault = isDefault,
    )
}
