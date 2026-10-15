package com.gabrieltagama.menuplanner.core.domain.common

import java.text.Collator
import java.util.Locale

/**
 * Alphabetical order of the app texts in Spanish, ignoring case and accents ("ñ" keeps its own
 * place). Every list of dishes and products shown to the user uses it, so they all sort alike.
 */
object SpanishOrder {

    private val locale: Locale = Locale.forLanguageTag("es-ES")

    fun <T> by(selector: (T) -> String): Comparator<T> =
        compareBy(Collator.getInstance(locale).apply { strength = Collator.PRIMARY }, selector)
}
