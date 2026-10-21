package com.gabrieltagama.menuplanner.core.data.initial

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.gabrieltagama.menuplanner.core.data.database.dao.DishDao
import com.gabrieltagama.menuplanner.core.data.di.SettingsDataStore
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.port.InitialDishesRepository
import com.gabrieltagama.menuplanner.core.domain.port.MenuSnapshotCodec
import com.gabrieltagama.menuplanner.core.domain.port.MenuSnapshotRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Loads assets/initial_dishes.json, a share document with recipes only, through the regular
 * codec and merge so it is validated and merged like any received file. A flag in the settings DataStore
 * makes it run once per installation, and it is skipped when the recipe book already has dishes
 * (users updating the app). The dishes have fixed ids and an old updatedAt, so any later edit by
 * the user wins.
 */
@Singleton
internal class AssetInitialDishesRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dishDao: DishDao,
    private val codec: MenuSnapshotCodec,
    private val snapshots: MenuSnapshotRepository,
    @param:SettingsDataStore private val dataStore: DataStore<Preferences>
) : InitialDishesRepository {

    private val mutex = Mutex()

    override suspend fun loadIfFirstLaunch() {
        mutex.withLock {
            if (dataStore.data.first()[KEY_LOADED] == true) return
            if (dishDao.getVersions().isEmpty()) (codec.decode(readInitialDishes()) as? Outcome.Success)?.let { snapshots.merge(it.value) }
            dataStore.edit { it[KEY_LOADED] = true }
        }
    }

    private suspend fun readInitialDishes(): String =
        withContext(Dispatchers.IO) { context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() } }

    private companion object {
        const val ASSET_NAME = "initial_dishes.json"
        val KEY_LOADED = booleanPreferencesKey("initial_dishes_loaded")
    }
}
