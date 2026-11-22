package com.sge.finca.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sge.finca.data.local.SyncStatus
import com.sge.finca.domain.model.Reminder

@Entity(tableName = "reminders", indices = [Index(value = ["farmId", "dueAt"])])
data class ReminderEntity(
    @PrimaryKey val id: String,
    val farmId: String,
    val title: String,
    val description: String?,
    val dueAt: Long,
    val isCompleted: Boolean,
    val createdBy: String,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
) {
    fun toDomain() = Reminder(
        id = id,
        farmId = farmId,
        title = title,
        description = description,
        dueAt = dueAt,
        isCompleted = isCompleted,
        createdBy = createdBy,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    companion object {
        fun fromDomain(r: Reminder, syncStatus: SyncStatus = SyncStatus.PENDING) =
            ReminderEntity(
                id = r.id,
                farmId = r.farmId,
                title = r.title,
                description = r.description,
                dueAt = r.dueAt,
                isCompleted = r.isCompleted,
                createdBy = r.createdBy,
                createdAt = r.createdAt,
                updatedAt = r.updatedAt,
                syncStatus = syncStatus,
            )
    }
}
