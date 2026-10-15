package com.gabrieltagama.menuplanner.core.domain.model

/**
 * A place of the lunch menu. Each slot accepts only dishes of its dishType; every slot but the
 * dessert is required by the menu shape it belongs to. This is the single source of the
 * slot-to-type rule used when saving, importing and editing a day.
 */
enum class MenuSlot(val dishType: DishType) {
    STARTER(DishType.STARTER),
    MAIN(DishType.MAIN),
    SINGLE(DishType.SINGLE),
    DESSERT(DishType.DESSERT);

    val isRequired: Boolean get() = this != DESSERT

    fun accepts(type: DishType): Boolean = type == dishType
}
