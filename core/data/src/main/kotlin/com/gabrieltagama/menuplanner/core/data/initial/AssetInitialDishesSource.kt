package com.gabrieltagama.menuplanner.core.data.initial

import android.content.Context
import com.gabrieltagama.menuplanner.core.domain.common.IoDispatcher
import com.gabrieltagama.menuplanner.core.domain.port.InitialDishesSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Reads assets/initial_dishes.json, the starter recipe book bundled with the app: a shared file of
 * recipes only, read off the main thread.
 */
internal class AssetInitialDishesSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : InitialDishesSource {

    override suspend fun read(): String =
        withContext(ioDispatcher) { context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() } }

    private companion object {
        const val ASSET_NAME = "initial_dishes.json"
    }
}
