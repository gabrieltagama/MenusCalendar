package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.fake.FakeMealPlanRepository
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes.dish
import com.gabrieltagama.menuplanner.core.domain.model.DailyMenu
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import com.gabrieltagama.menuplanner.core.domain.model.MealDay
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import com.gabrieltagama.menuplanner.core.domain.model.ShoppingItem
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests that BuildShoppingListUseCase adds up the ingredients of the starters, mains and single
 * dishes of one month, merging compatible units and leaving desserts and other months out.
 */
class BuildShoppingListUseCaseTest {
    private val october = YearMonth.of(2026, 10)

    private val starter = dish(
        name = "Ensalada",
        type = DishType.STARTER,
        ingredients = listOf(
            Ingredient(name = "Tomate", quantity = 500.0, unit = MeasureUnit.GRAM),
            Ingredient(name = "Aceite", quantity = 50.0, unit = MeasureUnit.MILLILITER),
            Ingredient(name = "Sal", quantity = 0.0, unit = MeasureUnit.TO_TASTE)
        )
    )
    private val main = dish(
        name = "Pollo con tomate",
        type = DishType.MAIN,
        ingredients = listOf(
            Ingredient(name = " tomate ", quantity = 1.0, unit = MeasureUnit.KILOGRAM),
            Ingredient(name = "Pollo", quantity = 1.0, unit = MeasureUnit.UNIT),
            Ingredient(name = "Orégano", quantity = 1.0, unit = MeasureUnit.PINCH)
        )
    )
    private val single = dish(
        name = "Paella",
        type = DishType.SINGLE,
        ingredients = listOf(
            Ingredient(name = "Arroz", quantity = 0.4, unit = MeasureUnit.KILOGRAM),
            Ingredient(name = "aceite", quantity = 0.1, unit = MeasureUnit.LITER),
            Ingredient(name = "SAL", quantity = 0.0, unit = MeasureUnit.TO_TASTE)
        )
    )
    private val dessert = dish(
        name = "Flan",
        type = DishType.DESSERT,
        ingredients = listOf(Ingredient(name = "Azúcar", quantity = 100.0, unit = MeasureUnit.GRAM))
    )

    private fun day(date: LocalDate, menu: DailyMenu) = MealDay(date = date, menu = menu)

    private fun build(vararg days: MealDay) = runBlocking {
        BuildShoppingListUseCase(FakeMealPlanRepository(days.toList()))(october)
    }

    @Test
    fun `adds up every planned day and merges grams with kilograms and millilitres with litres`() {
        val items = build(
            day(LocalDate.of(2026, 10, 1), DailyMenu.Courses(starter = starter, main = main)),
            day(LocalDate.of(2026, 10, 2), DailyMenu.Courses(starter = starter, main = main)),
            day(LocalDate.of(2026, 10, 3), DailyMenu.Single(single = single))
        )

        assertEquals(
            listOf(
                ShoppingItem(name = "Aceite", quantity = 200.0, unit = MeasureUnit.MILLILITER),
                ShoppingItem(name = "Arroz", quantity = 400.0, unit = MeasureUnit.GRAM),
                ShoppingItem(name = "Orégano", quantity = null, unit = MeasureUnit.PINCH),
                ShoppingItem(name = "Pollo", quantity = 2.0, unit = MeasureUnit.UNIT),
                ShoppingItem(name = "Sal", quantity = null, unit = MeasureUnit.TO_TASTE),
                ShoppingItem(name = "Tomate", quantity = 3.0, unit = MeasureUnit.KILOGRAM)
            ),
            items
        )
    }

    @Test
    fun `leaves desserts out`() {
        val items = build(day(LocalDate.of(2026, 10, 5), DailyMenu.Single(single = single, dessert = dessert)))

        assertTrue(items.none { it.name == "Azúcar" })
    }

    @Test
    fun `ignores days of other months`() {
        val items = build(day(LocalDate.of(2026, 11, 1), DailyMenu.Courses(starter = starter, main = main)))

        assertEquals(emptyList<ShoppingItem>(), items)
    }

    @Test
    fun `keeps incompatible units of the same ingredient in separate lines`() {
        val eggsByUnit = dish(type = DishType.SINGLE, ingredients = listOf(Ingredient("Huevo", 2.0, MeasureUnit.UNIT)))
        val eggsByWeight = dish(type = DishType.SINGLE, ingredients = listOf(Ingredient("Huevo", 120.0, MeasureUnit.GRAM)))

        val items = build(
            day(LocalDate.of(2026, 10, 6), DailyMenu.Single(single = eggsByUnit)),
            day(LocalDate.of(2026, 10, 7), DailyMenu.Single(single = eggsByWeight))
        )

        assertEquals(setOf(MeasureUnit.UNIT, MeasureUnit.GRAM), items.map { it.unit }.toSet())
    }
}
