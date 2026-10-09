package com.gabrieltagama.menuplanner.core.domain.model

/**
 * Domain enumerations. Persisted and exported by name, so constants must never be renamed;
 * new values can be appended safely.
 */
enum class DishType { STARTER, MAIN, DESSERT, SINGLE }

enum class Heaviness { VERY_LOW, MEDIUM, VERY_HIGH }

enum class MeasureUnit { GRAM, KILOGRAM, MILLILITER, LITER, UNIT, TABLESPOON, TEASPOON, CUP, PINCH, TO_TASTE }
