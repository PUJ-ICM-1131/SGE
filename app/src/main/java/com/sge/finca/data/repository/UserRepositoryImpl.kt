package com.sge.finca.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.sge.finca.data.remote.FirestorePaths
import com.sge.finca.domain.model.UserProfile
import com.sge.finca.domain.model.FarmRole
import com.sge.finca.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * El perfil vive solo en Firestore (users/{uid}); no hace falta copiarlo a Room porque
 * Firestore tiene caché offline propia y el listener responde desde ella sin red.
 */
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : UserRepository {

    override val currentUserId: String?
        get() = auth.currentUser?.uid

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeCurrentUser(): Flow<UserProfile?> =
        observeUid().flatMapLatest { uid ->
            if (uid == null) flowOf(null) else observeProfile(uid)
        }

    override fun observeCurrentFarmId(): Flow<String?> =
        observeCurrentUser().map { it?.farmId }.distinctUntilChanged()

    private fun observeUid(): Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    private fun observeProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        val registration = firestore.document(FirestorePaths.user(uid))
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Por ejemplo, permisos de Security Rules. No se cierra el flujo:
                    // se emite null y la UI lo trata como "sin finca".
                    Log.w("SgeUser", "No se pudo leer el perfil", error)
                    trySend(null)
                    return@addSnapshotListener
                }
                trySend(snapshot?.takeIf { it.exists() }?.toUserProfile())
            }
        awaitClose { registration.remove() }
    }

    private fun DocumentSnapshot.toUserProfile() = UserProfile(
        uid = id,
        name = getString("name") ?: "",
        email = getString("email") ?: "",
        farmId = getString("farmId"),
        role = getString("role")?.let { raw -> FarmRole.entries.firstOrNull { it.name == raw } },
        createdAt = getLong("createdAt") ?: 0L,
    )
}
