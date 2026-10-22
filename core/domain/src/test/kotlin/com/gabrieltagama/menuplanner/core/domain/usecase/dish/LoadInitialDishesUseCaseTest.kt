package com.gabrieltagama.menuplanner.core.domain.usecase.dish

import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.common.ShareError
import com.gabrieltagama.menuplanner.core.domain.fake.FakeDishRepository
import com.gabrieltagama.menuplanner.core.domain.fake.FakeMenuSnapshotCodec
import com.gabrieltagama.menuplanner.core.domain.fake.FakeSettingsRepository
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes.dish
import com.gabrieltagama.menuplanner.core.domain.fake.TestImports
import com.gabrieltagama.menuplanner.core.domain.model.ImportSummary
import com.gabrieltagama.menuplanner.core.domain.model.share.MenuSnapshot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * LoadInitialDishesUseCase imports the bundled recipe book once per installation and only into an
 * empty recipe book, and reports a broken bundled file instead of hiding it.
 */
class LoadInitialDishesUseCaseTest {
    private val settings = FakeSettingsRepository()
    private val codec = FakeMenuSnapshotCodec().apply { register(BUNDLED, MenuSnapshot(listOf(dish(id = "family", name = "Lentejas")))) }

    private fun useCase(dishes: FakeDishRepository, codec: FakeMenuSnapshotCodec = this.codec) =
        LoadInitialDishesUseCase(settings, dishes, { BUNDLED }, TestImports.importMenus(dishes, codec))

    @Test
    fun `an empty recipe book receives the bundled dishes once`() = runBlocking {
        val dishes = FakeDishRepository()

        assertEquals(Outcome.Success(ImportSummary(1, 0, 0, 0, 0, 0)), useCase(dishes)())
        assertEquals(setOf("family"), dishes.current.keys)
        assertTrue(settings.initialDishesLoaded)

        dishes.deleteIfUnused(dishes.getDishes().single().id)
        assertEquals(Outcome.Success(ImportSummary.EMPTY), useCase(dishes)())
        assertTrue(dishes.current.isEmpty())
    }

    @Test
    fun `an existing recipe book is kept`() = runBlocking {
        val dishes = FakeDishRepository(listOf(dish(id = "own", name = "Receta propia")))

        assertEquals(Outcome.Success(ImportSummary.EMPTY), useCase(dishes)())
        assertEquals(setOf("own"), dishes.current.keys)
        assertTrue(settings.initialDishesLoaded)
    }

    @Test
    fun `a broken bundled file is reported`() = runBlocking {
        val failure = Outcome.Failure(ShareError.InvalidImportFile)

        assertEquals(failure, useCase(FakeDishRepository(), FakeMenuSnapshotCodec(failure))())
    }

    private companion object {
        const val BUNDLED = "bundled"
    }
}
