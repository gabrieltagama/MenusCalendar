package com.gabrieltagama.menuplanner.core.domain.usecase.share

import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.common.ShareError
import com.gabrieltagama.menuplanner.core.domain.fake.FakeDishRepository
import com.gabrieltagama.menuplanner.core.domain.fake.FakeMenuSnapshotCodec
import com.gabrieltagama.menuplanner.core.domain.fake.FakeMenuSnapshotRepository
import com.gabrieltagama.menuplanner.core.domain.fake.ImmediateTransactionRunner
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes.dish
import com.gabrieltagama.menuplanner.core.domain.model.ImportSummary
import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import com.gabrieltagama.menuplanner.core.domain.model.share.MenuSnapshot
import com.gabrieltagama.menuplanner.core.domain.model.share.SnapshotScope
import com.gabrieltagama.menuplanner.core.domain.usecase.dish.StandardizeRecipeBookUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests that ExportMenusUseCase encodes a snapshot of everything and that ImportMenusUseCase
 * decodes, merges and leaves the imported recipes in standard units, propagating decode failures.
 */
class ShareUseCasesTest {
    private val dishes = FakeDishRepository(listOf(dish(id = "a", ingredients = listOf(Ingredient("Leche", 0.5, MeasureUnit.LITER)))))
    private val codec = FakeMenuSnapshotCodec()

    @Test
    fun `export encodes a snapshot of everything`() = runBlocking {
        val repository = FakeMenuSnapshotRepository(dishes)

        val text = ExportMenusUseCase(repository, codec, TestDishes.fixedClock)()

        assertEquals(listOf(SnapshotScope.EVERYTHING), repository.readScopes)
        assertEquals("dishes=1;lunches=0;dinners=0", text)
    }

    @Test
    fun `import merges the decoded snapshot and returns the summary`() = runBlocking {
        val summary = ImportSummary(1, 2, 3, 4, 5, 6)
        val repository = FakeMenuSnapshotRepository(dishes, mergeResult = summary)
        val snapshot = MenuSnapshot(listOf(dish(id = "b")))
        codec.register("payload", snapshot)

        assertEquals(Outcome.Success(summary), importMenus(repository, codec)("payload"))
        assertEquals(listOf(snapshot), repository.merged)
    }

    @Test
    fun `import propagates decode failures without merging`() = runBlocking {
        val failure = Outcome.Failure(ShareError.UnsupportedImportVersion)
        val repository = FakeMenuSnapshotRepository(dishes)

        assertEquals(failure, importMenus(repository, FakeMenuSnapshotCodec(failure))("bad"))
        assertTrue(repository.merged.isEmpty())
    }

    @Test
    fun `import converts the recipe book to standard units`() = runBlocking {
        val repository = FakeMenuSnapshotRepository(dishes)
        codec.register("payload", MenuSnapshot(emptyList()))

        importMenus(repository, codec)("payload")

        assertEquals(listOf(Ingredient("Leche", 500.0, MeasureUnit.MILLILITER)), dishes.current.getValue("a").ingredients)
    }

    private fun importMenus(repository: FakeMenuSnapshotRepository, codec: FakeMenuSnapshotCodec) =
        ImportMenusUseCase(repository, codec, StandardizeRecipeBookUseCase(dishes), ImmediateTransactionRunner())
}
