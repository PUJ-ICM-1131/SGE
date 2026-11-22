package com.sge.finca.data.remote

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.sge.finca.domain.model.FarmMember
import com.sge.finca.domain.model.FarmRole
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/*
 * Documento farms/{farmId}/members/{uid}. Campos:
 *   role         "OWNER" | "WORKER"
 *   isActive     Boolean (si falta se toma como true: miembros creados antes del #28)
 *   joinedAt     Long (ms)
 *   displayName  String
 *   email        String
 */

fun FarmMember.toFirestore(): Map<String, Any?> = mapOf(
    "role" to role.name,
    "isActive" to isActive,
    "joinedAt" to joinedAt,
    "displayName" to displayName,
    "email" to email,
)

/** Devuelve null si el documento no existe o tiene un rol desconocido. */
fun DocumentSnapshot.toFarmMember(farmId: String): FarmMember? {
    if (!exists()) return null
    val role = getString("role")
        ?.let { raw -> FarmRole.entries.firstOrNull { it.name == raw } }
        ?: return null
    return FarmMember(
        uid = id,
        farmId = farmId,
        role = role,
        isActive = getBoolean("isActive") ?: true,
        joinedAt = getLong("joinedAt") ?: 0L,
        // "name" era el campo que usaba el #23; se lee como respaldo.
        displayName = getString("displayName") ?: getString("name") ?: "",
        email = getString("email") ?: "",
    )
}

/**
 * Escucha la membresía de [uid] en [farmId]. Responde desde la caché offline de Firestore si no
 * hay red. Ante un error (por ejemplo, las reglas niegan la lectura) emite null: sin membresía
 * no hay permisos.
 */
fun FirebaseFirestore.observeMember(farmId: String, uid: String): Flow<FarmMember?> =
    callbackFlow {
        val registration = document(FirestorePaths.member(farmId, uid))
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("SgeSession", "No se pudo leer la membresía", error)
                    trySend(null)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toFarmMember(farmId))
            }
        awaitClose { registration.remove() }
    }
