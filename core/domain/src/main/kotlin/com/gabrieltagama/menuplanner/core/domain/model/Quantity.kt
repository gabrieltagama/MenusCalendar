package com.gabrieltagama.menuplanner.core.domain.model

/**
 * Value object with the rule of an ingredient quantity, shared by the domain, the editor and the
 * imports: a finite number that is not negative. of returns null for a value that breaks the rule,
 * so a Quantity always holds a valid amount. parse reads what the user typed, with a comma or a dot
 * as decimal separator; a blank text is accepted only for "to taste", as zero.
 */
@JvmInline
value class Quantity private constructor(val value: Double) {
    companion object {
        val ZERO: Quantity = Quantity(0.0)

        fun of(value: Double): Quantity? = value.takeIf { it.isFinite() && it >= 0.0 }?.let(::Quantity)

        fun isValid(value: Double): Boolean = of(value) != null

        fun parse(text: String, unit: MeasureUnit): Quantity? =
            if (text.isBlank() && unit == MeasureUnit.TO_TASTE) ZERO else text.trim().replace(',', '.').toDoubleOrNull()?.let(::of)
    }
}
