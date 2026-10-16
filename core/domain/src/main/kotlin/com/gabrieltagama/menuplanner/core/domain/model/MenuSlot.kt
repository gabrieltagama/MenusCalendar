package com.gabrieltagama.menuplanner.core.domain.model

/**
 * Position of a dish in a lunch menu and the dish type it accepts.
 */
enum class MenuSlot(val dishType: DishType) {
    STARTER(DishType.STARTER),
    MAIN(DishType.MAIN),
    SINGLE(DishType.SINGLE)
}
