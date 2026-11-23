package com.sge.finca.domain.repository

import com.sge.finca.domain.model.Coordinate
import com.sge.finca.domain.model.Paddock
import kotlinx.coroutines.flow.Flow

/**
 * Potreros de la finca (#71). Las escrituras verifican el rol (issue #28) y lanzan
 * [SecurityException] si no hay permiso: crear requiere START_INSPECTION; renombrar y eliminar,
 * CONFIGURE_FARM (solo el propietario).
 */
interface PaddockRepository {
    fun observePaddocks(farmId: String): Flow<List<Paddock>>

    /**
     * Guarda el área encerrada como potrero y calcula su área. Devuelve el id generado.
     * @throws IllegalArgumentException si falta el nombre o hay menos de 3 puntos.
     */
    suspend fun create(farmId: String, name: String, points: List<Coordinate>): String

    suspend fun rename(farmId: String, id: String, name: String)

    suspend fun delete(farmId: String, id: String)
}
