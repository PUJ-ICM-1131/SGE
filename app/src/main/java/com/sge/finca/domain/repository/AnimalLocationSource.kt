package com.sge.finca.domain.repository

import com.sge.finca.domain.model.AnimalLocation
import kotlinx.coroutines.flow.Flow

/**
 * Fuente de posiciones en vivo, indexadas por id de animal.
 * Para conectar la API de rastreo: crear una implementación nueva y cambiar el @Binds
 * en AnimalModule. Nada más en la app cambia.
 */
interface AnimalLocationSource {
    fun observeLocations(): Flow<Map<String, AnimalLocation>>
}
