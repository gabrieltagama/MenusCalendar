package com.gabrieltagama.menuplanner.core.domain.model

/**
 * The two shapes a lunch menu can take: starter + main (COURSES) or a single dish (SINGLE), with the
 * slots each one fills in menu order and the dish types those slots require.
 */
enum class MenuShape(val slots: List<MenuSlot>) {
    COURSES(listOf(MenuSlot.STARTER, MenuSlot.MAIN)),
    SINGLE(listOf(MenuSlot.SINGLE));

    val requiredTypes: List<DishType> get() = slots.map(MenuSlot::dishType)
}
