package com.gabrieltagama.menuplanner.core.data.shopping

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.gabrieltagama.menuplanner.core.data.di.SettingsDataStore
import com.gabrieltagama.menuplanner.core.domain.model.ShoppingFilter
import com.gabrieltagama.menuplanner.core.domain.model.measure.IngredientKey
import com.gabrieltagama.menuplanner.core.domain.port.ShoppingChecklistRepository
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeParseException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Products ticked as bought, kept as a string set in the app Preferences DataStore. Each entry is
 * "from|to|meals|key", so every ShoppingFilter has its own ticks. A file that cannot be read
 * behaves as an empty checklist. The global ticks of the previous version are not migrated: they
 * are dropped the first time expired ticks are cleared.
 */
@Singleton
internal class DataStoreShoppingChecklistRepository @Inject constructor(
    @param:SettingsDataStore private val dataStore: DataStore<Preferences>
) : ShoppingChecklistRepository {

    private val entries: Flow<Set<String>> =
        dataStore.data
            .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
            .map { it[CHECKED].orEmpty() }

    override fun checked(filter: ShoppingFilter): Flow<Set<IngredientKey>> {
        val prefix = filter.prefix()
        return entries.map { stored -> stored.filter { it.startsWith(prefix) }.map { IngredientKey.fromValue(it.removePrefix(prefix)) }.toSet() }.distinctUntilChanged()
    }

    override suspend fun setChecked(filter: ShoppingFilter, key: IngredientKey, isChecked: Boolean) {
        val entry = filter.prefix() + key.value
        dataStore.edit { stored ->
            val current = stored[CHECKED].orEmpty()
            stored[CHECKED] = if (isChecked) current + entry else current - entry
        }
    }

    override suspend fun clear(filter: ShoppingFilter) {
        val prefix = filter.prefix()
        dataStore.edit { stored -> stored[CHECKED] = stored[CHECKED].orEmpty().filterNot { it.startsWith(prefix) }.toSet() }
    }

    override suspend fun clearEndedBefore(date: LocalDate) {
        dataStore.edit { stored ->
            stored.remove(LEGACY_CHECKED)
            stored[CHECKED] = stored[CHECKED].orEmpty().filter { entry -> entry.endDate()?.isBefore(date) == false }.toSet()
        }
    }

    private fun ShoppingFilter.prefix(): String =
        listOf(from.toString(), to.toString(), meals.map { it.name }.sorted().joinToString(MEAL_SEPARATOR)).joinToString(SEPARATOR, postfix = SEPARATOR)

    private fun String.endDate(): LocalDate? =
        try {
            split(SEPARATOR, limit = PARTS).getOrNull(END_DATE_INDEX)?.let(LocalDate::parse)
        } catch (exception: DateTimeParseException) {
            null
        }

    private companion object {
        val CHECKED = stringSetPreferencesKey("shopping_checked_by_filter")
        val LEGACY_CHECKED = stringSetPreferencesKey("shopping_checked")
        const val SEPARATOR = "|"
        const val MEAL_SEPARATOR = ","
        const val PARTS = 4
        const val END_DATE_INDEX = 1
    }
}
