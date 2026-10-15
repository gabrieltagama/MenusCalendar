package com.gabrieltagama.menuplanner.core.data.initial

import com.gabrieltagama.menuplanner.core.data.fake.TestData
import com.gabrieltagama.menuplanner.core.data.share.ShareDocumentCodec
import com.gabrieltagama.menuplanner.core.data.share.ShareDocumentMapper
import com.gabrieltagama.menuplanner.core.data.share.dto.ShareDocumentDto
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Checks that the bundled initial_dishes.json is a valid share document that the import accepts
 * as a whole: recipes only, unique ids and names, and every dish type represented.
 */
class InitialDishesAssetTest {
    private val codec = ShareDocumentCodec(TestData.json)
    private val document = (codec.decode(File(ASSET_PATH).readText()) as Outcome.Success<ShareDocumentDto>).value

    @Test
    fun `asset is accepted by the import validation`() {
        val content = ShareDocumentMapper.toImportContent(document)

        assertTrue(content is Outcome.Success)
        assertEquals(document.dishes.size, (content as Outcome.Success).value.dishes.size)
    }

    @Test
    fun `asset has recipes only with unique ids and names`() {
        assertTrue(document.mealDays.isEmpty())
        assertEquals(document.dishes.size, document.dishes.map { it.id }.distinct().size)
        assertEquals(document.dishes.size, document.dishes.map { it.name.lowercase() }.distinct().size)
    }

    @Test
    fun `asset covers every dish type`() =
        assertEquals(DishType.entries.map { it.name }.toSet(), document.dishes.map { it.type }.toSet())

    private companion object {
        const val ASSET_PATH = "src/main/assets/initial_dishes.json"
    }
}
