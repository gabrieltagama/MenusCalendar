package com.gabrieltagama.menuplanner.core.domain.model

/**
 * Value object with the rule of a dish name, shared by the domain, the editor and the imports: the
 * text is trimmed (normalize) and must not be empty afterwards. of returns null for a text that
 * breaks the rule, so a DishName always holds a valid name.
 */
@JvmInline
value class DishName private constructor(val value: String) {
    companion object {
        fun normalize(text: String): String = text.trim()

        fun of(text: String): DishName? = normalize(text).takeIf { it.isNotEmpty() }?.let(::DishName)

        fun isValid(text: String): Boolean = of(text) != null
    }
}
