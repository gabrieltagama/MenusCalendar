package com.gabrieltagama.menuplanner.feature.dishes.list

import java.text.Normalizer

/**
 * Text normalization for case- and accent-insensitive search ("Pú" matches "puré").
 */
private val combiningMarks = Regex("\\p{Mn}+")

internal fun String.normalizedForSearch(): String =
    Normalizer.normalize(trim(), Normalizer.Form.NFD).replace(combiningMarks, "").lowercase()
