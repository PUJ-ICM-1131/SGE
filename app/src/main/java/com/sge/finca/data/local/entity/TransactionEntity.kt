package com.sge.finca.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sge.finca.data.local.SyncStatus
import com.sge.finca.domain.model.Transaction
import com.sge.finca.domain.model.TransactionType

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["farmId", "dateEpochDay"]),
        Index(value = ["farmId", "categoryId"]),
    ],
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val farmId: String,
    val type: TransactionType,
    val amountCents: Long,
    val categoryId: String,
    val categoryName: String,
    val dateEpochDay: Long,
    val note: String?,
    val paymentMethod: String?,
    val photoUri: String?,
    val remotePhotoUrl: String?,
    val createdBy: String,
    val createdByName: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
) {
    fun toDomain() = Transaction(
        id = id,
        farmId = farmId,
        type = type,
        amountCents = amountCents,
        categoryId = categoryId,
        categoryName = categoryName,
        dateEpochDay = dateEpochDay,
        note = note,
        paymentMethod = paymentMethod,
        photoUri = photoUri,
        remotePhotoUrl = remotePhotoUrl,
        createdBy = createdBy,
        createdByName = createdByName,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    companion object {
        fun fromDomain(t: Transaction, syncStatus: SyncStatus = SyncStatus.PENDING) =
            TransactionEntity(
                id = t.id,
                farmId = t.farmId,
                type = t.type,
                amountCents = t.amountCents,
                categoryId = t.categoryId,
                categoryName = t.categoryName,
                dateEpochDay = t.dateEpochDay,
                note = t.note,
                paymentMethod = t.paymentMethod,
                photoUri = t.photoUri,
                remotePhotoUrl = t.remotePhotoUrl,
                createdBy = t.createdBy,
                createdByName = t.createdByName,
                createdAt = t.createdAt,
                updatedAt = t.updatedAt,
                syncStatus = syncStatus,
            )
    }
}
