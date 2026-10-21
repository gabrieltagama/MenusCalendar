package com.gabrieltagama.menuplanner.core.domain.model.share

import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.text.foldedCaseAndAccents

/**
 * Natural identity of a dish: its name without accents, case, punctuation or extra spaces, plus its
 * type. An import uses it when the id of an incoming dish is unknown locally, so the same recipe
 * created in another installation (after reinstalling the app, or shared by someone else) updates
 * the local one instead of being duplicated. "Lentejas con chorizo" and "lentejas  con  CHORIZO."
 * share a key; the same name as a starter and as a single dish do not.
 */
@JvmInline
value class DishKey private constructor(val value: String) {

    companion object {
        private val SEPARATORS = Regex("[^\\p{L}\\p{N}]+")

        fun of(dish: Dish): DishKey = of(dish.name, dish.type)

        fun of(name: String, type: DishType): DishKey = DishKey("${normalize(name)}|${type.name}")

        private fun normalize(name: String): String =
            name.foldedCaseAndAccents().replace(SEPARATORS, " ").trim()
    }
}
