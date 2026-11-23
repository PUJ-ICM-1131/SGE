package com.sge.finca.data.remote

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.sge.finca.domain.model.FarmMember
import com.sge.finca.domain.model.FarmRole
import com.sge.finca.domain.model.Permission
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.retryWhen

/*
 * Documento farms/{farmId}/members/{uid}. Campos:
 *   role         "OWNER" | "WORKER"
 *   isActive     Boolean (si falta se toma como true: miembros creados antes del #28)
 *   joinedAt     Long (ms)
 *   displayName  String
 *   email        String
 *   grants       List<String> permisos que otorgó el propietario (#67). Solo lo escribe el
 *                propietario (WorkersRepository.setGrant); si falta, no hay ninguno.
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
        grants = toGrants(),
    )
}

/** Permisos otorgados de la membresía; ignora los desconocidos y los que no se pueden otorgar. */
fun DocumentSnapshot.toGrants(): Set<Permission> =
    (get("grants") as? List<*>).orEmpty()
        .mapNotNull { raw -> Permission.entries.firstOrNull { it.grantable && it.name == raw } }
        .toSet()

/**
 * Escucha la membresía de [uid] en [farmId]. Responde desde la caché offline de Firestore si no
 * hay red.
 *
 * Si Firestore niega la lectura, emite null (sin membresía no hay permisos) y **vuelve a
 * intentar**: Firestore cierra para siempre un listener rechazado. Pasa justo al crear la finca
 * o unirse con un código: la app ve la membresía en su caché local antes de que el servidor
 * confirme la escritura, empieza a escuchar, y el servidor todavía no la tiene. Sin reintento
 * la sesión quedaba sin rol hasta cerrar la app ("No tienes acceso a esta finca").
 */
fun FirebaseFirestore.observeMember(farmId: String, uid: String): Flow<FarmMember?> =
    callbackFlow {
        val registration = document(FirestorePaths.member(farmId, uid))
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toFarmMember(farmId))
            }
        awaitClose { registration.remove() }
    }.retryWhen { cause, attempt ->
        val retryable = cause is FirebaseFirestoreException && cause.code in RETRYABLE
        if (retryable) {
            Log.w("SgeSession", "No se pudo leer la membresía (reintento ${attempt + 1})", cause)
            emit(null)
            // 1 s, 2 s, 4 s… hasta 30 s: rápido justo después de registrarse, sin gastar lecturas.
            delay((1_000L shl attempt.coerceAtMost(5).toInt()).coerceAtMost(30_000L))
        }
        retryable
    }

private val RETRYABLE = setOf(
    FirebaseFirestoreException.Code.PERMISSION_DENIED,
    FirebaseFirestoreException.Code.UNAVAILABLE,
)
