package com.sge.finca.data.injection

import com.sge.finca.data.repository.RemoteAnimalLocationSource
import com.sge.finca.data.repository.RemoteCollarRepository
import com.sge.finca.data.repository.RoomAnimalRepository
import com.sge.finca.domain.repository.AnimalLocationSource
import com.sge.finca.domain.repository.AnimalRepository
import com.sge.finca.domain.repository.CollarRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AnimalModule {

    @Binds
    @Singleton
    abstract fun bindAnimalRepository(impl: RoomAnimalRepository): AnimalRepository

    // Posiciones en vivo desde el proveedor externo de collares GPS (collar-api/, REST).
    // Sin red o sin servidor: usar SimulatedAnimalLocationSource (simulación local) o
    // StationaryAnimalLocationSource (animales quietos donde se registraron).
    @Binds
    @Singleton
    abstract fun bindAnimalLocationSource(impl: RemoteAnimalLocationSource): AnimalLocationSource

    @Binds
    @Singleton
    abstract fun bindCollarRepository(impl: RemoteCollarRepository): CollarRepository
}
