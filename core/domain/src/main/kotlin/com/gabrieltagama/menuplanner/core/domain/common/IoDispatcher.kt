package com.gabrieltagama.menuplanner.core.domain.common

import javax.inject.Qualifier

/**
 * Qualifier of the CoroutineDispatcher used for blocking I/O (files, preferences, network). It is
 * injected instead of referencing Dispatchers.IO so tests can replace it.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
