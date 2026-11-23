package com.sge.finca.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.sge.finca.data.remote.FirestorePaths
import com.sge.finca.data.remote.toChatMessage
import com.sge.finca.data.remote.toChatThread
import com.sge.finca.domain.model.ChatMessage
import com.sge.finca.domain.model.ChatThread
import com.sge.finca.domain.model.Permission
import com.sge.finca.domain.repository.ChatRepository
import com.sge.finca.domain.repository.SessionRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * El chat vive solo en Firestore, sin Room: los listeners responden en tiempo real y la caché
 * offline de Firestore muestra la conversación y encola lo enviado sin señal.
 */
@Singleton
class FirestoreChatRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val sessionRepository: SessionRepository,
) : ChatRepository {

    override fun observeThreads(farmId: String): Flow<List<ChatThread>> = callbackFlow {
        val registration = firestore.collection(FirestorePaths.farmCollection(farmId, FirestorePaths.CHATS))
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents.orEmpty().mapNotNull { it.toChatThread() })
            }
        awaitClose { registration.remove() }
    }

    override fun observeMessages(farmId: String, workerUid: String): Flow<List<ChatMessage>> = callbackFlow {
        val registration = firestore.collection(FirestorePaths.chatMessages(farmId, workerUid))
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limitToLast(MESSAGES_LIMIT)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents.orEmpty().mapNotNull { it.toChatMessage() })
            }
        awaitClose { registration.remove() }
    }

    override suspend fun send(farmId: String, workerUid: String, text: String) {
        sessionRepository.requirePermission(farmId, Permission.CHAT)
        val session = sessionRepository.session.value
        val uid = checkNotNull(session.uid) { "Inicia sesión para enviar mensajes" }
        if (!session.isOwner && uid != workerUid) {
            throw SecurityException("Solo puedes escribir en tu conversación con el propietario")
        }
        val clean = text.trim()
        require(clean.isNotEmpty()) { "Escribe un mensaje" }
        require(clean.length <= ChatMessage.MAX_LENGTH) {
            "El mensaje es muy largo (máximo ${ChatMessage.MAX_LENGTH} caracteres)"
        }

        val now = System.currentTimeMillis()
        val chat = firestore.document(FirestorePaths.chat(farmId, workerUid))
        val message = chat.collection(FirestorePaths.MESSAGES).document()
        // Mensaje y resumen de la conversación juntos. Sin esperar al servidor: sin señal quedan
        // en la cola de Firestore y el listener ya muestra el mensaje.
        firestore.batch()
            .set(message, mapOf("senderUid" to uid, "text" to clean, "createdAt" to now))
            .set(
                chat,
                mapOf(
                    "lastMessage" to clean.take(PREVIEW_LENGTH),
                    "lastMessageAt" to now,
                    "lastSenderUid" to uid,
                ),
            )
            .commit()
            .addOnFailureListener { Log.e("SgeChat", "No se pudo enviar el mensaje", it) }
    }

    private companion object {
        const val MESSAGES_LIMIT = 200L
        const val PREVIEW_LENGTH = 200
    }
}
