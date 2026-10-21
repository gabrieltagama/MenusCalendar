package com.gabrieltagama.menuplanner.core.domain.model

/**
 * Units of an ingredient line. Ingredients are kept in the STANDARD units (grams, millilitres or
 * units); the other values remain only to read old data and shared files, and are converted on
 * save, import and app start. METRIC_FACTOR is the step between grams and kilograms and between
 * millilitres and litres. Persisted by name, so constants must never be renamed.
 */
enum class MeasureUnit {
    GRAM, KILOGRAM, MILLILITER, LITER, UNIT, TABLESPOON, TEASPOON, CUP, PINCH, TO_TASTE;

    val isStandard: Boolean get() = this in STANDARD

    val isVolume: Boolean get() = this == MILLILITER || this == LITER

    companion object {
        const val METRIC_FACTOR = 1000.0

        val STANDARD: List<MeasureUnit> = listOf(GRAM, MILLILITER, UNIT)
    }
}
