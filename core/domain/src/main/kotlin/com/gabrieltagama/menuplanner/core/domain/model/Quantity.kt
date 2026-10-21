package com.gabrieltagama.menuplanner.core.domain.model

/**
 * Value object with the rule of an ingredient quantity, shared by the domain, the editor and the
 * imports: a finite number that is not negative. of returns null for a value that breaks the rule,
 * so a Quantity always holds a valid amount.
 */
@JvmInline
value class Quantity private constructor(val value: Double) {
    companion object {
        val ZERO: Quantity = Quantity(0.0)

        fun of(value: Double): Quantity? = value.takeIf { it.isFinite() && it >= 0.0 }?.let(::Quantity)

        fun isValid(value: Double): Boolean = of(value) != null
    }
}
