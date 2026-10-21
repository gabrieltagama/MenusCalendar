package com.gabrieltagama.menuplanner.core.domain.common

import javax.inject.Qualifier

/**
 * Qualifiers of the injected CoroutineDispatchers, so no class hard-codes Dispatchers.IO or
 * Dispatchers.Default and tests can pass their own: IoDispatcher for blocking input and output
 * (files, network, assets) and DefaultDispatcher for CPU work such as filtering or building lists.
 * The app module provides both.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher
