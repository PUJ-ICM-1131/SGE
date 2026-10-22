package com.sge.finca.data.injection

import android.content.Context
import androidx.room.Room
import com.sge.finca.data.local.AppDatabase
import com.sge.finca.data.local.MIGRATION_1_2
import com.sge.finca.data.local.dao.AnimalDao
import com.sge.finca.data.local.dao.InspectionDao
import com.sge.finca.data.repository.RoomInspectionRepository
import com.sge.finca.domain.repository.InspectionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class InspectionModule {

    @Binds
    @Singleton
    abstract fun bindInspectionRepository(
        impl: RoomInspectionRepository,
    ): InspectionRepository

    companion object {
        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "sge_database")
                .addMigrations(MIGRATION_1_2)
                .build()

        @Provides
        fun provideInspectionDao(db: AppDatabase): InspectionDao = db.inspectionDao()

        @Provides
        fun provideAnimalDao(db: AppDatabase): AnimalDao = db.animalDao()
    }
}