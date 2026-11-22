package com.sge.finca.data.injection

import com.google.firebase.messaging.FirebaseMessaging
import com.sge.finca.domain.repository.ReminderScheduler
import com.sge.finca.platform.notifications.WorkManagerReminderScheduler
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationsModule {

    @Binds
    @Singleton
    abstract fun bindReminderScheduler(impl: WorkManagerReminderScheduler): ReminderScheduler

    companion object {
        @Provides
        @Singleton
        fun firebaseMessaging(): FirebaseMessaging = FirebaseMessaging.getInstance()
    }
}
