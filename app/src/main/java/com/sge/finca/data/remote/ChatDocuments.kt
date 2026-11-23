package com.sge.finca.data.remote

import com.google.firebase.firestore.DocumentSnapshot
import com.sge.finca.domain.model.ChatMessage
import com.sge.finca.domain.model.ChatThread

/*
 * Chat propietario ↔ trabajador (#74):
 *   farms/{farmId}/chats/{workerUid}                lastMessage, lastMessageAt, lastSenderUid
 *   farms/{farmId}/chats/{workerUid}/messages/{id}  senderUid, text, createdAt
 * sge-notifier escucha el documento de la conversación para avisar al otro participante.
 */

fun DocumentSnapshot.toChatMessage(): ChatMessage? {
    return ChatMessage(
        id = id,
        senderUid = getString("senderUid") ?: return null,
        text = getString("text") ?: return null,
        createdAt = getLong("createdAt") ?: 0L,
    )
}

fun DocumentSnapshot.toChatThread(): ChatThread? {
    return ChatThread(
        workerUid = id,
        lastMessage = getString("lastMessage") ?: return null,
        lastMessageAt = getLong("lastMessageAt") ?: 0L,
        lastSenderUid = getString("lastSenderUid") ?: "",
    )
}
