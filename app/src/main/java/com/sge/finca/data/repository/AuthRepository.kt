package com.sge.finca.data.repository

import com.google.firebase.auth.FirebaseUser
import com.sge.finca.data.datasource.AuthRemoteDataSource
import com.sge.finca.data.datasource.SessionStatus
import com.sge.finca.platform.notifications.PushTokenRegistrar
import jakarta.inject.Inject

class AuthRepository @Inject constructor(
    private val authRemoteDataSource: AuthRemoteDataSource,
    private val pushTokenRegistrar: PushTokenRegistrar,
) {

    val currentUser: FirebaseUser?
        get() = authRemoteDataSource.currentUser

    suspend fun signIn(email: String, password: String) {
        authRemoteDataSource.signIn(email, password)
    }

    suspend fun signUp(email: String, password: String) {
        authRemoteDataSource.signUp(email, password)
    }

    fun signOut() {
        // Invalida el token push de este celular: el siguiente usuario no recibe avisos ajenos.
        pushTokenRegistrar.onSignOut()
        authRemoteDataSource.signOut()
    }

    /** Verifica con Firebase que la sesión guardada en el dispositivo sigue siendo válida. */
    suspend fun validateSession(): SessionStatus = authRemoteDataSource.refreshSession()
}