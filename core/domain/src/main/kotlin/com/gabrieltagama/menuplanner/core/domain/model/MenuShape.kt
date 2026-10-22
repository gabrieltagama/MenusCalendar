package com.gabrieltagama.menuplanner.core.domain.model

/**
 * The two shapes a lunch menu can take: starter + main (COURSES) or a single dish (SINGLE), with the
 * slots each one fills in menu order and the dish types those slots require. of names the shape
 * whose slots are exactly the filled ones; any other combination (a single dish next to a starter
 * or a main, or half a course menu) has no shape.
 */
enum class MenuShape(val slots: List<MenuSlot>) {
    COURSES(listOf(MenuSlot.STARTER, MenuSlot.MAIN)),
    SINGLE(listOf(MenuSlot.SINGLE));

    val requiredTypes: List<DishType> get() = slots.map(MenuSlot::dishType)

    companion object {
        fun of(filled: Set<MenuSlot>): MenuShape? = entries.firstOrNull { it.slots.toSet() == filled }
    }
}
