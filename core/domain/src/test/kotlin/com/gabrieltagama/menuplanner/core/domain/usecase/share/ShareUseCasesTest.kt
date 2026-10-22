package com.gabrieltagama.menuplanner.core.domain.usecase.share

import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.common.ShareError
import com.gabrieltagama.menuplanner.core.domain.fake.FakeDishRepository
import com.gabrieltagama.menuplanner.core.domain.fake.FakeMenuSnapshotCodec
import com.gabrieltagama.menuplanner.core.domain.fake.FakeMenuSnapshotRepository
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes.dish
import com.gabrieltagama.menuplanner.core.domain.fake.TestImports
import com.gabrieltagama.menuplanner.core.domain.model.ImportSummary
import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import com.gabrieltagama.menuplanner.core.domain.model.share.MenuSnapshot
import com.gabrieltagama.menuplanner.core.domain.model.share.SnapshotScope
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
        val snapshot = MenuSnapshot(listOf(dish(id = "b", name = "Gazpacho")))
        codec.register("payload", snapshot)

        assertEquals(Outcome.Success(ImportSummary(1, 0, 0, 0, 0, 0)), TestImports.importMenus(dishes, codec)("payload"))
        assertEquals(setOf("a", "b"), dishes.current.keys)
    }

    @Test
    fun `import propagates decode failures without merging`() = runBlocking {
        val failure = Outcome.Failure(ShareError.UnsupportedImportVersion)

        assertEquals(failure, TestImports.importMenus(dishes, FakeMenuSnapshotCodec(failure))("bad"))
        assertTrue(dishes.upserted.isEmpty())
    }

    @Test
    fun `import converts the recipe book to standard units`() = runBlocking {
        codec.register("payload", MenuSnapshot(emptyList()))

        TestImports.importMenus(dishes, codec)("payload")

        assertEquals(listOf(Ingredient("Leche", 500.0, MeasureUnit.MILLILITER)), dishes.current.getValue("a").ingredients)
    }
}
