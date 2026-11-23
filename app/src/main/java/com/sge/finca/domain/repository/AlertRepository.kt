package com.sge.finca.domain.repository

import com.sge.finca.domain.model.AnimalAlert
import kotlinx.coroutines.flow.Flow

/** Alertas de vacas fuera de los potreros (#73). */
interface AlertRepository {
    /** null si la alerta no existe en la finca. */
    fun observeAlert(farmId: String, alertId: String): Flow<AnimalAlert?>

    /**
     * Pide al servidor avisar a los trabajadores activos. Solo el propietario
     * (FORWARD_ALERTS); lanza [SecurityException] si no tiene permiso.
     */
    suspend fun forward(farmId: String, alertId: String)
}
