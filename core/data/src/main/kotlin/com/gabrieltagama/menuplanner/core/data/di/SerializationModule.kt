package com.gabrieltagama.menuplanner.core.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json

/**
 * Provides the JSON configuration of the shared files and backups: readable output, unknown keys
 * of newer files ignored and default values always written.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object SerializationModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
}
