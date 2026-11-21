package com.sge.finca.domain.repository

import com.sge.finca.domain.model.Reminder
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    fun observeReminders(farmId: String): Flow<List<Reminder>>

    /** Solo los que no están completados, del más próximo al más lejano. */
    fun observePending(farmId: String): Flow<List<Reminder>>

    suspend fun getReminder(farmId: String, id: String): Reminder?

    /** @throws IllegalArgumentException si falta el título. Devuelve el id generado. */
    suspend fun create(farmId: String, title: String, description: String?, dueAt: Long): String

    suspend fun update(reminder: Reminder)
    suspend fun setCompleted(farmId: String, id: String, completed: Boolean)
    suspend fun delete(farmId: String, id: String)
}
