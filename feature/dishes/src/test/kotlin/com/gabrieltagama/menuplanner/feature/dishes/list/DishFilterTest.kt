package com.gabrieltagama.menuplanner.feature.dishes.list

import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import com.gabrieltagama.menuplanner.core.domain.model.MealTime
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import com.gabrieltagama.menuplanner.core.domain.model.Rating
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * DishFilter: name and ingredient search (case- and accent-insensitive), type, heaviness,
 * minimum rating and meal time filters, their combination and alphabetical ordering.
 */
class DishFilterTest {

    private val puree = dish("puree", "Puré de calabaza", DishType.STARTER, Heaviness.VERY_LOW, "Calabaza", "Caldo de pollo")
    private val salad = dish("salad", "Ensalada mixta", DishType.STARTER, Heaviness.VERY_LOW, "Lechuga", "Atún")
    private val lentils = dish("lentils", "Lentejas estofadas", DishType.SINGLE, Heaviness.VERY_HIGH, "Lentejas", "Chorizo")
    private val chicken = dish("chicken", "Pollo asado", DishType.MAIN, Heaviness.MEDIUM, "Pollo", "Patata")
    private val flan = dish("flan", "Flan de huevo", DishType.DESSERT, Heaviness.MEDIUM, "Huevo", "Azúcar")

    private val dishes = listOf(puree, salad, lentils, chicken, flan)

    @Test
    fun `empty criteria returns every dish sorted by name`() =
        assertEquals(listOf(salad, flan, lentils, chicken, puree), DishFilter.apply(dishes, DishFilterCriteria()))

    @Test
    fun `query matches dish name`() =
        assertEquals(listOf(chicken), DishFilter.apply(dishes, DishFilterCriteria(query = "asado")))

    @Test
    fun `query matches ingredient name`() =
        assertEquals(listOf(lentils), DishFilter.apply(dishes, DishFilterCriteria(query = "chorizo")))

    @Test
    fun `query matches name or ingredient of different dishes`() =
        assertEquals(listOf(chicken, puree), DishFilter.apply(dishes, DishFilterCriteria(query = "pollo")))

    @Test
    fun `search ignores accents and case in query and text`() {
        assertEquals(listOf(puree), DishFilter.apply(dishes, DishFilterCriteria(query = "  PURE ")))
        assertEquals(listOf(salad), DishFilter.apply(dishes, DishFilterCriteria(query = "atun")))
        assertEquals(listOf(flan), DishFilter.apply(dishes, DishFilterCriteria(query = "AZÚCAR")))
    }

    @Test
    fun `filters by type`() =
        assertEquals(listOf(salad, puree), DishFilter.apply(dishes, DishFilterCriteria(type = DishType.STARTER)))

    @Test
    fun `filters by heaviness`() =
        assertEquals(listOf(flan, chicken), DishFilter.apply(dishes, DishFilterCriteria(heaviness = Heaviness.MEDIUM)))

    @Test
    fun `combines type and heaviness`() {
        assertEquals(
            listOf(chicken),
            DishFilter.apply(dishes, DishFilterCriteria(type = DishType.MAIN, heaviness = Heaviness.MEDIUM))
        )
        assertEquals(
            emptyList<Dish>(),
            DishFilter.apply(dishes, DishFilterCriteria(type = DishType.STARTER, heaviness = Heaviness.VERY_HIGH))
        )
    }

    @Test
    fun `combines query with type and heaviness`() =
        assertEquals(
            listOf(puree),
            DishFilter.apply(dishes, DishFilterCriteria(query = "caldo", type = DishType.STARTER, heaviness = Heaviness.VERY_LOW))
        )

    private val rated = listOf(
        puree.copy(rating = Rating.FIVE, mealTime = MealTime.LUNCH),
        salad.copy(rating = Rating.FOUR, mealTime = MealTime.DINNER),
        lentils.copy(rating = Rating.TWO, mealTime = MealTime.ANY),
        chicken.copy(rating = Rating.THREE, mealTime = MealTime.LUNCH),
        flan.copy(rating = Rating.ONE, mealTime = MealTime.DINNER)
    )

    @Test
    fun `minimum rating keeps dishes with at least that many stars`() {
        assertEquals(listOf("salad", "puree"), DishFilter.apply(rated, DishFilterCriteria(minRating = Rating.FOUR)).map(Dish::id))
        assertEquals(listOf("puree"), DishFilter.apply(rated, DishFilterCriteria(minRating = Rating.FIVE)).map(Dish::id))
        assertEquals(5, DishFilter.apply(rated, DishFilterCriteria(minRating = Rating.ONE)).size)
    }

    @Test
    fun `lunch filter includes dishes served at any time`() =
        assertEquals(listOf("lentils", "chicken", "puree"), DishFilter.apply(rated, DishFilterCriteria(mealTime = MealTime.LUNCH)).map(Dish::id))

    @Test
    fun `dinner filter includes dishes served at any time`() =
        assertEquals(listOf("salad", "flan", "lentils"), DishFilter.apply(rated, DishFilterCriteria(mealTime = MealTime.DINNER)).map(Dish::id))

    @Test
    fun `any filter keeps only dishes served at any time`() =
        assertEquals(listOf("lentils"), DishFilter.apply(rated, DishFilterCriteria(mealTime = MealTime.ANY)).map(Dish::id))

    @Test
    fun `combines minimum rating and meal time with type`() =
        assertEquals(
            listOf("puree"),
            DishFilter.apply(rated, DishFilterCriteria(type = DishType.STARTER, minRating = Rating.FOUR, mealTime = MealTime.LUNCH)).map(Dish::id)
        )

    private fun dish(id: String, name: String, type: DishType, heaviness: Heaviness, vararg ingredients: String) = Dish(
        id = id,
        name = name,
        ingredients = ingredients.map { Ingredient(it, 1.0, MeasureUnit.UNIT) },
        type = type,
        heaviness = heaviness
    )
}
