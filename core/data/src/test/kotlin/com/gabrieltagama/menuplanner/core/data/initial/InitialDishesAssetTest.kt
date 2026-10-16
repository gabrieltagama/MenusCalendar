package com.gabrieltagama.menuplanner.core.data.initial

import com.gabrieltagama.menuplanner.core.data.fake.TestData
import com.gabrieltagama.menuplanner.core.data.share.JsonMenuSnapshotCodec
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.MeasureUnit
import com.gabrieltagama.menuplanner.core.domain.model.measure.IngredientStandardizer
import com.gabrieltagama.menuplanner.core.domain.model.share.MenuSnapshot
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Checks that the bundled initial_dishes.json is a valid share file that the import accepts as a
 * whole: recipes only, unique ids and names, every dish type represented, and ingredients already
 * in standard units so the app does not need to convert them.
 */
class InitialDishesAssetTest {
    private val decoded = JsonMenuSnapshotCodec(TestData.json).decode(File(ASSET_PATH).readText())
    private val snapshot: MenuSnapshot get() = (decoded as Outcome.Success).value

    @Test
    fun `asset is accepted by the import validation`() = assertTrue(decoded is Outcome.Success)

    @Test
    fun `asset has recipes only with unique ids and names`() {
        assertTrue(snapshot.lunches.isEmpty() && snapshot.dinners.isEmpty())
        assertEquals(snapshot.dishes.size, snapshot.dishes.map { it.id }.distinct().size)
        assertEquals(snapshot.dishes.size, snapshot.dishes.map { it.name.lowercase() }.distinct().size)
    }

    @Test
    fun `asset covers every dish type`() = assertEquals(DishType.entries.toSet(), snapshot.dishes.map { it.type }.toSet())

    @Test
    fun `asset ingredients are already in standard units`() {
        val dishes = snapshot.dishes

        assertTrue(dishes.flatMap { it.ingredients }.all { it.unit in MeasureUnit.STANDARD })
        assertEquals(dishes, IngredientStandardizer.standardize(dishes))
    }

    private companion object {
        const val ASSET_PATH = "src/main/assets/initial_dishes.json"
    }
}
