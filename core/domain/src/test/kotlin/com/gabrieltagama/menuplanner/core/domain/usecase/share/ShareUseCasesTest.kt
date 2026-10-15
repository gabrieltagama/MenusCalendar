package com.gabrieltagama.menuplanner.core.domain.usecase.share

import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.fake.FakeDishRepository
import com.gabrieltagama.menuplanner.core.domain.fake.FakeMenuShareRepository
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes.dish
import com.gabrieltagama.menuplanner.core.domain.model.ImportSummary
import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import com.gabrieltagama.menuplanner.core.domain.usecase.dish.StandardizeRecipeBookUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests that ExportMenusUseCase and ImportMenusUseCase delegate to the share repository and that
 * imported recipes end in standard units.
 */
class ShareUseCasesTest {

    @Test
    fun `export returns repository json`() = runBlocking {
        val repository = FakeMenuShareRepository(exportJson = """{"dishes":[]}""")

        assertEquals("""{"dishes":[]}""", ExportMenusUseCase(repository)())
        assertEquals(1, repository.exportCalls)
    }

    @Test
    fun `import passes json and returns repository summary`() = runBlocking {
        val summary = ImportSummary(1, 2, 3, 4, 5, 6)
        val repository = FakeMenuShareRepository(importResult = Outcome.Success(summary))

        assertEquals(Outcome.Success(summary), ImportMenusUseCase(repository, StandardizeRecipeBookUseCase(FakeDishRepository()))("payload"))
        assertEquals(listOf("payload"), repository.importedJson)
    }

    @Test
    fun `import propagates repository failure`() = runBlocking {
        val failure = Outcome.Failure(DomainError.UnsupportedImportVersion)
        val repository = FakeMenuShareRepository(importResult = failure)

        assertEquals(failure, ImportMenusUseCase(repository, StandardizeRecipeBookUseCase(FakeDishRepository()))("bad"))
    }

    @Test
    fun `import converts the recipe book to standard units`() = runBlocking {
        val dishes = FakeDishRepository(listOf(dish(id = "a", ingredients = listOf(Ingredient("Leche", 0.5, MeasureUnit.LITER)))))
        val repository = FakeMenuShareRepository(importResult = Outcome.Success(ImportSummary.EMPTY))

        ImportMenusUseCase(repository, StandardizeRecipeBookUseCase(dishes))("payload")

        assertEquals(listOf(Ingredient("Leche", 500.0, MeasureUnit.MILLILITER)), dishes.current.getValue("a").ingredients)
    }
}
