package com.sge.finca.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.sge.finca.data.remote.FirestorePaths
import com.sge.finca.domain.model.Farm
import com.sge.finca.domain.model.UserRole
import com.sge.finca.domain.repository.CategoryRepository
import com.sge.finca.domain.repository.FarmRepository
import java.security.SecureRandom
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

@Singleton
class FarmRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val categoryRepository: CategoryRepository,
) : FarmRepository {

    override fun observeFarm(farmId: String): Flow<Farm?> = callbackFlow {
        val registration = firestore.document(FirestorePaths.farm(farmId))
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("SgeFarm", "No se pudo leer la finca $farmId", error)
                    trySend(null)
                    return@addSnapshotListener
                }
                trySend(snapshot?.takeIf { it.exists() }?.toFarm())
            }
        awaitClose { registration.remove() }
    }

    override suspend fun createFarm(farmName: String, ownerName: String): Farm {
        val user = checkNotNull(auth.currentUser) { "Inicia sesión para crear la finca" }
        val cleanName = farmName.trim()
        require(cleanName.isNotEmpty()) { "El nombre de la finca es obligatorio" }

        val now = System.currentTimeMillis()
        val farm = Farm(
            id = UUID.randomUUID().toString(),
            name = cleanName,
            ownerUid = user.uid,
            inviteCode = generateInviteCode(),
            createdAt = now,
        )
        val email = user.email ?: ""
        val name = ownerName.trim().ifEmpty { email }

        // Un solo batch: o se crean la finca, la membresía y el perfil, o no se crea nada.
        firestore.batch()
            .set(
                firestore.document(FirestorePaths.farm(farm.id)),
                mapOf(
                    "name" to farm.name,
                    "ownerUid" to farm.ownerUid,
                    "inviteCode" to farm.inviteCode,
                    "createdAt" to farm.createdAt,
                ),
            )
            .set(
                firestore.document(FirestorePaths.member(farm.id, user.uid)),
                mapOf(
                    "role" to UserRole.OWNER.name,
                    "name" to name,
                    "email" to email,
                    "joinedAt" to now,
                ),
            )
            .set(
                firestore.document(FirestorePaths.user(user.uid)),
                mapOf(
                    "name" to name,
                    "email" to email,
                    "farmId" to farm.id,
                    "role" to UserRole.OWNER.name,
                    "createdAt" to now,
                ),
                SetOptions.merge(),
            )
            .commit()
            .await()

        categoryRepository.insertDefaults(farm.id)
        return farm
    }

    private fun DocumentSnapshot.toFarm() = Farm(
        id = id,
        name = getString("name") ?: "",
        ownerUid = getString("ownerUid") ?: "",
        inviteCode = getString("inviteCode"),
        createdAt = getLong("createdAt") ?: 0L,
    )

    /**
     * Código de 6 caracteres sin letras/números confundibles (0/O, 1/I/L).
     * Con 31^6 ≈ 890 millones de combinaciones una colisión es muy improbable; buscar la finca
     * por código y manejar ese caso es parte del issue #29.
     */
    private fun generateInviteCode(): String {
        val random = SecureRandom()
        return (1..INVITE_CODE_LENGTH)
            .map { INVITE_ALPHABET[random.nextInt(INVITE_ALPHABET.length)] }
            .joinToString("")
    }

    private companion object {
        const val INVITE_CODE_LENGTH = 6
        const val INVITE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
    }
}
