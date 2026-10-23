package com.sge.finca.data.injection

import com.sge.finca.data.repository.RoomAnimalRepository
import com.sge.finca.data.repository.SimulatedAnimalLocationSource
import com.sge.finca.domain.repository.AnimalLocationSource
import com.sge.finca.domain.repository.AnimalRepository
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

    // PRUEBAS: los animales caminan solos. Para dejarlos quietos usar StationaryAnimalLocationSource.
    // Cuando exista la API real, reemplazar por su implementación.
    @Binds
    @Singleton
    abstract fun bindAnimalLocationSource(impl: SimulatedAnimalLocationSource): AnimalLocationSource
}
