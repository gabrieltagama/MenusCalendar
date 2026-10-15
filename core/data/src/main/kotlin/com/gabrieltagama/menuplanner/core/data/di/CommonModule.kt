package com.gabrieltagama.menuplanner.core.data.di

import com.gabrieltagama.menuplanner.core.domain.common.ApplicationScope
import com.gabrieltagama.menuplanner.core.domain.common.IoDispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton
import kotlin.random.Random
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json

/**
 * App-wide infrastructure that is not tied to the database: the system clock, the random source
 * used by the calendar autofill and the editors, the I/O dispatcher, the process-wide coroutine
 * scope and the JSON configuration of the share format.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object CommonModule {

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()

    @Provides
    @Singleton
    fun provideRandom(): Random = Random.Default

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
}
