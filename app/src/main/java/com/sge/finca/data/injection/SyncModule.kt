package com.sge.finca.data.injection

import com.sge.finca.data.sync.SyncRepositoryImpl
import com.sge.finca.data.sync.SyncScheduler
import com.sge.finca.data.sync.WorkManagerSyncScheduler
import com.sge.finca.domain.repository.SyncRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Sincronización offline Room <-> Firestore (issue #32). */
@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {

    @Binds
    @Singleton
    abstract fun bindSyncScheduler(impl: WorkManagerSyncScheduler): SyncScheduler

    @Binds
    @Singleton
    abstract fun bindSyncRepository(impl: SyncRepositoryImpl): SyncRepository
}
