package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.fake.FakeShoppingChecklistRepository
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import com.gabrieltagama.menuplanner.core.domain.model.ShoppingItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Shopping checklist: a product is ticked by its key whatever its spelling, can be unticked, and
 * clearing unticks everything.
 */
class ShoppingChecklistUseCasesTest {
    private val repository = FakeShoppingChecklistRepository()
    private val onions = ShoppingItem(name = "Cebollas", quantity = 2.0, unit = MeasureUnit.UNIT)
    private val rice = ShoppingItem(name = "Arroz", quantity = 500.0, unit = MeasureUnit.GRAM)

    @Test
    fun `ticks products by key whatever their spelling`() = runBlocking {
        SetShoppingItemCheckedUseCase(repository)(onions, true)

        val checked = ObserveShoppingChecksUseCase(repository)().first()
        assertTrue(onions.copy(name = "cebolla").key in checked)
    }

    @Test
    fun `unticks one product and clears all`() = runBlocking {
        val setChecked = SetShoppingItemCheckedUseCase(repository)
        setChecked(onions, true)
        setChecked(rice, true)

        setChecked(onions, false)
        assertEquals(setOf(rice.key), repository.checked.value)

        ClearShoppingChecksUseCase(repository)()
        assertTrue(repository.checked.value.isEmpty())
    }
}
