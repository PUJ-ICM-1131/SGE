package com.sge.finca.data.injection

import com.sge.finca.data.repository.RoomAnimalRepository
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
}
