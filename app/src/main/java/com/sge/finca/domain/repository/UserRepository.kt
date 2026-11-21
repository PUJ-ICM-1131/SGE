package com.sge.finca.domain.repository

import com.sge.finca.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    /** uid de la sesión actual, o null si no hay sesión. */
    val currentUserId: String?

    /** Perfil del usuario con sesión (users/{uid}); emite null sin sesión o si aún no hay perfil. */
    fun observeCurrentUser(): Flow<UserProfile?>

    /**
     * Finca del usuario actual. Es lo que las pantallas usan para saber qué farmId pasar a los
     * demás repositorios. Emite null si no hay sesión o el usuario aún no tiene finca.
     */
    fun observeCurrentFarmId(): Flow<String?>
}
