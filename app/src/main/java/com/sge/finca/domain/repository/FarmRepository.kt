package com.sge.finca.domain.repository

import com.sge.finca.domain.model.Farm
import kotlinx.coroutines.flow.Flow

interface FarmRepository {
    fun observeFarm(farmId: String): Flow<Farm?>

    /**
     * Crea la finca con el usuario actual como propietario (OWNER): escribe la finca, la
     * membresía, el farmId en el perfil y el índice del código de invitación, e inserta las
     * categorías por defecto. Requiere sesión iniciada y conexión (se llama justo después del registro).
     */
    suspend fun createFarm(farmName: String, ownerName: String): Farm

    /**
     * Cambia el código de invitación (solo el propietario). El código anterior deja de servir
     * en ese mismo momento. Requiere conexión.
     */
    suspend fun regenerateInviteCode(farmId: String): String

    /**
     * Une al usuario actual a la finca de [inviteCode] como trabajador (WORKER) activo y la deja
     * como su finca en el perfil (#29). Acepta el formato actual y el anterior del código.
     *
     * @throws IllegalArgumentException si el código no tiene el formato o no corresponde a ninguna
     *   finca (mensaje apto para la UI).
     * @throws IllegalStateException si no se puede unir: sin conexión, ya es el propietario o fue
     *   dado de baja en esa finca (mensaje apto para la UI).
     */
    suspend fun joinFarm(inviteCode: String, displayName: String): JoinedFarm

    /**
     * Fincas creadas antes del #29 no tienen su código en el índice; el propietario lo agrega al
     * abrir Trabajadores para que el código actual funcione. No hace nada si ya existe.
     */
    suspend fun ensureInviteCodeIndex(farmId: String)
}

/** Finca a la que se unió el usuario. */
data class JoinedFarm(val farmId: String, val farmName: String)
