package com.gabrieltagama.menuplanner.feature.dishes.list

import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import com.gabrieltagama.menuplanner.core.domain.model.MealTime
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import com.gabrieltagama.menuplanner.core.domain.model.Rating
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * DishFilter: name and ingredient search (case- and accent-insensitive), multiple-selection type,
 * heaviness, rating and meal time filters (empty set = no filter), their combination and
 * alphabetical ordering.
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
        assertEquals(listOf(salad, flan, lentils, chicken, puree), applyFilter(dishes, DishFilterCriteria()))

    @Test
    fun `query matches dish name`() =
        assertEquals(listOf(chicken), applyFilter(dishes, DishFilterCriteria(query = "asado")))

    @Test
    fun `query matches ingredient name`() =
        assertEquals(listOf(lentils), applyFilter(dishes, DishFilterCriteria(query = "chorizo")))

    @Test
    fun `query matches name or ingredient of different dishes`() =
        assertEquals(listOf(chicken, puree), applyFilter(dishes, DishFilterCriteria(query = "pollo")))

    @Test
    fun `search ignores accents and case in query and text`() {
        assertEquals(listOf(puree), applyFilter(dishes, DishFilterCriteria(query = "  PURE ")))
        assertEquals(listOf(salad), applyFilter(dishes, DishFilterCriteria(query = "atun")))
        assertEquals(listOf(flan), applyFilter(dishes, DishFilterCriteria(query = "AZÚCAR")))
    }

    @Test
    fun `filters by type`() =
        assertEquals(listOf(salad, puree), applyFilter(dishes, DishFilterCriteria(types = setOf(DishType.STARTER))))

    @Test
    fun `filters by heaviness`() =
        assertEquals(listOf(flan, chicken), applyFilter(dishes, DishFilterCriteria(heaviness = setOf(Heaviness.MEDIUM))))

    @Test
    fun `combines type and heaviness`() {
        assertEquals(
            listOf(chicken),
            applyFilter(dishes, DishFilterCriteria(types = setOf(DishType.MAIN), heaviness = setOf(Heaviness.MEDIUM)))
        )
        assertEquals(
            emptyList<Dish>(),
            applyFilter(dishes, DishFilterCriteria(types = setOf(DishType.STARTER), heaviness = setOf(Heaviness.VERY_HIGH)))
        )
    }

    @Test
    fun `combines query with type and heaviness`() =
        assertEquals(
            listOf(puree),
            applyFilter(dishes, DishFilterCriteria(query = "caldo", types = setOf(DishType.STARTER), heaviness = setOf(Heaviness.VERY_LOW)))
        )

    private val rated = listOf(
        puree.copy(rating = Rating.FIVE, mealTime = MealTime.LUNCH),
        salad.copy(rating = Rating.FOUR, mealTime = MealTime.DINNER),
        lentils.copy(rating = Rating.TWO, mealTime = MealTime.ANY),
        chicken.copy(rating = Rating.THREE, mealTime = MealTime.LUNCH),
        flan.copy(rating = Rating.ONE, mealTime = MealTime.DINNER)
    )

    @Test
    fun `several types keep dishes of any of them`() =
        assertEquals(listOf(flan, chicken), applyFilter(dishes, DishFilterCriteria(types = setOf(DishType.MAIN, DishType.DESSERT))))

    @Test
    fun `several heaviness values keep dishes of any of them`() =
        assertEquals(listOf(salad, lentils, puree), applyFilter(dishes, DishFilterCriteria(heaviness = setOf(Heaviness.VERY_LOW, Heaviness.VERY_HIGH))))

    @Test
    fun `ratings filter keeps only the selected star values`() {
        assertEquals(listOf("salad", "puree"), applyFilter(rated, DishFilterCriteria(ratings = setOf(Rating.FOUR, Rating.FIVE))).map { it.id.value })
        assertEquals(listOf("flan", "chicken"), applyFilter(rated, DishFilterCriteria(ratings = setOf(Rating.ONE, Rating.THREE))).map { it.id.value })
    }

    @Test
    fun `meal time filter matches exactly`() {
        assertEquals(listOf("salad", "flan"), applyFilter(rated, DishFilterCriteria(mealTimes = setOf(MealTime.DINNER))).map { it.id.value })
        assertEquals(listOf("lentils"), applyFilter(rated, DishFilterCriteria(mealTimes = setOf(MealTime.ANY))).map { it.id.value })
    }

    @Test
    fun `dinner and any together keep both`() =
        assertEquals(listOf("salad", "flan", "lentils"), applyFilter(rated, DishFilterCriteria(mealTimes = setOf(MealTime.DINNER, MealTime.ANY))).map { it.id.value })

    @Test
    fun `combines ratings and meal times with types`() =
        assertEquals(
            listOf("puree"),
            applyFilter(
                rated,
                DishFilterCriteria(types = setOf(DishType.STARTER), ratings = setOf(Rating.FOUR, Rating.FIVE), mealTimes = setOf(MealTime.LUNCH))
            ).map { it.id.value }
        )

    private fun dish(id: String, name: String, type: DishType, heaviness: Heaviness, vararg ingredients: String) = Dish(
        id = DishId(id),
        name = name,
        ingredients = ingredients.map { Ingredient(it, 1.0, MeasureUnit.UNIT) },
        type = type,
        heaviness = heaviness,
        updatedAt = Instant.EPOCH
    )

    private fun applyFilter(dishes: List<Dish>, criteria: DishFilterCriteria): List<Dish> = DishFilter.filter(DishFilter.index(dishes), criteria)
}
