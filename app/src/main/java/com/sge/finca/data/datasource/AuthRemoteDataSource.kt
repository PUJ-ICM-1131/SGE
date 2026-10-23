package com.sge.finca.data.datasource

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/** Resultado de verificar contra Firebase que la sesión guardada en el dispositivo sigue válida. */
enum class SessionStatus {
    /** Firebase confirmó que la sesión es válida. */
    VALID,

    /** La sesión ya no sirve (usuario deshabilitado o eliminado, token revocado o vencido). */
    EXPIRED,

    /** No se pudo verificar (por ejemplo, sin internet). La app es offline-first: se deja pasar. */
    UNVERIFIED,
}

class AuthRemoteDataSource @Inject constructor (
    private val auth: FirebaseAuth
){

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email,password).await()
    }

    suspend fun signUp(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email,password).await()
    }

    fun signOut() {
        auth.signOut()
    }

    /**
     * Fuerza la renovación del token de Firebase para saber si la sesión sigue válida.
     * Sólo los errores de usuario/credenciales inválidos cuentan como sesión expirada; cualquier
     * otro fallo (típicamente de red) se reporta como [SessionStatus.UNVERIFIED].
     */
    suspend fun refreshSession(): SessionStatus {
        val user = auth.currentUser ?: return SessionStatus.EXPIRED
        return try {
            user.getIdToken(true).await()
            SessionStatus.VALID
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseAuthInvalidUserException) {
            SessionStatus.EXPIRED
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            SessionStatus.EXPIRED
        } catch (e: Exception) {
            SessionStatus.UNVERIFIED
        }
    }

}
