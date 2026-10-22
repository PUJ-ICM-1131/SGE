package com.sge.finca.data.injection

import android.content.Context
import com.sge.finca.platform.sensors.ShakeDetector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SensorModule {

    @Provides
    @Singleton
    fun provideShakeDetector(
        @ApplicationContext context: Context,
    ): ShakeDetector {
        return ShakeDetector(
            context = context,
            threshold = 12f,
            cooldownMillis = 2_000L,
        )
    }
}