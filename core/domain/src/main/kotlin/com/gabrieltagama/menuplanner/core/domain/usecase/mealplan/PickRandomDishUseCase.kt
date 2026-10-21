package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.model.Dish
import javax.inject.Inject
import kotlin.random.Random

/**
 * Picks a random dish among the options of a lunch slot or a dinner, different from the one already
 * chosen when another option exists. The Random is injected, so tests can use a fixed seed.
 */
class PickRandomDishUseCase @Inject constructor(private val random: Random) {
    operator fun invoke(options: List<Dish>, current: Dish?): Dish? =
        options.filterNot { it.id == current?.id }.ifEmpty { options }.randomOrNull(random)
}
