package com.gabrieltagama.menuplanner.core.domain.model.measure

import java.text.Normalizer
import java.util.Locale

/**
 * Value object: comparison key of an ingredient name. It ignores case, accents, extra spaces and
 * Spanish plurals ("Calabacines", "calabacín" and "CALABACIN" share a key), so the same product
 * written in different ways is grouped as one ingredient.
 */
@JvmInline
value class IngredientKey private constructor(val value: String) {

    companion object {
        fun of(name: String): IngredientKey = IngredientKey(
            name.lowercase(spanishLocale)
                .withoutAccents()
                .split(whitespace)
                .filter { it.isNotEmpty() }
                .joinToString(" ") { it.singular() }
        )
    }
}

private const val MIN_INFLECTED_LENGTH = 3

private val spanishLocale: Locale = Locale.forLanguageTag("es-ES")

private val whitespace = Regex("\\s+")

private val combiningMarks = Regex("\\p{Mn}+")

private fun String.withoutAccents(): String = Normalizer.normalize(this, Normalizer.Form.NFD).replace(combiningMarks, "")

private fun String.singular(): String =
    if (length > MIN_INFLECTED_LENGTH) removeSuffix("s").removeSuffix("e") else this
