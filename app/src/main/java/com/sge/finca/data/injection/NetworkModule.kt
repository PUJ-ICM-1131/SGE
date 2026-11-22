package com.sge.finca.data.injection

import com.sge.finca.BuildConfig
import com.sge.finca.data.remote.collar.CollarApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Clientes REST de servicios externos.
 * Un solo [OkHttpClient] base (comparte conexiones y caché); cada API agrega lo suyo con newBuilder().
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private val json = Json {
        ignoreUnknownKeys = true // el proveedor puede agregar campos sin romper la app
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .callTimeout(15, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideCollarApi(baseClient: OkHttpClient): CollarApi {
        val client = baseClient.newBuilder()
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("X-Api-Key", BuildConfig.COLLAR_API_KEY)
                        .build()
                )
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.COLLAR_API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(CollarApi::class.java)
    }
}
