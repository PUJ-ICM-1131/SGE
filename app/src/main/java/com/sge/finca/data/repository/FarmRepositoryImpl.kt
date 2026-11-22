package com.sge.finca.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.sge.finca.data.remote.FirestorePaths
import com.sge.finca.data.remote.toFirestore
import com.sge.finca.domain.model.Farm
import com.sge.finca.domain.model.FarmMember
import com.sge.finca.domain.model.FarmRole
import com.sge.finca.domain.model.InviteCode
import com.sge.finca.domain.repository.CategoryRepository
import com.sge.finca.domain.repository.FarmRepository
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
            inviteCode = InviteCode.generate(),
            createdAt = now,
        )
        val email = user.email ?: ""
        val name = ownerName.trim().ifEmpty { email }

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
                FarmMember(
                    uid = user.uid,
                    farmId = farm.id,
                    role = FarmRole.OWNER,
                    isActive = true,
                    joinedAt = now,
                    displayName = name,
                    email = email,
                ).toFirestore(),
            )
            .set(
                firestore.document(FirestorePaths.user(user.uid)),
                mapOf(
                    "name" to name,
                    "email" to email,
                    "farmId" to farm.id,
                    "role" to FarmRole.OWNER.name,
                    "createdAt" to now,
                ),
                SetOptions.merge(),
            )
            .commit()
            .await()

        categoryRepository.insertDefaults(farm.id)
        return farm
    }

    override suspend fun regenerateInviteCode(farmId: String): String {
        val uid = checkNotNull(auth.currentUser?.uid) {
            "Inicia sesión para regenerar el código"
        }
        val farmReference = firestore.document(FirestorePaths.farm(farmId))

        return firestore.runTransaction { transaction ->
            val snapshot = transaction.get(farmReference)
            check(snapshot.exists()) { "La finca ya no existe" }
            check(snapshot.getString("ownerUid") == uid) {
                "Solo el propietario puede regenerar el código"
            }

            val previousCode = snapshot.getString("inviteCode")
            var newCode = InviteCode.generate()
            while (newCode == previousCode) {
                newCode = InviteCode.generate()
            }

            transaction.update(farmReference, "inviteCode", newCode)
            newCode
        }.await()
    }

    private fun DocumentSnapshot.toFarm() = Farm(
        id = id,
        name = getString("name") ?: "",
        ownerUid = getString("ownerUid") ?: "",
        inviteCode = getString("inviteCode"),
        createdAt = getLong("createdAt") ?: 0L,
    )
}