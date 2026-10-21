package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.model.Meal
import com.gabrieltagama.menuplanner.core.domain.model.ShoppingItem
import com.gabrieltagama.menuplanner.core.domain.model.ShoppingList
import com.gabrieltagama.menuplanner.core.domain.query.ShoppingListQueries
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

/**
 * Observes the shopping list of a month or of a date range (both ends included, it may span several
 * months) from the starters, mains and single dishes planned for lunch and the dishes planned for
 * dinner, limited to the requested meals (both by default), and emits it again whenever the plan or
 * a planned dish changes. It reads only ingredient lines (ShoppingListQueries), never whole dishes,
 * and a dish counts once per meal it is planned in. The lines become a ShoppingList, which adds up
 * and names each product.
 */
class ObserveShoppingListUseCase @Inject constructor(private val queries: ShoppingListQueries) {

    operator fun invoke(month: YearMonth, meals: Set<Meal> = Meal.entries.toSet()): Flow<List<ShoppingItem>> =
        invoke(month.atDay(1)..month.atEndOfMonth(), meals)

    operator fun invoke(dates: ClosedRange<LocalDate>, meals: Set<Meal> = Meal.entries.toSet()): Flow<List<ShoppingItem>> {
        val lineFlows = Meal.entries.filter { it in meals }.map { queries.observeIngredientLines(dates, it) }
        if (lineFlows.isEmpty()) return flowOf(emptyList())
        return combine(lineFlows) { lines -> ShoppingList.of(lines.toList().flatten()).items }
    }
}
