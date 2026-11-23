package com.sge.finca.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sge.finca.data.remote.FirestorePaths
import com.sge.finca.data.remote.toAnimalAlert
import com.sge.finca.domain.model.AnimalAlert
import com.sge.finca.domain.model.Permission
import com.sge.finca.domain.repository.AlertRepository
import com.sge.finca.domain.repository.SessionRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Las alertas las genera el servidor y solo se leen: no pasan por Room, basta la caché offline
 * de Firestore.
 */
@Singleton
class FirestoreAlertRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val sessionRepository: SessionRepository,
) : AlertRepository {

    override fun observeAlert(farmId: String, alertId: String): Flow<AnimalAlert?> = callbackFlow {
        val registration = firestore.document(FirestorePaths.alert(farmId, alertId))
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toAnimalAlert(farmId))
            }
        awaitClose { registration.remove() }
    }

    override suspend fun forward(farmId: String, alertId: String) {
        sessionRepository.requirePermission(farmId, Permission.FORWARD_ALERTS)
        val uid = checkNotNull(auth.currentUser?.uid) { "Inicia sesión para reenviar la alerta" }
        // Sin esperar al servidor: sin señal queda en la cola de Firestore y se envía al
        // reconectar; el listener ya muestra la alerta como reenviada.
        firestore.document(FirestorePaths.alert(farmId, alertId))
            .update(mapOf("forwardedAt" to System.currentTimeMillis(), "forwardedBy" to uid))
            .addOnFailureListener { Log.e("SgeAlerts", "No se pudo reenviar la alerta $alertId", it) }
    }
}
