package com.sge.finca.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.sge.finca.data.injection.ApplicationScope
import com.sge.finca.data.remote.observeMember
import com.sge.finca.domain.model.Permission
import com.sge.finca.domain.model.Session
import com.sge.finca.domain.repository.SessionRepository
import com.sge.finca.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Sesión = usuario de Firebase Auth + su perfil (users/{uid}) + su membresía
 * (farms/{farmId}/members/{uid}). Todo con listeners, así que el rol se actualiza en vivo.
 */
@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val userRepository: UserRepository,
    private val firestore: FirebaseFirestore,
    @ApplicationScope scope: CoroutineScope,
) : SessionRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override val session: StateFlow<Session> = userRepository.observeCurrentUser()
        .flatMapLatest { profile ->
            val uid = profile?.uid ?: userRepository.currentUserId
            val farmId = profile?.farmId
            when {
                uid == null -> flowOf(Session.SignedOut)
                farmId == null -> flowOf(Session(uid = uid, loading = false))
                else -> firestore.observeMember(farmId, uid)
                    .map { member ->
                        Session(uid = uid, farmId = farmId, member = member, loading = false)
                    }
                    .onStart { emit(Session(uid = uid, farmId = farmId, loading = true)) }
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, Session.Loading)

    override suspend fun requirePermission(farmId: String, permission: Permission) {
        val current = withTimeoutOrNull(PERMISSION_TIMEOUT_MS) { session.first { !it.loading } }
            ?: throw SecurityException(
                "No se pudieron verificar tus permisos. Revisa tu conexión e inténtalo de nuevo"
            )
        when {
            current.uid == null ->
                throw SecurityException("Inicia sesión para continuar")
            current.farmId != farmId || current.role == null ->
                throw SecurityException("No tienes acceso a esta finca")
            !current.can(permission) ->
                throw SecurityException("Solo el propietario de la finca puede hacer esto")
        }
    }

    private companion object {
        /** Sin red y sin caché, el listener puede tardar: no bloquear la UI indefinidamente. */
        const val PERMISSION_TIMEOUT_MS = 5_000L
    }
}
