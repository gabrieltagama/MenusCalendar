package com.gabrieltagama.menuplanner.core.domain.usecase.dish

import com.gabrieltagama.menuplanner.core.domain.port.DishRepository
import com.gabrieltagama.menuplanner.core.domain.port.InitialDishesSource
import com.gabrieltagama.menuplanner.core.domain.usecase.share.ImportMenusUseCase
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Loads the bundled starter recipe book on the first launch after installing the app, through the
 * regular import so it is validated and merged like any received file. It runs once per
 * installation and only while the recipe book is empty, so users updating the app never receive
 * it; the mutex keeps two concurrent calls from loading it twice.
 */
@Singleton
class LoadInitialDishesUseCase @Inject constructor(
    private val source: InitialDishesSource,
    private val dishes: DishRepository,
    private val importMenus: ImportMenusUseCase
) {
    private val mutex = Mutex()

    suspend operator fun invoke() = mutex.withLock {
        if (source.isLoaded()) return@withLock
        if (dishes.getDishes().isEmpty()) importMenus(source.read())
        source.markLoaded()
    }
}
