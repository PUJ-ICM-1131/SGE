package com.sge.finca.domain.repository

import com.sge.finca.domain.model.ChatMessage
import com.sge.finca.domain.model.ChatThread
import kotlinx.coroutines.flow.Flow

/** Chat propietario ↔ trabajador (#74). */
interface ChatRepository {
    /** Conversaciones de la finca; solo el propietario puede listarlas todas. */
    fun observeThreads(farmId: String): Flow<List<ChatThread>>

    /** Mensajes de la conversación con [workerUid], del más viejo al más nuevo. */
    fun observeMessages(farmId: String, workerUid: String): Flow<List<ChatMessage>>

    /**
     * Envía [text] en la conversación con [workerUid]. Un trabajador solo escribe en la suya.
     * @throws IllegalArgumentException si el texto está vacío o es demasiado largo.
     * @throws SecurityException si el usuario no participa en esa conversación.
     */
    suspend fun send(farmId: String, workerUid: String, text: String)
}
