package com.gabrieltagama.menuplanner.feature.dishes.list

import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * DishFilter: name and ingredient search (case- and accent-insensitive), type and heaviness
 * filters, their combination and alphabetical ordering.
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

    private fun dish(id: String, name: String, type: DishType, heaviness: Heaviness, vararg ingredients: String) = Dish(
        id = id,
        name = name,
        ingredients = ingredients.map { Ingredient(it, 1.0, MeasureUnit.UNIT) },
        type = type,
        heaviness = heaviness
    )
}
