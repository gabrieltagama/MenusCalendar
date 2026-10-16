package com.gabrieltagama.menuplanner.core.domain.usecase.dish

import com.gabrieltagama.menuplanner.core.domain.fake.FakeDishRepository
import com.gabrieltagama.menuplanner.core.domain.fake.FakeSettingsRepository
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes.dish
import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests that the recipe book is converted to standard units only while the stored conversion
 * version is older than the current one, and that the version is recorded afterwards.
 */
class MigrateRecipeUnitsUseCaseTest {
    private val legacyDish = dish(id = "old", ingredients = listOf(Ingredient("Aceite de oliva", 2.0, MeasureUnit.TABLESPOON)))

    @Test
    fun `outdated book is converted once and the version is recorded`() = runBlocking {
        val dishes = FakeDishRepository(listOf(legacyDish))
        val settings = FakeSettingsRepository()
        val migrate = MigrateRecipeUnitsUseCase(settings, StandardizeRecipeBookUseCase(dishes))

        migrate()
        migrate()

        assertEquals(1, dishes.upserted.size)
        assertEquals(MigrateRecipeUnitsUseCase.CURRENT_VERSION, settings.unitsVersion)
    }

    @Test
    fun `up to date book is not read again`() = runBlocking {
        val dishes = FakeDishRepository(listOf(legacyDish))
        val settings = FakeSettingsRepository(unitsVersion = MigrateRecipeUnitsUseCase.CURRENT_VERSION)

        MigrateRecipeUnitsUseCase(settings, StandardizeRecipeBookUseCase(dishes))()

        assertTrue(dishes.upserted.isEmpty())
    }
}
