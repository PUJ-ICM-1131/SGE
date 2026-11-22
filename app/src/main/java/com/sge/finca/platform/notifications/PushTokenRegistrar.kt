package com.sge.finca.platform.notifications

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.sge.finca.data.injection.ApplicationScope
import com.sge.finca.data.remote.FirestorePaths
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mantiene el token FCM de este celular en users/{uid}.fcmTokens, que es donde sge-notifier
 * busca a quién avisar. Un usuario puede tener varios celulares, por eso es una lista.
 *
 * - Al iniciar sesión (o al arrancar la app con sesión) guarda el token.
 * - Si FCM rota el token, guarda el nuevo ([onNewToken]).
 * - Al cerrar sesión invalida el token del celular: así el siguiente usuario no recibe los
 *   avisos de la finca anterior. El token viejo queda muerto y el servidor lo limpia solo.
 */
@Singleton
class PushTokenRegistrar @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val messaging: FirebaseMessaging,
    @ApplicationScope private val scope: CoroutineScope,
) {
    /** Registrar y borrar no deben cruzarse (p. ej. cerrar sesión e iniciar otra enseguida). */
    private val mutex = Mutex()

    /** Se llama una vez al iniciar la app. */
    fun start() {
        scope.launch {
            callbackFlow {
                val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
                auth.addAuthStateListener(listener)
                awaitClose { auth.removeAuthStateListener(listener) }
            }
                .distinctUntilChanged()
                .collect { uid -> if (uid != null) register(uid) }
        }
    }

    /** FCM generó un token nuevo (reinstalación, restauración, rotación). */
    fun onNewToken(token: String) {
        val uid = auth.currentUser?.uid ?: return
        scope.launch { mutex.withLock { save(uid, token) } }
    }

    /** Llamar antes de cerrar sesión. No bloquea: si no hay red, se completa cuando vuelva. */
    fun onSignOut() {
        scope.launch {
            mutex.withLock {
                runLogged("invalidar el token") { messaging.deleteToken().await() }
            }
        }
    }

    private suspend fun register(uid: String) = mutex.withLock {
        runLogged("registrar el token") { save(uid, messaging.token.await()) }
    }

    private suspend fun save(uid: String, token: String) {
        firestore.document(FirestorePaths.user(uid))
            .set(mapOf("fcmTokens" to FieldValue.arrayUnion(token)), SetOptions.merge())
            .await()
    }

    private suspend fun runLogged(action: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Sin Google Play Services o sin red: la app sigue funcionando, solo sin avisos push.
            Log.w(TAG, "No se pudo $action", e)
        }
    }

    private companion object {
        const val TAG = "SgePush"
    }
}
