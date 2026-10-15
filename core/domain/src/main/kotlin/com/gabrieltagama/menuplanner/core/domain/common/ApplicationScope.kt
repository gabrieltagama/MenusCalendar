package com.gabrieltagama.menuplanner.core.domain.common

import javax.inject.Qualifier

/**
 * Qualifier of the CoroutineScope that lives as long as the app process, for work that must not
 * be cancelled with a screen, such as preparing the recipe book at start-up.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
