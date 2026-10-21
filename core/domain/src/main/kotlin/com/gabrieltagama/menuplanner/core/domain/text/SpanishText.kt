package com.gabrieltagama.menuplanner.core.domain.text

import java.text.Collator
import java.text.Normalizer
import java.util.Locale

/**
 * Spanish text rules shared by the whole app: the locale of every Spanish text, the alphabetical
 * order that ignores case and accents (a new Collator each time, because it is not thread-safe),
 * removing accents so "Pú" and "pu" compare equal, folding case and accents in one step for every
 * comparison key (search, dish and ingredient keys), and splitting or collapsing whitespace.
 */
val spanishLocale: Locale = Locale.forLanguageTag("es-ES")

private val combiningMarks = Regex("\\p{Mn}+")

private val whitespace = Regex("\\s+")

fun spanishCollator(): Collator = Collator.getInstance(spanishLocale).apply { strength = Collator.PRIMARY }

fun String.withoutAccents(): String = Normalizer.normalize(this, Normalizer.Form.NFD).replace(combiningMarks, "")

fun String.foldedCaseAndAccents(): String = withoutAccents().lowercase(spanishLocale)

fun String.collapsedSpaces(): String = trim().replace(whitespace, " ")

fun String.words(): List<String> = trim().split(whitespace).filter(String::isNotEmpty)
