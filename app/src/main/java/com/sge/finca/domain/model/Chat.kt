package com.sge.finca.domain.model

/**
 * Chat 1 a 1 entre el propietario y un trabajador (#74). Cada conversación se identifica con el
 * uid del trabajador: farms/{farmId}/chats/{workerUid}/messages/{id}.
 */
data class ChatMessage(
    val id: String,
    val senderUid: String,
    val text: String,
    val createdAt: Long,
) {
    companion object {
        const val MAX_LENGTH = 1000
    }
}

/** Resumen de una conversación, para la lista de Mensajes del propietario. */
data class ChatThread(
    val workerUid: String,
    val lastMessage: String,
    val lastMessageAt: Long,
    val lastSenderUid: String,
)
