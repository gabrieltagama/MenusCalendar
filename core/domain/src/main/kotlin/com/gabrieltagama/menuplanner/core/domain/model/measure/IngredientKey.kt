package com.gabrieltagama.menuplanner.core.domain.model.measure

import com.gabrieltagama.menuplanner.core.domain.text.foldedCaseAndAccents
import com.gabrieltagama.menuplanner.core.domain.text.words

/**
 * Value object: comparison key of an ingredient name. It ignores case, accents, extra spaces and
 * Spanish plurals ("Calabacines", "calabacín" and "CALABACIN" share a key), so the same product
 * written in different ways is grouped as one ingredient. fromValue rebuilds a key already
 * computed and stored, without normalizing it again.
 */
@JvmInline
value class IngredientKey private constructor(val value: String) {

    companion object {
        fun of(name: String): IngredientKey = IngredientKey(name.foldedCaseAndAccents().words().joinToString(" ") { it.singular() })

        fun fromValue(value: String): IngredientKey = IngredientKey(value)
    }
}

private const val MIN_INFLECTED_LENGTH = 3

private fun String.singular(): String =
    if (length > MIN_INFLECTED_LENGTH) removeSuffix("s").removeSuffix("e") else this
