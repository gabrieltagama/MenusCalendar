package com.gabrieltagama.menuplanner.core.data.shopping

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.gabrieltagama.menuplanner.core.data.di.SettingsDataStore
import com.gabrieltagama.menuplanner.core.domain.repository.ShoppingChecklistRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Products ticked as bought, kept as a string set in the app Preferences DataStore. A file that
 * cannot be read behaves as an empty checklist.
 */
@Singleton
internal class DataStoreShoppingChecklistRepository @Inject constructor(
    @param:SettingsDataStore private val dataStore: DataStore<Preferences>
) : ShoppingChecklistRepository {

    override val checked: Flow<Set<String>> =
        dataStore.data
            .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
            .map { it[CHECKED].orEmpty() }
            .distinctUntilChanged()

    override suspend fun setChecked(key: String, isChecked: Boolean) {
        dataStore.edit { stored ->
            val current = stored[CHECKED].orEmpty()
            stored[CHECKED] = if (isChecked) current + key else current - key
        }
    }

    override suspend fun clear() {
        dataStore.edit { it.remove(CHECKED) }
    }

    private companion object {
        val CHECKED = stringSetPreferencesKey("shopping_checked")
    }
}
