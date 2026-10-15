package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.fake.FakeDinnerPlanRepository
import com.gabrieltagama.menuplanner.core.domain.fake.FakeMealPlanRepository
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes.dish
import com.gabrieltagama.menuplanner.core.domain.model.DailyMenu
import com.gabrieltagama.menuplanner.core.domain.model.DinnerDay
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import com.gabrieltagama.menuplanner.core.domain.model.Meal
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
 * dishes of one month and its dinners in standard units, one line per product, leaving desserts and
 * other months out, that it can be limited to lunches or to dinners and to a date range that may
 * span several months.
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

    private fun build(
        vararg days: MealDay,
        dinners: List<DinnerDay> = emptyList(),
        meals: Set<Meal> = Meal.entries.toSet()
    ) = runBlocking {
        BuildShoppingListUseCase(FakeMealPlanRepository(days.toList()), FakeDinnerPlanRepository(dinners))(october, meals)
    }

    @Test
    fun `adds up every planned day in standard units with one line per product`() {
        val items = build(
            day(LocalDate.of(2026, 10, 1), DailyMenu.Courses(starter = starter, main = main)),
            day(LocalDate.of(2026, 10, 2), DailyMenu.Courses(starter = starter, main = main)),
            day(LocalDate.of(2026, 10, 3), DailyMenu.Single(single = single))
        )

        assertEquals(
            listOf(
                ShoppingItem(name = "Aceite", quantity = 200.0, unit = MeasureUnit.MILLILITER),
                ShoppingItem(name = "Arroz", quantity = 400.0, unit = MeasureUnit.GRAM),
                ShoppingItem(name = "Orégano", quantity = 4.0, unit = MeasureUnit.GRAM),
                ShoppingItem(name = "Pollo", quantity = 2.0, unit = MeasureUnit.UNIT),
                ShoppingItem(name = "Sal", quantity = 15.0, unit = MeasureUnit.GRAM),
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
    fun `weighs counted units when the same product is also weighed`() {
        val eggsByUnit = dish(type = DishType.SINGLE, ingredients = listOf(Ingredient("Huevos", 2.0, MeasureUnit.UNIT)))
        val eggsByWeight = dish(type = DishType.SINGLE, ingredients = listOf(Ingredient("Huevo", 120.0, MeasureUnit.GRAM)))

        val items = build(
            day(LocalDate.of(2026, 10, 6), DailyMenu.Single(single = eggsByUnit)),
            day(LocalDate.of(2026, 10, 7), DailyMenu.Single(single = eggsByWeight))
        )

        assertEquals(listOf(ShoppingItem(name = "Huevos", quantity = 240.0, unit = MeasureUnit.GRAM)), items)
    }

    @Test
    fun `merges spoons pinches and plurals of the same product into one line`() {
        val stew = dish(
            type = DishType.SINGLE,
            ingredients = listOf(
                Ingredient("Aceite de oliva", 2.0, MeasureUnit.TABLESPOON),
                Ingredient("Harina", 1.0, MeasureUnit.TABLESPOON),
                Ingredient("Calabacines", 2.0, MeasureUnit.UNIT)
            )
        )
        val salad = dish(
            type = DishType.SINGLE,
            ingredients = listOf(
                Ingredient("aceite de oliva", 0.0, MeasureUnit.TO_TASTE),
                Ingredient("Harina", 100.0, MeasureUnit.GRAM),
                Ingredient("Calabacín", 1.0, MeasureUnit.UNIT)
            )
        )

        val items = build(
            day(LocalDate.of(2026, 10, 8), DailyMenu.Single(single = stew)),
            day(LocalDate.of(2026, 10, 9), DailyMenu.Single(single = salad))
        )

        assertEquals(
            listOf(
                ShoppingItem(name = "Aceite de oliva", quantity = 40.0, unit = MeasureUnit.MILLILITER),
                ShoppingItem(name = "Calabacines", quantity = 3.0, unit = MeasureUnit.UNIT),
                ShoppingItem(name = "Harina", quantity = 120.0, unit = MeasureUnit.GRAM)
            ),
            items
        )
    }

    @Test
    fun `adds the dinners of the month to the lunches`() {
        val omelette = dish(type = DishType.MAIN, ingredients = listOf(Ingredient("Huevos", 3.0, MeasureUnit.UNIT)))
        val lunch = dish(type = DishType.SINGLE, ingredients = listOf(Ingredient("Huevo", 2.0, MeasureUnit.UNIT)))

        val items = build(
            day(LocalDate.of(2026, 10, 10), DailyMenu.Single(single = lunch)),
            dinners = listOf(
                DinnerDay(date = LocalDate.of(2026, 10, 10), dish = omelette),
                DinnerDay(date = LocalDate.of(2026, 11, 1), dish = omelette)
            )
        )

        assertEquals(listOf(ShoppingItem(name = "Huevo", quantity = 5.0, unit = MeasureUnit.UNIT)), items)
    }

    @Test
    fun `lists only the lunches when only lunch is selected`() {
        val omelette = dish(type = DishType.MAIN, ingredients = listOf(Ingredient("Huevos", 3.0, MeasureUnit.UNIT)))
        val lunch = dish(type = DishType.SINGLE, ingredients = listOf(Ingredient("Arroz", 200.0, MeasureUnit.GRAM)))

        val items = build(
            day(LocalDate.of(2026, 10, 10), DailyMenu.Single(single = lunch)),
            dinners = listOf(DinnerDay(date = LocalDate.of(2026, 10, 10), dish = omelette)),
            meals = setOf(Meal.LUNCH)
        )

        assertEquals(listOf(ShoppingItem(name = "Arroz", quantity = 200.0, unit = MeasureUnit.GRAM)), items)
    }

    @Test
    fun `lists only the dinners when only dinner is selected`() {
        val omelette = dish(type = DishType.MAIN, ingredients = listOf(Ingredient("Huevos", 3.0, MeasureUnit.UNIT)))
        val lunch = dish(type = DishType.SINGLE, ingredients = listOf(Ingredient("Arroz", 200.0, MeasureUnit.GRAM)))

        val items = build(
            day(LocalDate.of(2026, 10, 10), DailyMenu.Single(single = lunch)),
            dinners = listOf(DinnerDay(date = LocalDate.of(2026, 10, 10), dish = omelette)),
            meals = setOf(Meal.DINNER)
        )

        assertEquals(listOf(ShoppingItem(name = "Huevos", quantity = 3.0, unit = MeasureUnit.UNIT)), items)
    }

    @Test
    fun `lists only the days inside the date range, even across months`() {
        val rice = dish(type = DishType.SINGLE, ingredients = listOf(Ingredient("Arroz", 100.0, MeasureUnit.GRAM)))
        val omelette = dish(type = DishType.MAIN, ingredients = listOf(Ingredient("Huevos", 1.0, MeasureUnit.UNIT)))
        val lunches = listOf(
            day(LocalDate.of(2026, 10, 14), DailyMenu.Single(single = rice)),
            day(LocalDate.of(2026, 10, 15), DailyMenu.Single(single = rice)),
            day(LocalDate.of(2026, 11, 2), DailyMenu.Single(single = rice)),
            day(LocalDate.of(2026, 11, 3), DailyMenu.Single(single = rice))
        )
        val dinners = listOf(
            DinnerDay(date = LocalDate.of(2026, 10, 14), dish = omelette),
            DinnerDay(date = LocalDate.of(2026, 10, 31), dish = omelette)
        )

        val items = runBlocking {
            BuildShoppingListUseCase(FakeMealPlanRepository(lunches), FakeDinnerPlanRepository(dinners))(
                LocalDate.of(2026, 10, 15)..LocalDate.of(2026, 11, 2)
            )
        }

        assertEquals(
            listOf(
                ShoppingItem(name = "Arroz", quantity = 200.0, unit = MeasureUnit.GRAM),
                ShoppingItem(name = "Huevos", quantity = 1.0, unit = MeasureUnit.UNIT)
            ),
            items
        )
    }
}
