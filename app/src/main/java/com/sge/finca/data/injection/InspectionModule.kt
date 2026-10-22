package com.sge.finca.data.injection

import com.sge.finca.data.repository.InMemoryInspectionRepository
import com.sge.finca.domain.repository.InspectionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class InspectionModule {

    @Binds
    @Singleton
    abstract fun bindInspectionRepository(
        impl: InMemoryInspectionRepository,
    ): InspectionRepository
}
