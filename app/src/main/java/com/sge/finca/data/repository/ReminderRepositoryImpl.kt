package com.sge.finca.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sge.finca.data.injection.ApplicationScope
import com.sge.finca.data.local.SyncStatus
import com.sge.finca.data.local.dao.ReminderDao
import com.sge.finca.data.local.entity.ReminderEntity
import com.sge.finca.data.remote.FirestorePaths
import com.sge.finca.data.remote.deleteDocument
import com.sge.finca.data.remote.pushDocument
import com.sge.finca.data.remote.toFirestore
import com.sge.finca.domain.model.Permission
import com.sge.finca.domain.model.Reminder
import com.sge.finca.domain.repository.ReminderRepository
import com.sge.finca.domain.repository.SessionRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Solo datos. Programar la notificación local (WorkManager/AlarmManager) es el issue #31,
 * que debe llamar a este repositorio y agendar a partir de [Reminder.dueAt].
 */
@Singleton
class ReminderRepositoryImpl @Inject constructor(
    private val dao: ReminderDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val sessionRepository: SessionRepository,
    @ApplicationScope private val scope: CoroutineScope,
) : ReminderRepository {

    override fun observeReminders(farmId: String): Flow<List<Reminder>> {
        require(farmId.isNotBlank()) { "La finca es obligatoria" }
        return dao.observeByFarm(farmId).map { list -> list.map { it.toDomain() } }
    }

    override fun observePending(farmId: String): Flow<List<Reminder>> {
        require(farmId.isNotBlank()) { "La finca es obligatoria" }
        return dao.observePending(farmId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getReminder(farmId: String, id: String): Reminder? =
        dao.getById(farmId, id)?.toDomain()

    override suspend fun create(
        farmId: String,
        title: String,
        description: String?,
        dueAt: Long,
    ): String {
        sessionRepository.requirePermission(farmId, Permission.MANAGE_REMINDERS)
        require(farmId.isNotBlank()) { "La finca es obligatoria" }
        val uid = checkNotNull(auth.currentUser?.uid) { "Inicia sesión para crear recordatorios" }
        val cleanTitle = title.trim()
        require(cleanTitle.isNotEmpty()) { "Escribe un título" }
        val now = System.currentTimeMillis()

        val entity = ReminderEntity(
            id = UUID.randomUUID().toString(),
            farmId = farmId,
            title = cleanTitle,
            description = description?.trim()?.takeIf { it.isNotEmpty() },
            dueAt = dueAt,
            isCompleted = false,
            createdBy = uid,
            createdAt = now,
            updatedAt = now,
        )
        dao.insert(entity)
        push(entity)
        return entity.id
    }

    override suspend fun update(reminder: Reminder) {
        sessionRepository.requirePermission(reminder.farmId, Permission.MANAGE_REMINDERS)
        val current = dao.getById(reminder.farmId, reminder.id)
            ?: throw IllegalArgumentException("Recordatorio no encontrado")
        val cleanTitle = reminder.title.trim()
        require(cleanTitle.isNotEmpty()) { "Escribe un título" }

        save(
            current.copy(
                title = cleanTitle,
                description = reminder.description?.trim()?.takeIf { it.isNotEmpty() },
                dueAt = reminder.dueAt,
                isCompleted = reminder.isCompleted,
            )
        )
    }

    override suspend fun setCompleted(farmId: String, id: String, completed: Boolean) {
        sessionRepository.requirePermission(farmId, Permission.MANAGE_REMINDERS)
        val current = dao.getById(farmId, id)
            ?: throw IllegalArgumentException("Recordatorio no encontrado")
        if (current.isCompleted == completed) return
        save(current.copy(isCompleted = completed))
    }

    override suspend fun delete(farmId: String, id: String) {
        sessionRepository.requirePermission(farmId, Permission.MANAGE_REMINDERS)
        dao.delete(farmId, id)
        firestore.deleteDocument(FirestorePaths.reminder(farmId, id))
    }

    private suspend fun save(changed: ReminderEntity) {
        val updated = changed.copy(
            updatedAt = maxOf(System.currentTimeMillis(), changed.updatedAt + 1),
            syncStatus = SyncStatus.PENDING,
        )
        dao.update(updated)
        push(updated)
    }

    private fun push(entity: ReminderEntity) {
        firestore.pushDocument(
            path = FirestorePaths.reminder(entity.farmId, entity.id),
            data = entity.toFirestore(),
            scope = scope,
        ) {
            dao.markSynced(entity.farmId, entity.id, entity.updatedAt)
        }
    }
}
