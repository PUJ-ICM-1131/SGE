package com.sge.finca.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.sge.finca.data.remote.FirestorePaths
import com.sge.finca.data.remote.toFirestore
import com.sge.finca.domain.model.Farm
import com.sge.finca.domain.model.FarmMember
import com.sge.finca.domain.model.FarmRole
import com.sge.finca.domain.model.InviteCode
import com.sge.finca.domain.repository.CategoryRepository
import com.sge.finca.domain.repository.FarmRepository
import com.sge.finca.domain.repository.JoinedFarm
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

        // Un solo batch: o se crean la finca, la membresía, el perfil y el índice del código,
        // o no se crea nada.
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
            .set(
                firestore.document(FirestorePaths.inviteCode(checkNotNull(farm.inviteCode))),
                inviteIndex(farm.id, farm.name),
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
            // En una transacción todas las lecturas van antes de las escrituras.
            val previousIndex = previousCode?.let { firestore.document(FirestorePaths.inviteCode(it)) }
            val previousIndexExists = previousIndex?.let { transaction.get(it).exists() } ?: false

            var newCode = InviteCode.generate()
            while (newCode == previousCode) {
                newCode = InviteCode.generate()
            }

            transaction.update(farmReference, "inviteCode", newCode)
            // El código anterior deja de servir: se borra del índice y se publica el nuevo.
            if (previousIndexExists) transaction.delete(checkNotNull(previousIndex))
            transaction.set(
                firestore.document(FirestorePaths.inviteCode(newCode)),
                inviteIndex(farmId, snapshot.getString("name").orEmpty()),
            )
            newCode
        }.await()
    }

    override suspend fun joinFarm(inviteCode: String, displayName: String): JoinedFarm {
        val user = checkNotNull(auth.currentUser) { "Inicia sesión para unirte a una finca" }
        val code = InviteCode.normalize(inviteCode)
        require(InviteCode.isValid(code)) {
            "El código debe tener el formato XXXX-XXXX (o 6 caracteres si es un código anterior)"
        }

        // 1) Buscar la finca por el código. Cualquier usuario con sesión puede leer un código
        //    que ya conoce, pero no listarlos (ver firebase/firestore.rules).
        val index = try {
            firestore.document(FirestorePaths.inviteCode(code)).get().await()
        } catch (e: FirebaseFirestoreException) {
            throw IllegalStateException(e.toJoinMessage(), e)
        }
        val farmId = index.getString("farmId")
        require(index.exists() && !farmId.isNullOrBlank()) {
            "Ese código no corresponde a ninguna finca. Pídele al propietario el código actual"
        }
        val farmName = index.getString("farmName").orEmpty()

        // 2) Crear la membresía WORKER y apuntar el perfil a la finca, juntos. Las reglas
        //    verifican que inviteCode sea el código vigente de la finca.
        val email = user.email.orEmpty()
        val name = displayName.trim().ifEmpty { user.displayName ?: email }
        val now = System.currentTimeMillis()
        try {
            firestore.batch()
                .set(
                    firestore.document(FirestorePaths.member(farmId, user.uid)),
                    FarmMember(
                        uid = user.uid,
                        farmId = farmId,
                        role = FarmRole.WORKER,
                        isActive = true,
                        joinedAt = now,
                        displayName = name,
                        email = email,
                    ).toFirestore() + ("inviteCode" to code),
                )
                .set(
                    firestore.document(FirestorePaths.user(user.uid)),
                    mapOf(
                        "name" to name,
                        "email" to email,
                        "farmId" to farmId,
                        "role" to FarmRole.WORKER.name,
                    ),
                    SetOptions.merge(),
                )
                .commit()
                .await()
        } catch (e: FirebaseFirestoreException) {
            throw IllegalStateException(e.toJoinMessage(), e)
        }
        return JoinedFarm(farmId, farmName)
    }

    override suspend fun ensureInviteCodeIndex(farmId: String) {
        val farm = firestore.document(FirestorePaths.farm(farmId)).get().await()
        val code = farm.getString("inviteCode") ?: return
        val index = firestore.document(FirestorePaths.inviteCode(code))
        if (!index.get().await().exists()) {
            index.set(inviteIndex(farmId, farm.getString("name").orEmpty())).await()
        }
    }

    /** Lo único que expone el índice: a qué finca lleva el código (nada de datos internos). */
    private fun inviteIndex(farmId: String, farmName: String) =
        mapOf("farmId" to farmId, "farmName" to farmName)

    private fun FirebaseFirestoreException.toJoinMessage(): String = when (code) {
        FirebaseFirestoreException.Code.UNAVAILABLE ->
            "Necesitas conexión a internet para unirte a una finca"
        // Las reglas niegan si el usuario ya es el dueño de esa finca o fue dado de baja en ella,
        // o si el código cambió justo ahora.
        FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "No se pudo unir a la finca. Si ya eras miembro y te dieron de baja, habla con el " +
                "propietario; si no, pídele el código actual"
        else -> "No se pudo unir a la finca. Inténtalo de nuevo"
    }

    private fun DocumentSnapshot.toFarm() = Farm(
        id = id,
        name = getString("name") ?: "",
        ownerUid = getString("ownerUid") ?: "",
        inviteCode = getString("inviteCode"),
        createdAt = getLong("createdAt") ?: 0L,
    )
}
