package com.gabrieltagama.menuplanner.core.domain.model

/**
 * Kind of dish of the recipe book. Persisted and exported by name, so constants must never be
 * renamed; new values can be appended safely. Desserts are kept in the book but never planned.
 */
enum class DishType { STARTER, MAIN, DESSERT, SINGLE }
