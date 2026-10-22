package com.sge.finca.domain.repository

import com.sge.finca.domain.model.Animal
import kotlinx.coroutines.flow.Flow

interface AnimalRepository {
    fun getAnimals(): Flow<List<Animal>>
    suspend fun saveAnimal(animal: Animal)
}
