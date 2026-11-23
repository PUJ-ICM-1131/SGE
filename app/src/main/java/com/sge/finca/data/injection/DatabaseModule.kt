package com.sge.finca.data.injection

import android.content.Context
import androidx.room.Room
import com.sge.finca.data.local.ALL_MIGRATIONS
import com.sge.finca.data.local.AppDatabase
import com.sge.finca.data.local.dao.AnimalDao
import com.sge.finca.data.local.dao.CategoryDao
import com.sge.finca.data.local.dao.FarmContactDao
import com.sge.finca.data.local.dao.InspectionDao
import com.sge.finca.data.local.dao.PaddockDao
import com.sge.finca.data.local.dao.PendingDeleteDao
import com.sge.finca.data.local.dao.ReminderDao
import com.sge.finca.data.local.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Base de datos Room y todos sus DAOs (antes vivían en InspectionModule). */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "sge_database")
            .addMigrations(*ALL_MIGRATIONS)
            .build()

    @Provides
    fun provideInspectionDao(db: AppDatabase): InspectionDao = db.inspectionDao()

    @Provides
    fun provideAnimalDao(db: AppDatabase): AnimalDao = db.animalDao()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideFarmContactDao(db: AppDatabase): FarmContactDao = db.farmContactDao()

    @Provides
    fun provideReminderDao(db: AppDatabase): ReminderDao = db.reminderDao()

    @Provides
    fun providePendingDeleteDao(db: AppDatabase): PendingDeleteDao = db.pendingDeleteDao()

    @Provides
    fun providePaddockDao(db: AppDatabase): PaddockDao = db.paddockDao()
}
