package com.gabrieltagama.menuplanner.core.domain.model

/**
 * Value object with the rule of the product name of an ingredient line: the text is trimmed
 * (normalize) and must not be empty afterwards. of returns null for a text that breaks the rule.
 */
@JvmInline
value class IngredientName private constructor(val value: String) {
    companion object {
        fun normalize(text: String): String = text.trim()

        fun of(text: String): IngredientName? = normalize(text).takeIf { it.isNotEmpty() }?.let(::IngredientName)

        fun isValid(text: String): Boolean = of(text) != null
    }
}
