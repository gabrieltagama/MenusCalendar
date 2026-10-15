package com.gabrieltagama.menuplanner.core.data.initial

import android.content.Context
import androidx.core.content.edit
import com.gabrieltagama.menuplanner.core.domain.common.IoDispatcher
import com.gabrieltagama.menuplanner.core.domain.repository.InitialDishesSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Starter recipe book stored in assets/initial_dishes.json, a share document with recipes only,
 * plus a private flag that records it was already offered. The dishes have fixed ids and an old
 * updatedAt, so any later edit by the user wins.
 */
internal class AssetInitialDishesSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : InitialDishesSource {

    private val preferences by lazy { context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE) }

    override suspend fun isLoaded(): Boolean = withContext(ioDispatcher) { preferences.getBoolean(KEY_LOADED, false) }

    override suspend fun read(): String =
        withContext(ioDispatcher) { context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() } }

    override suspend fun markLoaded() = withContext(ioDispatcher) { preferences.edit { putBoolean(KEY_LOADED, true) } }

    private companion object {
        const val ASSET_NAME = "initial_dishes.json"
        const val PREFERENCES_NAME = "menu_planner_initial_data"
        const val KEY_LOADED = "initial_dishes_loaded"
    }
}
