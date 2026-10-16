package com.gabrieltagama.menuplanner.core.domain.model

/**
 * Rating of a dish from one to five stars, informative only. Persisted by name.
 */
enum class Rating(val stars: Int) { ONE(1), TWO(2), THREE(3), FOUR(4), FIVE(5) }
