package com.gabrieltagama.menuplanner.core.data.initial

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gabrieltagama.menuplanner.core.data.database.DatabaseTestData
import com.gabrieltagama.menuplanner.core.data.database.MenuPlannerDatabase
import com.gabrieltagama.menuplanner.core.data.database.RoomTransactionRunner
import com.gabrieltagama.menuplanner.core.data.fake.TestData
import com.gabrieltagama.menuplanner.core.data.repository.RoomDinnerPlanRepository
import com.gabrieltagama.menuplanner.core.data.repository.RoomDishRepository
import com.gabrieltagama.menuplanner.core.data.repository.RoomLunchPlanRepository
import com.gabrieltagama.menuplanner.core.data.settings.DataStoreSettingsRepository
import com.gabrieltagama.menuplanner.core.data.share.JsonMenuSnapshotCodec
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.usecase.dish.LoadInitialDishesUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.dish.StandardizeRecipeBookUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.share.ImportMenusUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.share.MenuSnapshotMerger
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

/**
 * Robolectric tests of LoadInitialDishesUseCase wired as in the app, with the real asset
 * (AssetInitialDishesSource), JSON codec, Room repositories and settings DataStore: the family recipe
 * book is loaded on the first launch with valid, unique dishes, it is loaded only once even if the
 * user later empties the book, and it is skipped when the book already has dishes.
 */
@RunWith(AndroidJUnit4::class)
class InitialDishesLoadingTest {

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var database: MenuPlannerDatabase
    private lateinit var dishRepository: RoomDishRepository
    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>

    @Before
    fun setUp() {
        database = DatabaseTestData.inMemoryDatabase()
        dishRepository = RoomDishRepository(database.dishDao())
        dataStoreScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        dataStore = PreferenceDataStoreFactory.create(scope = dataStoreScope) { File(folder.root, "settings.preferences_pb") }
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
        database.close()
    }

    @Test
    fun firstLaunchLoadsTheFamilyRecipeBook() = runTest {
        val result = loadInitialDishes()

        val dishes = dishRepository.getDishes()
        assertTrue(dishes.isNotEmpty())
        assertEquals(dishes.size, (result as Outcome.Success).value.dishesAdded)
        assertEquals(dishes.size, dishes.map { it.id }.toSet().size)
        assertTrue(dishes.all { it.validationError == null })
    }

    @Test
    fun theRecipeBookIsLoadedOnlyOnce() = runTest {
        loadInitialDishes()
        dishRepository.getDishes().forEach { dishRepository.deleteIfUnused(it.id) }

        loadInitialDishes()

        assertTrue(dishRepository.getDishes().isEmpty())
    }

    @Test
    fun anExistingRecipeBookIsKept() = runTest {
        val own = DatabaseTestData.dish("own", name = "Receta propia")
        dishRepository.upsert(own)

        loadInitialDishes()

        assertEquals(listOf(own), dishRepository.getDishes())
    }

    private suspend fun loadInitialDishes() = LoadInitialDishesUseCase(
        appState = DataStoreSettingsRepository(dataStore),
        dishes = dishRepository,
        source = AssetInitialDishesSource(ApplicationProvider.getApplicationContext<Context>(), Dispatchers.IO),
        importMenus = ImportMenusUseCase(
            codec = JsonMenuSnapshotCodec(TestData.json),
            merger = MenuSnapshotMerger(
                dishRepository,
                RoomLunchPlanRepository(database.lunchDayDao(), database.dishDao()),
                RoomDinnerPlanRepository(database.dinnerDayDao(), database.dishDao())
            ),
            standardizeRecipeBook = StandardizeRecipeBookUseCase(dishRepository),
            transaction = RoomTransactionRunner(database)
        )
    )()
}
