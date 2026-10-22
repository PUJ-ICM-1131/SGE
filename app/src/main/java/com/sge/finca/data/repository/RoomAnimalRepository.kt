package com.sge.finca.data.repository

import com.sge.finca.data.local.dao.AnimalDao
import com.sge.finca.data.local.entity.AnimalEntity
import com.sge.finca.domain.model.Animal
import com.sge.finca.domain.repository.AnimalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomAnimalRepository @Inject constructor(
    private val dao: AnimalDao,
) : AnimalRepository {

    override fun getAnimals(): Flow<List<Animal>> =
        dao.observeAnimals().map { entities -> entities.map { it.toDomain() } }

    override suspend fun saveAnimal(animal: Animal) {
        dao.insertAnimal(AnimalEntity.fromDomain(animal))
    }
}
