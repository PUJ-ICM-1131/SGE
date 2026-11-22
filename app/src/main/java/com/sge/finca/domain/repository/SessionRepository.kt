package com.sge.finca.domain.repository

import com.sge.finca.domain.model.Permission
import com.sge.finca.domain.model.Session
import kotlinx.coroutines.flow.StateFlow

interface SessionRepository {
    /**
     * Sesión actual. Se actualiza sola al iniciar/cerrar sesión y cuando cambia el rol en
     * Firestore (por ejemplo, si el propietario da de baja a un trabajador).
     */
    val session: StateFlow<Session>

    /**
     * Verifica que el usuario actual pertenezca a [farmId] y tenga [permission].
     * Los repositorios lo llaman antes de cualquier escritura.
     *
     * @throws SecurityException con un mensaje apto para mostrar en la UI.
     */
    suspend fun requirePermission(farmId: String, permission: Permission)
}
