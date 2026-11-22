package com.sge.finca.data.injection

import com.sge.finca.data.repository.CategoryRepositoryImpl
import com.sge.finca.data.repository.ContactRepositoryImpl
import com.sge.finca.data.repository.FarmRepositoryImpl
import com.sge.finca.data.repository.ReminderRepositoryImpl
import com.sge.finca.data.repository.TransactionRepositoryImpl
import com.sge.finca.data.repository.UserRepositoryImpl
import com.sge.finca.domain.repository.CategoryRepository
import com.sge.finca.domain.repository.ContactRepository
import com.sge.finca.domain.repository.FarmRepository
import com.sge.finca.domain.repository.ReminderRepository
import com.sge.finca.domain.repository.TransactionRepository
import com.sge.finca.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Repositorios del modelo de datos (issue #23). Animales e inspecciones tienen su propio módulo. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindContactRepository(impl: ContactRepositoryImpl): ContactRepository

    @Binds
    @Singleton
    abstract fun bindReminderRepository(impl: ReminderRepositoryImpl): ReminderRepository

    @Binds
    @Singleton
    abstract fun bindFarmRepository(impl: FarmRepositoryImpl): FarmRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository
}
