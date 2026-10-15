package com.gabrieltagama.menuplanner.core.data.initial

import android.content.Context
import androidx.core.content.edit
import com.gabrieltagama.menuplanner.core.data.database.dao.DishDao
import com.gabrieltagama.menuplanner.core.domain.repository.InitialDishesRepository
import com.gabrieltagama.menuplanner.core.domain.repository.MenuShareRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Loads assets/initial_dishes.json, a share document with recipes only, through the regular
 * import so it is validated and merged like any received file. A private flag makes it run once
 * per installation, and it is skipped when the recipe book already has dishes (users updating the
 * app). The dishes have fixed ids and an old updatedAt, so any later edit by the user wins.
 */
@Singleton
internal class AssetInitialDishesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dishDao: DishDao,
    private val shareRepository: MenuShareRepository
) : InitialDishesRepository {

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val mutex = Mutex()

    override suspend fun loadIfFirstLaunch() = mutex.withLock {
        if (isAlreadyLoaded()) return@withLock
        if (dishDao.getVersions().isEmpty()) shareRepository.importAndMerge(readInitialDishes())
        markAsLoaded()
    }

    private suspend fun isAlreadyLoaded(): Boolean =
        withContext(Dispatchers.IO) { preferences.getBoolean(KEY_LOADED, false) }

    private suspend fun readInitialDishes(): String =
        withContext(Dispatchers.IO) { context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() } }

    private suspend fun markAsLoaded() =
        withContext(Dispatchers.IO) { preferences.edit { putBoolean(KEY_LOADED, true) } }

    private companion object {
        const val ASSET_NAME = "initial_dishes.json"
        const val PREFERENCES_NAME = "menu_planner_initial_data"
        const val KEY_LOADED = "initial_dishes_loaded"
    }
}
