package com.sge.finca.domain.repository

import com.sge.finca.domain.model.Farm
import kotlinx.coroutines.flow.Flow

interface FarmRepository {
    fun observeFarm(farmId: String): Flow<Farm?>

    /**
     * Crea la finca con el usuario actual como propietario (OWNER): escribe la finca, la
     * membresía y el farmId en el perfil, e inserta las categorías por defecto.
     * Requiere sesión iniciada y conexión (se llama justo después del registro).
     */
    suspend fun createFarm(farmName: String, ownerName: String): Farm

    suspend fun regenerateInviteCode(farmId: String): String
}