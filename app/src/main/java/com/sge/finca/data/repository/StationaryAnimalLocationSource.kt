package com.sge.finca.data.repository

import com.sge.finca.domain.model.AnimalLocation
import com.sge.finca.domain.repository.AnimalLocationSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación actual: no hay rastreo, todos los animales siguen quietos
 * en el sitio donde se registraron.
 * TODO: reemplazar por una implementación que consuma la API de ubicación (Retrofit).
 */
@Singleton
class StationaryAnimalLocationSource @Inject constructor() : AnimalLocationSource {

    override fun observeLocations(): Flow<Map<String, AnimalLocation>> = flowOf(emptyMap())
}
