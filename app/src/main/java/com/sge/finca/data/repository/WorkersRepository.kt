package com.sge.finca.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.sge.finca.data.remote.FirestorePaths
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class WorkerSummary(
    val uid: String,
    val name: String,
    val email: String,
    val isActive: Boolean,
    val joinedAt: Long,
)

@Singleton
class WorkersRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    fun observeWorkers(farmId: String): Flow<List<WorkerSummary>> = callbackFlow {
        val registration = firestore
            .collection("${FirestorePaths.farm(farmId)}/${FirestorePaths.MEMBERS}")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val workers = snapshot?.documents.orEmpty()
                    .filter { it.getString("role") == "WORKER" }
                    .map { document ->
                        WorkerSummary(
                            uid = document.id,
                            name = document.getString("displayName")
                                ?: document.getString("name")
                                ?: "",
                            email = document.getString("email").orEmpty(),
                            isActive = document.getBoolean("isActive") ?: true,
                            joinedAt = document.getLong("joinedAt") ?: 0L,
                        )
                    }
                    .sortedBy { it.name.lowercase() }

                trySend(workers)
            }

        awaitClose { registration.remove() }
    }
}